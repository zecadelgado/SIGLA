-- Fase 5 — financeiro e contratos: vigencias como fonte de verdade, origem
-- formal dos lancamentos, cobranca explicita de OS contratual, estados
-- financeiros factuais, compensacoes (estorno/credito/reembolso) e autoridade
-- unica de mensalidade (rpc_faturar_mensalidades_v1).

-- =========================================================================
-- 1) Vigencias versionadas como fonte de verdade
-- =========================================================================
-- Backfill estrutural: contratos sem vigencia ganham a vigencia inicial
-- espelhando os proprios campos do contrato (nenhuma inferencia textual).
INSERT INTO contrato_vigencias (contrato_id, versao, data_inicio, data_fim,
  valor_mensal, tipo_contrato, renovacao_tipo, chave_idempotencia)
SELECT c.id, 1, c.data_inicio, c.data_fim, COALESCE(c.valor_mensal, 0),
       COALESCE(NULLIF(btrim(c.tipo_contrato), ''), 'MENSAL'), 'INICIAL',
       'VIG:INICIAL:' || c.id
  FROM contratos c
 WHERE c.data_inicio IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM contrato_vigencias v WHERE v.contrato_id = c.id)
ON CONFLICT (chave_idempotencia) DO NOTHING;

-- Renovacao manual/automatica idempotente: mesma identidade de contrato, nova
-- vigencia versionada; a exclusion constraint da V19 impede sobreposicao.
CREATE OR REPLACE FUNCTION renovar_contrato_v1(
  p_contrato_id uuid,
  p_data_inicio date,
  p_data_fim date,
  p_valor_mensal numeric,
  p_tipo_contrato text,
  p_renovacao_tipo text,
  p_usuario_id uuid,
  p_chave varchar
) RETURNS uuid LANGUAGE plpgsql AS $$
DECLARE
  v_contrato contratos%ROWTYPE;
  v_anterior contrato_vigencias%ROWTYPE;
  v_id uuid;
BEGIN
  IF NULLIF(btrim(COALESCE(p_chave, '')), '') IS NULL THEN
    RAISE EXCEPTION 'chave de idempotencia da renovacao e obrigatoria' USING ERRCODE = '23514';
  END IF;
  SELECT id INTO v_id FROM contrato_vigencias WHERE chave_idempotencia = p_chave;
  IF FOUND THEN
    RETURN v_id;
  END IF;
  IF p_data_inicio IS NULL THEN
    RAISE EXCEPTION 'data de inicio da nova vigencia e obrigatoria' USING ERRCODE = '23514';
  END IF;
  IF COALESCE(p_valor_mensal, 0) < 0 THEN
    RAISE EXCEPTION 'valor mensal da vigencia nao pode ser negativo' USING ERRCODE = '23514';
  END IF;
  IF upper(COALESCE(p_renovacao_tipo, '')) NOT IN ('MANUAL', 'AUTOMATICA') THEN
    RAISE EXCEPTION 'tipo de renovacao deve ser MANUAL ou AUTOMATICA' USING ERRCODE = '23514';
  END IF;

  SELECT * INTO STRICT v_contrato FROM contratos WHERE id = p_contrato_id FOR UPDATE;
  IF upper(COALESCE(v_contrato.status, '')) IN ('CANCELLED', 'CANCELADO') THEN
    RAISE EXCEPTION 'contrato cancelado nao pode ser renovado' USING ERRCODE = '23514';
  END IF;

  SELECT * INTO v_anterior FROM contrato_vigencias
   WHERE contrato_id = p_contrato_id ORDER BY versao DESC LIMIT 1;

  INSERT INTO contrato_vigencias (contrato_id, versao, data_inicio, data_fim,
    valor_mensal, tipo_contrato, renovacao_tipo, renovacao_autorizada,
    vigencia_anterior_id, chave_idempotencia, criado_por)
  VALUES (p_contrato_id, COALESCE(v_anterior.versao, 0) + 1, p_data_inicio, p_data_fim,
    COALESCE(p_valor_mensal, v_anterior.valor_mensal, v_contrato.valor_mensal, 0),
    COALESCE(NULLIF(btrim(p_tipo_contrato), ''), v_anterior.tipo_contrato, v_contrato.tipo_contrato, 'MENSAL'),
    upper(p_renovacao_tipo), true, v_anterior.id, p_chave, p_usuario_id)
  RETURNING id INTO v_id;

  -- contratos.data_fim/valor_mensal viram espelho de leitura do legado.
  UPDATE contratos SET data_fim = p_data_fim,
         valor_mensal = COALESCE(p_valor_mensal, valor_mensal),
         updated_at = now()
   WHERE id = p_contrato_id;

  INSERT INTO auditoria_eventos (entidade_tipo, entidade_id, acao, detalhe, usuario_id)
  VALUES ('contratos', p_contrato_id::text, 'CONTRATO_RENOVADO',
    'vigencia=' || v_id::text || '; inicio=' || p_data_inicio || '; chave=' || p_chave, p_usuario_id);

  RETURN v_id;
END;
$$;

-- OS contratual sempre referencia a vigencia que cobre a data agendada.
CREATE OR REPLACE FUNCTION trg_vincular_vigencia_os() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
  IF NEW.contrato_id IS NOT NULL AND NEW.data_agendada IS NOT NULL THEN
    SELECT v.id INTO NEW.contrato_vigencia_id
      FROM contrato_vigencias v
     WHERE v.contrato_id = NEW.contrato_id
       AND (NEW.data_agendada AT TIME ZONE 'America/Sao_Paulo')::date >= v.data_inicio
       AND (v.data_fim IS NULL OR (NEW.data_agendada AT TIME ZONE 'America/Sao_Paulo')::date <= v.data_fim)
     ORDER BY v.versao DESC LIMIT 1;
  END IF;
  RETURN NEW;
END;
$$;
CREATE TRIGGER vincular_vigencia_os
BEFORE INSERT OR UPDATE OF contrato_id, data_agendada ON ordens_servico
FOR EACH ROW WHEN (NEW.contrato_id IS NOT NULL)
EXECUTE FUNCTION trg_vincular_vigencia_os();

-- =========================================================================
-- 2) Regra explicita de cobranca da OS contratual
-- =========================================================================
ALTER TABLE ordens_servico ADD COLUMN regra_cobranca text;
ALTER TABLE ordens_servico ADD CONSTRAINT chk_os_regra_cobranca
  CHECK (regra_cobranca IS NULL OR regra_cobranca IN ('COBERTA_PELO_CONTRATO', 'COBRAR_EXTRA'));

-- Backfill somente com evidencia estrutural:
--  - OS contratual com conta a receber propria nao cancelada => COBRAR_EXTRA;
--  - OS contratual concluida sem conta a receber => COBERTA_PELO_CONTRATO;
--  - demais OS contratuais legadas ficam NULL e entram na conciliacao.
UPDATE ordens_servico os SET regra_cobranca = 'COBRAR_EXTRA'
 WHERE os.contrato_id IS NOT NULL AND os.regra_cobranca IS NULL
   AND EXISTS (SELECT 1 FROM financeiro_lancamentos l
                WHERE l.ordem_servico_id = os.id AND l.tipo = 'ENTRY'
                  AND upper(COALESCE(l.status, '')) <> 'CANCELLED');
UPDATE ordens_servico os SET regra_cobranca = 'COBERTA_PELO_CONTRATO'
 WHERE os.contrato_id IS NOT NULL AND os.regra_cobranca IS NULL
   AND (upper(COALESCE(os.status, '')) IN ('CONCLUIDA', 'COMPLETED') OR os.foi_feito);

CREATE OR REPLACE FUNCTION trg_exigir_regra_cobranca_os() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
  IF TG_OP = 'INSERT' AND NEW.contrato_id IS NOT NULL AND NEW.regra_cobranca IS NULL THEN
    RAISE EXCEPTION 'OS contratual exige regra de cobranca explicita (COBERTA_PELO_CONTRATO ou COBRAR_EXTRA)'
      USING ERRCODE = '23514';
  END IF;
  IF TG_OP = 'UPDATE' AND OLD.contrato_id IS NULL AND NEW.contrato_id IS NOT NULL
     AND NEW.regra_cobranca IS NULL THEN
    RAISE EXCEPTION 'vincular contrato exige regra de cobranca explicita (COBERTA_PELO_CONTRATO ou COBRAR_EXTRA)'
      USING ERRCODE = '23514';
  END IF;
  RETURN NEW;
END;
$$;
CREATE TRIGGER exigir_regra_cobranca_os
BEFORE INSERT OR UPDATE OF contrato_id, regra_cobranca ON ordens_servico
FOR EACH ROW EXECUTE FUNCTION trg_exigir_regra_cobranca_os();

-- Conciliacao: legado contratual ainda sem classificacao explicita.
CREATE OR REPLACE VIEW vw_os_contratuais_sem_regra_v1 AS
SELECT os.id, os.numero_os, os.cliente_id, os.contrato_id, os.contrato_vigencia_id,
       os.status, os.data_agendada, os.foi_feito
  FROM ordens_servico os
 WHERE os.contrato_id IS NOT NULL AND os.regra_cobranca IS NULL;

-- =========================================================================
-- 3) Origem formal, competencia e vinculos dos lancamentos
-- =========================================================================
-- Deriva a origem de forma estrutural quando o gravador nao informar:
--  ORDEM_SERVICO quando ha OS; CONTRATO quando ha contrato+competencia;
--  MANUAL nos demais. Tambem herda contrato/vigencia da OS vinculada.
CREATE OR REPLACE FUNCTION trg_definir_origem_lancamento() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE v_os ordens_servico%ROWTYPE;
BEGIN
  IF NEW.ordem_servico_id IS NOT NULL THEN
    SELECT * INTO v_os FROM ordens_servico WHERE id = NEW.ordem_servico_id;
    IF FOUND THEN
      NEW.contrato_id := COALESCE(NEW.contrato_id, v_os.contrato_id);
      NEW.contrato_vigencia_id := COALESCE(NEW.contrato_vigencia_id, v_os.contrato_vigencia_id);
    END IF;
  END IF;
  IF NEW.origem_tipo IS NULL THEN
    IF NEW.ordem_servico_id IS NOT NULL THEN
      NEW.origem_tipo := 'ORDEM_SERVICO';
      NEW.origem_id := NEW.ordem_servico_id;
    ELSIF NEW.contrato_id IS NOT NULL AND NEW.competencia IS NOT NULL THEN
      NEW.origem_tipo := 'CONTRATO';
      NEW.origem_id := NEW.contrato_id;
    ELSE
      NEW.origem_tipo := 'MANUAL';
      NEW.origem_id := COALESCE(NEW.origem_id, NEW.id);
    END IF;
  END IF;
  IF NEW.origem_id IS NULL THEN
    NEW.origem_id := CASE NEW.origem_tipo
      WHEN 'ORDEM_SERVICO' THEN NEW.ordem_servico_id
      WHEN 'CONTRATO' THEN NEW.contrato_id
      ELSE NEW.id END;
  END IF;
  RETURN NEW;
END;
$$;
CREATE TRIGGER definir_origem_lancamento
BEFORE INSERT ON financeiro_lancamentos
FOR EACH ROW EXECUTE FUNCTION trg_definir_origem_lancamento();

-- Unicidade de cobranca automatica por OS (conta a receber): escritas novas
-- nao podem duplicar uma cobranca nao cancelada da mesma OS.
CREATE OR REPLACE FUNCTION trg_bloquear_cobranca_duplicada_os() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
  IF NEW.tipo = 'ENTRY' AND NEW.ordem_servico_id IS NOT NULL
     AND EXISTS (SELECT 1 FROM financeiro_lancamentos l
                  WHERE l.ordem_servico_id = NEW.ordem_servico_id
                    AND l.tipo = 'ENTRY'
                    AND upper(COALESCE(l.status, '')) <> 'CANCELLED') THEN
    RAISE EXCEPTION 'OS ja possui cobranca ativa; cancele ou compense antes de gerar outra'
      USING ERRCODE = '23505';
  END IF;
  RETURN NEW;
END;
$$;
CREATE TRIGGER bloquear_cobranca_duplicada_os
BEFORE INSERT ON financeiro_lancamentos
FOR EACH ROW EXECUTE FUNCTION trg_bloquear_cobranca_duplicada_os();

-- Unicidade de mensalidade por contrato/competencia (linhas com origem formal;
-- legado sem origem nao entra e permanece na conciliacao).
CREATE UNIQUE INDEX ux_financeiro_mensalidade_contrato_competencia
  ON financeiro_lancamentos (contrato_id, competencia)
 WHERE origem_tipo = 'CONTRATO' AND status <> 'CANCELLED';

-- Consultas por contrato, vigencia, OS e origem.
CREATE INDEX IF NOT EXISTS idx_financeiro_lancamentos_vigencia
  ON financeiro_lancamentos (contrato_vigencia_id);
CREATE INDEX IF NOT EXISTS idx_financeiro_lancamentos_origem
  ON financeiro_lancamentos (origem_tipo, origem_id);
CREATE INDEX IF NOT EXISTS idx_financeiro_lancamentos_os
  ON financeiro_lancamentos (ordem_servico_id);

-- =========================================================================
-- 4) Estados financeiros: fatos x projecoes
-- =========================================================================
-- OVERDUE persistido e legado: vira PENDING (a projecao por relogio ja existe
-- em vw_financeiro_lancamentos_v1, timezone America/Sao_Paulo).
UPDATE financeiro_lancamentos SET status = 'PENDING' WHERE upper(status) = 'OVERDUE';
UPDATE financeiro_parcelas SET status = 'PENDING' WHERE upper(status) = 'OVERDUE';

CREATE OR REPLACE FUNCTION trg_bloquear_status_por_relogio() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
  IF upper(COALESCE(NEW.status, '')) = 'OVERDUE'
     AND (TG_OP = 'INSERT' OR OLD.status IS DISTINCT FROM NEW.status) THEN
    RAISE EXCEPTION 'OVERDUE e projecao por relogio; persista PENDING/PARTIAL/PAID/CANCELLED'
      USING ERRCODE = '23514';
  END IF;
  RETURN NEW;
END;
$$;
CREATE TRIGGER bloquear_status_por_relogio
BEFORE INSERT OR UPDATE OF status ON financeiro_lancamentos
FOR EACH ROW EXECUTE FUNCTION trg_bloquear_status_por_relogio();
CREATE TRIGGER bloquear_status_por_relogio_parcela
BEFORE INSERT OR UPDATE OF status ON financeiro_parcelas
FOR EACH ROW EXECUTE FUNCTION trg_bloquear_status_por_relogio();

-- Pagamento realizado nunca transita direto para CANCELLED.
CREATE OR REPLACE FUNCTION trg_bloquear_cancelamento_de_pago() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
  IF upper(COALESCE(OLD.status, '')) IN ('PAID', 'PARTIAL')
     AND upper(COALESCE(NEW.status, '')) = 'CANCELLED' THEN
    RAISE EXCEPTION 'lancamento pago ou parcial exige estorno/credito/reembolso; nao pode ser cancelado'
      USING ERRCODE = '23514';
  END IF;
  RETURN NEW;
END;
$$;
CREATE TRIGGER bloquear_cancelamento_de_pago
BEFORE UPDATE OF status ON financeiro_lancamentos
FOR EACH ROW EXECUTE FUNCTION trg_bloquear_cancelamento_de_pago();

-- =========================================================================
-- 5) Compensacoes: estorno, credito e reembolso
-- =========================================================================
CREATE OR REPLACE FUNCTION compensar_lancamento_v1(
  p_lancamento_id uuid,
  p_tipo text,
  p_valor numeric,
  p_motivo text,
  p_meio text,
  p_usuario_id uuid,
  p_chave varchar,
  p_autorizar_divisao boolean DEFAULT false
) RETURNS uuid LANGUAGE plpgsql AS $$
DECLARE
  v_lancamento financeiro_lancamentos%ROWTYPE;
  v_usuario usuarios%ROWTYPE;
  v_liquidado numeric;
  v_compensado numeric;
  v_compensacao uuid;
  v_compensatorio uuid;
BEGIN
  IF upper(COALESCE(p_tipo, '')) NOT IN ('ESTORNO', 'CREDITO', 'REEMBOLSO') THEN
    RAISE EXCEPTION 'tipo de compensacao deve ser ESTORNO, CREDITO ou REEMBOLSO' USING ERRCODE = '23514';
  END IF;
  IF p_valor IS NULL OR p_valor <= 0 THEN
    RAISE EXCEPTION 'valor da compensacao deve ser maior que zero' USING ERRCODE = '23514';
  END IF;
  IF NULLIF(btrim(COALESCE(p_motivo, '')), '') IS NULL THEN
    RAISE EXCEPTION 'motivo da compensacao e obrigatorio' USING ERRCODE = '23514';
  END IF;
  IF NULLIF(btrim(COALESCE(p_chave, '')), '') IS NULL THEN
    RAISE EXCEPTION 'chave de idempotencia da compensacao e obrigatoria' USING ERRCODE = '23514';
  END IF;
  SELECT * INTO v_usuario FROM usuarios WHERE id = p_usuario_id;
  IF NOT FOUND OR NOT COALESCE(v_usuario.ativo, false)
     OR upper(COALESCE(v_usuario.tipo, '')) NOT IN ('ADMIN', 'FINANCEIRO') THEN
    RAISE EXCEPTION 'somente ADMIN ou FINANCEIRO ativo pode compensar pagamento' USING ERRCODE = '23514';
  END IF;

  SELECT id INTO v_compensacao FROM financeiro_compensacoes WHERE chave_idempotencia = p_chave;
  IF FOUND THEN
    RETURN v_compensacao;
  END IF;

  SELECT * INTO STRICT v_lancamento FROM financeiro_lancamentos
   WHERE id = p_lancamento_id FOR UPDATE;
  IF upper(COALESCE(v_lancamento.status, '')) NOT IN ('PAID', 'PARTIAL') THEN
    RAISE EXCEPTION 'compensacao exige lancamento pago ou parcialmente pago' USING ERRCODE = '23514';
  END IF;

  -- Valor liquidado factual: parcelas pagas quando existem, senao o total pago.
  SELECT COALESCE(SUM(p.valor_parcela), 0) INTO v_liquidado
    FROM financeiro_parcelas p
   WHERE p.lancamento_id = p_lancamento_id AND upper(COALESCE(p.status, '')) = 'PAID';
  IF v_liquidado = 0 AND upper(v_lancamento.status) = 'PAID' THEN
    v_liquidado := COALESCE(v_lancamento.valor_total, 0);
  END IF;

  SELECT COALESCE(SUM(c.valor), 0) INTO v_compensado
    FROM financeiro_compensacoes c
   WHERE c.lancamento_original_id = p_lancamento_id
     AND upper(c.status) NOT IN ('FALHOU', 'CANCELADA');

  IF p_valor > v_liquidado - v_compensado THEN
    RAISE EXCEPTION 'compensacao de % excede o valor liquidado disponivel de %',
      p_valor, v_liquidado - v_compensado USING ERRCODE = '23514';
  END IF;

  -- Credito e reembolso sao mutuamente exclusivos para a mesma parcela
  -- compensada, salvo divisao explicita e auditavel.
  IF NOT p_autorizar_divisao THEN
    IF upper(p_tipo) = 'CREDITO' AND EXISTS (
        SELECT 1 FROM financeiro_compensacoes c
         WHERE c.lancamento_original_id = p_lancamento_id AND c.tipo = 'REEMBOLSO'
           AND upper(c.status) NOT IN ('FALHOU', 'CANCELADA')) THEN
      RAISE EXCEPTION 'lancamento ja possui reembolso; divisao credito/reembolso exige autorizacao explicita'
        USING ERRCODE = '23514';
    END IF;
    IF upper(p_tipo) = 'REEMBOLSO' AND EXISTS (
        SELECT 1 FROM financeiro_compensacoes c
         WHERE c.lancamento_original_id = p_lancamento_id AND c.tipo = 'CREDITO'
           AND upper(c.status) NOT IN ('FALHOU', 'CANCELADA')) THEN
      RAISE EXCEPTION 'lancamento ja possui credito; divisao credito/reembolso exige autorizacao explicita'
        USING ERRCODE = '23514';
    END IF;
  END IF;

  IF upper(p_tipo) = 'ESTORNO' THEN
    -- Lancamento compensatorio espelho, vinculado ao original.
    INSERT INTO financeiro_lancamentos (tipo, descricao, cliente_id, ordem_servico_id,
      contrato_id, contrato_vigencia_id, origem_tipo, origem_id, valor_total,
      data_emissao, data_vencimento, status, observacoes, chave_idempotencia,
      lancamento_original_id)
    VALUES (CASE WHEN v_lancamento.tipo = 'ENTRY' THEN 'EXPENSE' ELSE 'ENTRY' END,
      'Estorno: ' || COALESCE(v_lancamento.descricao, v_lancamento.id::text),
      v_lancamento.cliente_id, NULL, v_lancamento.contrato_id,
      v_lancamento.contrato_vigencia_id, 'COMPENSACAO', v_lancamento.id, p_valor,
      (CURRENT_TIMESTAMP AT TIME ZONE 'America/Sao_Paulo')::date,
      (CURRENT_TIMESTAMP AT TIME ZONE 'America/Sao_Paulo')::date,
      'PENDING', 'Compensacao ESTORNO do lancamento ' || v_lancamento.id || '; motivo: ' || btrim(p_motivo),
      'COMP:' || p_chave, v_lancamento.id)
    RETURNING id INTO v_compensatorio;
  END IF;

  INSERT INTO financeiro_compensacoes (lancamento_original_id, lancamento_compensatorio_id,
    tipo, valor, motivo, status, meio, chave_idempotencia, criado_por, processed_at)
  VALUES (p_lancamento_id, v_compensatorio, upper(p_tipo), p_valor, btrim(p_motivo),
    CASE WHEN upper(p_tipo) = 'REEMBOLSO' THEN 'SOLICITADA' ELSE 'CONFIRMADA' END,
    NULLIF(btrim(COALESCE(p_meio, '')), ''), p_chave, p_usuario_id,
    CASE WHEN upper(p_tipo) = 'REEMBOLSO' THEN NULL ELSE now() END)
  RETURNING id INTO v_compensacao;

  IF upper(p_tipo) = 'CREDITO' THEN
    IF v_lancamento.cliente_id IS NULL THEN
      RAISE EXCEPTION 'credito exige lancamento com cliente' USING ERRCODE = '23514';
    END IF;
    -- Credito e razao de movimentos, nunca saldo editavel.
    INSERT INTO cliente_credito_movimentos (cliente_id, compensacao_id, tipo, valor, chave_idempotencia)
    VALUES (v_lancamento.cliente_id, v_compensacao, 'CREDITO', p_valor, 'CRED:' || p_chave);
  END IF;

  INSERT INTO auditoria_eventos (entidade_tipo, entidade_id, acao, detalhe, usuario_id)
  VALUES ('financeiro_lancamentos', p_lancamento_id::text,
    'COMPENSACAO_' || upper(p_tipo),
    'valor=' || p_valor || '; motivo=' || btrim(p_motivo) || '; chave=' || p_chave
      || CASE WHEN p_autorizar_divisao THEN '; divisao_autorizada=true' ELSE '' END,
    p_usuario_id);

  RETURN v_compensacao;
END;
$$;

-- Reembolso registra solicitacao e depois confirmacao ou falha.
CREATE OR REPLACE FUNCTION concluir_reembolso_v1(
  p_chave varchar,
  p_sucesso boolean,
  p_usuario_id uuid
) RETURNS uuid LANGUAGE plpgsql AS $$
DECLARE
  v_compensacao financeiro_compensacoes%ROWTYPE;
  v_novo_status text;
BEGIN
  SELECT * INTO STRICT v_compensacao FROM financeiro_compensacoes
   WHERE chave_idempotencia = p_chave FOR UPDATE;
  IF v_compensacao.tipo <> 'REEMBOLSO' THEN
    RAISE EXCEPTION 'somente compensacao de REEMBOLSO pode ser concluida' USING ERRCODE = '23514';
  END IF;
  v_novo_status := CASE WHEN p_sucesso THEN 'CONFIRMADA' ELSE 'FALHOU' END;
  IF v_compensacao.status = v_novo_status THEN
    RETURN v_compensacao.id;
  END IF;
  IF v_compensacao.status <> 'SOLICITADA' THEN
    RAISE EXCEPTION 'reembolso ja concluido com resultado %', v_compensacao.status USING ERRCODE = '23514';
  END IF;
  UPDATE financeiro_compensacoes SET status = v_novo_status, processed_at = now()
   WHERE id = v_compensacao.id;
  INSERT INTO auditoria_eventos (entidade_tipo, entidade_id, acao, detalhe, usuario_id)
  VALUES ('financeiro_compensacoes', v_compensacao.id::text,
    'REEMBOLSO_' || v_novo_status, 'chave=' || p_chave, p_usuario_id);
  RETURN v_compensacao.id;
END;
$$;

-- =========================================================================
-- 6) Autoridade unica de mensalidade
-- =========================================================================
CREATE TABLE sigla_faturamento_execucoes (
  chave_execucao varchar(180) PRIMARY KEY,
  data_referencia date NOT NULL,
  executado_em timestamptz NOT NULL DEFAULT now(),
  execucoes integer NOT NULL DEFAULT 1,
  criadas integer NOT NULL DEFAULT 0,
  existentes integer NOT NULL DEFAULT 0
);

INSERT INTO sigla_feature_flags (chave, habilitada, descricao) VALUES
  ('faturamento_rpc_v1', true,
   'rpc_faturar_mensalidades_v1 e a unica autoridade que escreve mensalidades.'),
  ('mensalidade_produtor_java', false,
   'Produtor Java paralelo de mensalidades desativado na Fase 5.')
ON CONFLICT (chave) DO UPDATE SET habilitada = EXCLUDED.habilitada,
  descricao = EXCLUDED.descricao, updated_at = now();

CREATE OR REPLACE FUNCTION rpc_faturar_mensalidades_v1(
  p_data_referencia date,
  p_chave_execucao varchar
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
    -- Competencia sempre dentro da vigencia que a originou.
    IF v_competencia < date_trunc('month', r.data_inicio)::date THEN
      CONTINUE;
    END IF;
    v_vencimento := v_competencia
      + (LEAST(extract(day FROM r.data_inicio)::int,
               extract(day FROM (v_competencia + interval '1 month - 1 day'))::int) - 1);

    -- Disparadores concorrentes podem colidir em qualquer indice unico
    -- (chave de idempotencia OU contrato/competencia): ambos os casos sao a
    -- mesma mensalidade ja existente, nunca uma duplicata.
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

  INSERT INTO sigla_faturamento_execucoes (chave_execucao, data_referencia, criadas, existentes)
  VALUES (p_chave_execucao, p_data_referencia, v_criadas, v_existentes)
  ON CONFLICT (chave_execucao) DO UPDATE SET executado_em = now(),
    execucoes = sigla_faturamento_execucoes.execucoes + 1,
    criadas = EXCLUDED.criadas, existentes = EXCLUDED.existentes;

  RETURN;
END;
$$;

REVOKE ALL ON FUNCTION rpc_faturar_mensalidades_v1(date, varchar) FROM PUBLIC;
