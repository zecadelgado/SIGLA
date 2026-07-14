-- Fase 6 — seguranca: RLS com negacao por padrao, privilegios minimos e
-- auditoria de disparo do faturamento.
--
-- Matriz de acesso (docs/fase6-matriz-acessos.md):
--  * postgres (desktop Java + Flyway): superusuario/BYPASSRLS — inalterado.
--  * service_role (Edge Function/jobs): mantem acesso de tabela concedido
--    pelo Supabase (BYPASSRLS) + EXECUTE nas RPCs de job.
--  * anon / authenticated: NENHUM acesso direto a tabelas ou RPCs.
--  * Perfis funcionais (ADMIN/OPERADOR/FINANCEIRO/TECNICO) sao papeis da
--    aplicacao validados dentro das RPCs (usuarios.tipo/ativo).

-- 1) RLS habilitado em TODAS as tabelas do schema public (idempotente).
--    Reproduz por migration a postura ja aplicada manualmente em producao
--    (V9 ficou comentada) e cobre as tabelas novas de V19+.
-- flyway_schema_history fica fora do loop: o proprio Flyway segura o lock da
-- tabela de historico durante a migracao (auto-bloqueio). O RLS dela e
-- aplicado fora de banda (producao ja tem; homologacao recebe via console).
DO $$
DECLARE r record;
BEGIN
  FOR r IN (SELECT tablename FROM pg_tables
             WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history') LOOP
    EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', r.tablename);
  END LOOP;
END $$;

-- 2) Negacao por padrao para as chaves publicas: nenhum privilegio direto de
--    tabela/sequencia para anon e authenticated.
DO $$
BEGIN
  IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'anon') THEN
    REVOKE ALL ON ALL TABLES IN SCHEMA public FROM anon;
    REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM anon;
    REVOKE ALL ON ALL FUNCTIONS IN SCHEMA public FROM anon;
  END IF;
  IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'authenticated') THEN
    REVOKE ALL ON ALL TABLES IN SCHEMA public FROM authenticated;
    REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM authenticated;
    REVOKE ALL ON ALL FUNCTIONS IN SCHEMA public FROM authenticated;
  END IF;
END $$;

-- Funcoes: remove o EXECUTE implicito de PUBLIC (novas RPCs ficam fechadas
-- por padrao; concessoes explicitas logo abaixo).
DO $$
DECLARE r record;
BEGIN
  FOR r IN (SELECT p.oid::regprocedure AS assinatura
              FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
             WHERE n.nspname = 'public' AND p.prokind = 'f') LOOP
    EXECUTE format('REVOKE ALL ON FUNCTION %s FROM PUBLIC', r.assinatura);
  END LOOP;
END $$;

-- 3) search_path fixo em todas as funcoes proprias do schema public
--    (advisor function_search_path_mutable; inclui as SECURITY DEFINER).
DO $$
DECLARE r record;
BEGIN
  FOR r IN (SELECT p.oid::regprocedure AS assinatura
              FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
             WHERE n.nspname = 'public' AND p.prokind = 'f'
               AND p.prolang IN (SELECT oid FROM pg_language WHERE lanname IN ('plpgsql', 'sql'))) LOOP
    EXECUTE format('ALTER FUNCTION %s SET search_path = public, pg_temp', r.assinatura);
  END LOOP;
END $$;

-- 4) Autorizacao administrativa nao confia em UUID enviado por cliente
--    autenticado: a RPC de desvinculacao (SECURITY DEFINER) deixa de ser
--    executavel por authenticated; somente o caminho tecnico permanece.
REVOKE ALL ON FUNCTION desvincular_os_contratual_v1(uuid, text, uuid) FROM PUBLIC;
DO $$
BEGIN
  IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'anon') THEN
    REVOKE ALL ON FUNCTION desvincular_os_contratual_v1(uuid, text, uuid) FROM anon;
  END IF;
  IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'authenticated') THEN
    REVOKE ALL ON FUNCTION desvincular_os_contratual_v1(uuid, text, uuid) FROM authenticated;
  END IF;
  IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'service_role') THEN
    GRANT EXECUTE ON FUNCTION desvincular_os_contratual_v1(uuid, text, uuid) TO service_role;
  END IF;
END $$;

-- 5) Auditoria de disparo do faturamento: ator e origem do disparo passam a
--    ser registrados. A assinatura antiga (2 argumentos) continua valida via
--    parametros com default — o desktop nao muda.
ALTER TABLE sigla_faturamento_execucoes
  ADD COLUMN origem text NOT NULL DEFAULT 'DESKTOP',
  ADD COLUMN ator uuid;

DROP FUNCTION rpc_faturar_mensalidades_v1(date, varchar);

CREATE FUNCTION rpc_faturar_mensalidades_v1(
  p_data_referencia date,
  p_chave_execucao varchar,
  p_origem text DEFAULT 'DESKTOP',
  p_ator uuid DEFAULT NULL
) RETURNS SETOF faturamento_mensalidade_resultado_v1
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = public, pg_temp
AS $$
DECLARE
  v_competencia date;
  v_criadas integer := 0;
  v_existentes integer := 0;
  r record;
  v_lancamento uuid;
  v_vencimento date;
  v_resultado faturamento_mensalidade_resultado_v1;
BEGIN
  IF p_data_referencia IS NULL THEN
    RAISE EXCEPTION 'data_referencia obrigatoria' USING ERRCODE = '23514';
  END IF;
  IF p_chave_execucao IS NULL OR btrim(p_chave_execucao) = '' THEN
    RAISE EXCEPTION 'chave_execucao obrigatoria' USING ERRCODE = '23514';
  END IF;
  IF upper(COALESCE(p_origem, '')) NOT IN ('DESKTOP', 'EDGE_FUNCTION', 'PG_CRON', 'MANUAL') THEN
    RAISE EXCEPTION 'origem do disparo invalida: %', p_origem USING ERRCODE = '23514';
  END IF;
  IF NOT COALESCE((SELECT habilitada FROM sigla_feature_flags
                   WHERE chave = 'faturamento_rpc_v1'), false) THEN
    RAISE EXCEPTION 'faturamento por RPC desativado pela feature flag faturamento_rpc_v1'
      USING ERRCODE = '23514';
  END IF;

  v_competencia := date_trunc('month', p_data_referencia)::date;

  FOR r IN (
    SELECT c.id AS contrato_id, c.cliente_id, c.descricao AS contrato_descricao,
           v.id AS vigencia_id, v.valor_mensal, v.data_inicio
      FROM contratos c
      JOIN contrato_vigencias v ON v.contrato_id = c.id
     WHERE upper(COALESCE(c.status, '')) IN ('ACTIVE', 'ATIVO')
       AND p_data_referencia >= v.data_inicio
       AND (v.data_fim IS NULL OR p_data_referencia <= v.data_fim)
       AND COALESCE(v.valor_mensal, 0) > 0
     ORDER BY c.id
  ) LOOP
    IF v_competencia < date_trunc('month', r.data_inicio)::date THEN
      CONTINUE;
    END IF;
    v_vencimento := v_competencia
      + (LEAST(extract(day FROM r.data_inicio)::int,
               extract(day FROM (v_competencia + interval '1 month - 1 day'))::int) - 1);

    BEGIN
      INSERT INTO financeiro_lancamentos (tipo, descricao, cliente_id, contrato_id,
        contrato_vigencia_id, origem_tipo, origem_id, competencia, valor_total,
        data_emissao, data_vencimento, status, observacoes, chave_idempotencia)
      VALUES ('ENTRY',
        'Mensalidade contrato'
          || CASE WHEN COALESCE(btrim(r.contrato_descricao), '') = '' THEN ''
                  ELSE ' ' || btrim(r.contrato_descricao) END
          || ' - ' || to_char(v_competencia, 'MM/YYYY'),
        r.cliente_id, r.contrato_id, r.vigencia_id, 'CONTRATO', r.contrato_id,
        v_competencia, r.valor_mensal, LEAST(v_vencimento, (CURRENT_TIMESTAMP AT TIME ZONE 'America/Sao_Paulo')::date),
        v_vencimento, 'PENDING',
        '[CONTRATO ' || r.contrato_id || ' COMPETENCIA ' || to_char(v_competencia, 'YYYY-MM') || ']',
        'MENSALIDADE:' || r.contrato_id || ':' || to_char(v_competencia, 'YYYY-MM'))
      ON CONFLICT (chave_idempotencia) WHERE chave_idempotencia IS NOT NULL DO NOTHING
      RETURNING id INTO v_lancamento;
    EXCEPTION WHEN unique_violation THEN
      v_lancamento := NULL;
    END;

    v_resultado.contrato_id := r.contrato_id;
    v_resultado.contrato_vigencia_id := r.vigencia_id;
    v_resultado.competencia := v_competencia;
    IF v_lancamento IS NULL THEN
      SELECT id INTO v_lancamento FROM financeiro_lancamentos
       WHERE chave_idempotencia = 'MENSALIDADE:' || r.contrato_id || ':' || to_char(v_competencia, 'YYYY-MM');
      v_existentes := v_existentes + 1;
      v_resultado.resultado := 'JA_EXISTENTE';
    ELSE
      v_criadas := v_criadas + 1;
      v_resultado.resultado := 'CRIADA';
    END IF;
    v_resultado.lancamento_id := v_lancamento;
    v_lancamento := NULL;
    RETURN NEXT v_resultado;
  END LOOP;

  INSERT INTO sigla_faturamento_execucoes (chave_execucao, data_referencia, criadas, existentes, origem, ator)
  VALUES (p_chave_execucao, p_data_referencia, v_criadas, v_existentes, upper(p_origem), p_ator)
  ON CONFLICT (chave_execucao) DO UPDATE SET executado_em = now(),
    execucoes = sigla_faturamento_execucoes.execucoes + 1,
    criadas = EXCLUDED.criadas, existentes = EXCLUDED.existentes,
    origem = EXCLUDED.origem, ator = EXCLUDED.ator;

  INSERT INTO auditoria_eventos (entidade_tipo, entidade_id, acao, detalhe, usuario_id)
  VALUES ('sigla_faturamento_execucoes', p_chave_execucao, 'FATURAMENTO_EXECUTADO',
    'origem=' || upper(p_origem) || '; data_referencia=' || p_data_referencia
      || '; criadas=' || v_criadas || '; existentes=' || v_existentes, p_ator);

  RETURN;
END;
$$;

REVOKE ALL ON FUNCTION rpc_faturar_mensalidades_v1(date, varchar, text, uuid) FROM PUBLIC;
DO $$
BEGIN
  IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'anon') THEN
    REVOKE ALL ON FUNCTION rpc_faturar_mensalidades_v1(date, varchar, text, uuid) FROM anon;
  END IF;
  IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'authenticated') THEN
    REVOKE ALL ON FUNCTION rpc_faturar_mensalidades_v1(date, varchar, text, uuid) FROM authenticated;
  END IF;
  IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'service_role') THEN
    GRANT EXECUTE ON FUNCTION rpc_faturar_mensalidades_v1(date, varchar, text, uuid) TO service_role;
  END IF;
END $$;
