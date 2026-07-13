-- Somente leitura. Execute em clone ou producao com usuario read-only.
BEGIN TRANSACTION READ ONLY;

SELECT table_name, column_name, data_type, udt_name, numeric_precision, numeric_scale, is_nullable
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name IN ('contratos','agenda_eventos','ordens_servico','ordem_servico_produtos',
                     'produtos','estoque_movimentacoes','financeiro_lancamentos','financeiro_parcelas')
ORDER BY table_name, ordinal_position;

SELECT 'contratos' entidade, count(*) total FROM contratos
UNION ALL SELECT 'agenda_eventos', count(*) FROM agenda_eventos
UNION ALL SELECT 'ordens_servico', count(*) FROM ordens_servico
UNION ALL SELECT 'ordem_servico_produtos', count(*) FROM ordem_servico_produtos
UNION ALL SELECT 'produtos', count(*) FROM produtos
UNION ALL SELECT 'estoque_movimentacoes', count(*) FROM estoque_movimentacoes
UNION ALL SELECT 'financeiro_lancamentos', count(*) FROM financeiro_lancamentos
UNION ALL SELECT 'financeiro_parcelas', count(*) FROM financeiro_parcelas;

SELECT 'agenda_os_duplicada' ambiguidade, count(*) quantidade FROM (
  SELECT ordem_servico_id FROM agenda_eventos WHERE ordem_servico_id IS NOT NULL
  GROUP BY ordem_servico_id HAVING count(*) > 1
) x
UNION ALL
SELECT 'financeiro_os_duplicado', count(*) FROM (
  SELECT ordem_servico_id FROM financeiro_lancamentos WHERE ordem_servico_id IS NOT NULL AND status <> 'CANCELLED'
  GROUP BY ordem_servico_id HAVING count(*) > 1
) x
UNION ALL
SELECT 'quantidade_escala_maior_4', count(*) FROM estoque_movimentacoes
WHERE quantidade <> round(quantidade, 4)
UNION ALL
SELECT 'saldo_produto_escala_maior_4', count(*) FROM produtos
WHERE quantidade_atual <> round(quantidade_atual, 4)
UNION ALL
SELECT 'agenda_intervalo_invalido', count(*) FROM agenda_eventos
WHERE data_fim IS NOT NULL AND data_fim <= data_inicio;

ROLLBACK;
