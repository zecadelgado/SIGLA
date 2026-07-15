-- Fase 6 (cutover) — convivencia com o produtor legado de mensalidades.
--
-- Antes da Fase 5, o desktop gravava mensalidades diretamente com id
-- DETERMINISTICO (UUIDv3 de 'contrato-mensalidade:<contrato>:<AAAA-MM>') e o
-- marcador '[CONTRATO <uuid> COMPETENCIA <AAAA-MM>]' nas observacoes — sem
-- origem/competencia/chave e, em bancos V17, sem sequer a coluna contrato_id.
--
-- A identificacao aqui NAO e inferencia textual solta: o contrato/competencia
-- extraidos do marcador so sao aceitos quando o UUID deterministico recomputado
-- bate exatamente com o id da linha (prova estrutural de que o produtor gravou
-- aquela linha com aqueles valores). Linhas nao verificaveis ficam intactas e
-- aparecem no diagnostico da Fase 5.

CREATE OR REPLACE FUNCTION fn_uuid_mensalidade_legada_v1(p_contrato uuid, p_competencia date)
RETURNS uuid LANGUAGE sql IMMUTABLE AS $$
  SELECT (
    substr(h, 1, 8) || '-' || substr(h, 9, 4) || '-3' || substr(h, 14, 3) || '-' ||
    lpad(to_hex((get_byte(decode(h, 'hex'), 8) & 63) | 128), 2, '0') || substr(h, 19, 14)
  )::uuid
  FROM md5('contrato-mensalidade:' || p_contrato || ':' || to_char(p_competencia, 'YYYY-MM')) AS h;
$$;

-- 1) Backfill: mensalidades legadas verificadas ganham identidade formal
--    (origem/competencia/chave/vigencia e, quando ausente, o proprio
--    contrato_id). Apenas uma por contrato/competencia; sem colisao de chave.
WITH marcadas AS (
  SELECT l.id,
         ((regexp_match(l.observacoes, '\[CONTRATO ([0-9a-fA-F-]{36}) COMPETENCIA ([0-9]{4}-[0-9]{2})'))[1])::uuid AS contrato_extraido,
         (((regexp_match(l.observacoes, '\[CONTRATO ([0-9a-fA-F-]{36}) COMPETENCIA ([0-9]{4}-[0-9]{2})'))[2]) || '-01')::date AS competencia
    FROM financeiro_lancamentos l
   WHERE l.tipo = 'ENTRY'
     AND l.ordem_servico_id IS NULL
     AND l.origem_tipo IS NULL
     AND l.chave_idempotencia IS NULL
     AND l.competencia IS NULL
     AND l.observacoes ~ '\[CONTRATO [0-9a-fA-F-]{36} COMPETENCIA [0-9]{4}-[0-9]{2}'
), verificadas AS (
  SELECT m.id, m.contrato_extraido, m.competencia,
         row_number() OVER (PARTITION BY m.contrato_extraido, m.competencia ORDER BY m.id) AS ordem
    FROM marcadas m
    JOIN contratos c ON c.id = m.contrato_extraido
   WHERE m.id = fn_uuid_mensalidade_legada_v1(m.contrato_extraido, m.competencia)
)
UPDATE financeiro_lancamentos l
   SET contrato_id = COALESCE(l.contrato_id, v.contrato_extraido),
       origem_tipo = 'CONTRATO',
       origem_id = v.contrato_extraido,
       competencia = v.competencia,
       chave_idempotencia = 'MENSALIDADE:' || v.contrato_extraido || ':' || to_char(v.competencia, 'YYYY-MM'),
       contrato_vigencia_id = COALESCE(l.contrato_vigencia_id, (
         SELECT vg.id FROM contrato_vigencias vg
          WHERE vg.contrato_id = v.contrato_extraido
            AND v.competencia >= date_trunc('month', vg.data_inicio)::date
            AND (vg.data_fim IS NULL OR v.competencia <= vg.data_fim)
          ORDER BY vg.versao DESC LIMIT 1))
  FROM verificadas v
 WHERE l.id = v.id
   AND v.ordem = 1
   AND NOT EXISTS (SELECT 1 FROM financeiro_lancamentos e
                    WHERE e.chave_idempotencia = 'MENSALIDADE:' || v.contrato_extraido || ':' || to_char(v.competencia, 'YYYY-MM'));

-- 2) Normalizacao em tempo de gravacao: enquanto houver desktop antigo rodando
--    o produtor Java, o insert legado (verificado pelo mesmo criterio) recebe a
--    identidade formal na hora. Se a RPC ja faturou a competencia, a chave
--    unica rejeita a duplicata; se o legado chegar primeiro, a RPC reconhece
--    JA_EXISTENTE. Nunca ha duas mensalidades ativas do mesmo contrato/mes.
CREATE OR REPLACE FUNCTION trg_normalizar_mensalidade_produtor_legado() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
  v_contrato uuid;
  v_competencia date;
  v_marcador text[];
BEGIN
  IF NEW.chave_idempotencia IS NOT NULL OR NEW.tipo <> 'ENTRY'
     OR NEW.ordem_servico_id IS NOT NULL OR NEW.competencia IS NOT NULL THEN
    RETURN NEW;
  END IF;

  IF NEW.contrato_id IS NOT NULL AND NEW.data_vencimento IS NOT NULL THEN
    -- Producao pos-V18: produtor legado ja gravava contrato_id.
    v_contrato := NEW.contrato_id;
    v_competencia := date_trunc('month', NEW.data_vencimento)::date;
  ELSE
    -- Producao V17: identidade so pelo marcador + id deterministico verificado.
    v_marcador := regexp_match(COALESCE(NEW.observacoes, ''),
      '\[CONTRATO ([0-9a-fA-F-]{36}) COMPETENCIA ([0-9]{4}-[0-9]{2})');
    IF v_marcador IS NULL THEN
      RETURN NEW;
    END IF;
    v_contrato := v_marcador[1]::uuid;
    v_competencia := (v_marcador[2] || '-01')::date;
    IF NEW.id IS NULL OR NEW.id <> fn_uuid_mensalidade_legada_v1(v_contrato, v_competencia)
       OR NOT EXISTS (SELECT 1 FROM contratos c WHERE c.id = v_contrato) THEN
      RETURN NEW;
    END IF;
  END IF;

  NEW.contrato_id := v_contrato;
  NEW.origem_tipo := 'CONTRATO';
  NEW.origem_id := v_contrato;
  NEW.competencia := v_competencia;
  NEW.chave_idempotencia := 'MENSALIDADE:' || v_contrato || ':' || to_char(v_competencia, 'YYYY-MM');
  NEW.contrato_vigencia_id := COALESCE(NEW.contrato_vigencia_id, (
    SELECT vg.id FROM contrato_vigencias vg
     WHERE vg.contrato_id = v_contrato
       AND v_competencia >= date_trunc('month', vg.data_inicio)::date
       AND (vg.data_fim IS NULL OR v_competencia <= vg.data_fim)
     ORDER BY vg.versao DESC LIMIT 1));

  INSERT INTO auditoria_eventos (entidade_tipo, entidade_id, acao, detalhe)
  VALUES ('financeiro_lancamentos', COALESCE(NEW.id::text, 'sem-id'),
          'MENSALIDADE_LEGADA_NORMALIZADA',
          'contrato=' || v_contrato || '; competencia=' || to_char(v_competencia, 'YYYY-MM')
            || '; produtor legado normalizado: autoridade e rpc_faturar_mensalidades_v1');
  RETURN NEW;
END;
$$;

-- Precisa rodar ANTES de definir_origem_lancamento (ordem alfabetica dos
-- triggers BEFORE), senao a origem derivada mascararia o fingerprint legado.
CREATE TRIGGER aa_normalizar_mensalidade_produtor_legado
BEFORE INSERT ON financeiro_lancamentos
FOR EACH ROW EXECUTE FUNCTION trg_normalizar_mensalidade_produtor_legado();
