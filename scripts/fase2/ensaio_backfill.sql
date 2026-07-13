-- Ensaio reversivel: classifica somente casos inequivocos e sempre termina em ROLLBACK.
BEGIN;

UPDATE produtos SET
  quantidade_atual_decimal = quantidade_atual::numeric(19,4),
  quantidade_minima_decimal = quantidade_minima::numeric(19,4)
WHERE quantidade_atual = round(quantidade_atual, 4)
  AND quantidade_minima = round(quantidade_minima, 4);

UPDATE estoque_movimentacoes
SET quantidade_decimal = quantidade::numeric(19,4)
WHERE quantidade = round(quantidade, 4);

UPDATE ordem_servico_produtos
SET quantidade_solicitada = quantidade::numeric(19,4)
WHERE quantidade = round(quantidade, 4);

INSERT INTO contrato_vigencias(
  contrato_id, versao, data_inicio, data_fim, valor_mensal, tipo_contrato,
  renovacao_tipo, chave_idempotencia, created_at
)
SELECT c.id, 1, c.data_inicio, c.data_fim, c.valor_mensal::numeric(19,2),
       c.tipo_contrato, 'INICIAL', 'LEGADO:CONTRATO:' || c.id || ':V1', COALESCE(c.created_at, now())
FROM contratos c
WHERE c.data_inicio IS NOT NULL
ON CONFLICT DO NOTHING;

UPDATE agenda_eventos a
SET origem_tipo = CASE
  WHEN a.ordem_servico_id IS NOT NULL THEN 'OPERACIONAL'
  WHEN a.certificado_id IS NOT NULL THEN 'CERTIFICADO'
  WHEN a.contrato_id IS NOT NULL THEN 'CONTRATO'
  ELSE 'MANUAL'
END,
timezone = 'America/Sao_Paulo'
WHERE origem_tipo IS NULL;

-- Evidencias do que seria alterado; nenhuma escrita e confirmada.
SELECT 'produtos_decimal' item, count(*) FROM produtos WHERE quantidade_atual_decimal IS NOT NULL
UNION ALL SELECT 'movimentos_decimal', count(*) FROM estoque_movimentacoes WHERE quantidade_decimal IS NOT NULL
UNION ALL SELECT 'alocacoes_decimal', count(*) FROM ordem_servico_produtos WHERE quantidade_solicitada IS NOT NULL
UNION ALL SELECT 'vigencias_iniciais', count(*) FROM contrato_vigencias
UNION ALL SELECT 'agenda_origem_tipificada', count(*) FROM agenda_eventos WHERE origem_tipo IS NOT NULL;

ROLLBACK;
