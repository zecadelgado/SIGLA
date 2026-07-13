CREATE OR REPLACE VIEW vw_agenda_operacional_v1 AS
SELECT a.id, a.ordem_servico_id, a.ocorrencia_id, a.responsavel_id,
       a.data_inicio, a.data_fim, a.timezone, a.status AS status_factual,
       CASE
         WHEN a.status IN ('CANCELLED','COMPLETED') THEN a.status
         WHEN a.data_fim < now() THEN 'MISSED'
         ELSE a.status
       END AS status_efetivo
FROM agenda_eventos a
WHERE a.origem_tipo = 'OPERACIONAL';

CREATE OR REPLACE VIEW vw_financeiro_lancamentos_v1 AS
SELECT l.*,
       CASE
         WHEN l.status IN ('PAID','CANCELLED','PARTIAL') THEN l.status
         WHEN l.data_vencimento < (CURRENT_TIMESTAMP AT TIME ZONE 'America/Sao_Paulo')::date THEN 'OVERDUE'
         ELSE l.status
       END AS status_efetivo
FROM financeiro_lancamentos l;

CREATE OR REPLACE VIEW vw_cliente_credito_saldo_v1 AS
SELECT cliente_id,
       COALESCE(SUM(CASE WHEN tipo = 'CREDITO' THEN valor ELSE -valor END), 0)::numeric(19,2) AS saldo
FROM cliente_credito_movimentos
GROUP BY cliente_id;

CREATE TYPE faturamento_mensalidade_resultado_v1 AS (
  contrato_id uuid,
  contrato_vigencia_id uuid,
  competencia date,
  lancamento_id uuid,
  resultado text
);

-- Contrato inicial: valida entrada e torna retries observaveis. A geracao efetiva
-- sera ativada na Fase 5; nesta fase nenhum produtor legado e substituido.
CREATE OR REPLACE FUNCTION rpc_faturar_mensalidades_v1(
  p_data_referencia date,
  p_chave_execucao varchar
) RETURNS SETOF faturamento_mensalidade_resultado_v1
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = public, pg_temp
AS $$
BEGIN
  IF p_data_referencia IS NULL THEN
    RAISE EXCEPTION 'data_referencia obrigatoria';
  END IF;
  IF p_chave_execucao IS NULL OR btrim(p_chave_execucao) = '' THEN
    RAISE EXCEPTION 'chave_execucao obrigatoria';
  END IF;
  RETURN QUERY
  SELECT v.contrato_id, v.id, date_trunc('month', p_data_referencia)::date,
         NULL::uuid, 'CONTRATO_INICIAL_SEM_ESCRITA'::text
  FROM contrato_vigencias v
  WHERE p_data_referencia >= v.data_inicio
    AND (v.data_fim IS NULL OR p_data_referencia <= v.data_fim);
END;
$$;

REVOKE ALL ON FUNCTION rpc_faturar_mensalidades_v1(date, varchar) FROM PUBLIC;
