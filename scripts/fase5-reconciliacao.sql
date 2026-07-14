-- Fase 5 — reconciliação (SOMENTE LEITURA).
-- Confere os invariantes transversais após aplicar V18–V26.

-- 1) Estoque: saldo materializado × (baseline + razão imutável).
--    Esperado: zero linhas.
SELECT * FROM reconciliar_estoque_v1();

-- 2) Mensalidades: no máximo uma ativa por contrato/competência.
--    Esperado: zero linhas.
SELECT contrato_id, competencia, count(*)
  FROM financeiro_lancamentos
 WHERE origem_tipo = 'CONTRATO' AND status <> 'CANCELLED'
 GROUP BY contrato_id, competencia
HAVING count(*) > 1;

-- 3) Competência de mensalidade dentro da vigência que a originou.
--    Esperado: zero linhas.
SELECT l.id, l.contrato_id, l.competencia, v.data_inicio, v.data_fim
  FROM financeiro_lancamentos l
  JOIN contrato_vigencias v ON v.id = l.contrato_vigencia_id
 WHERE l.origem_tipo = 'CONTRATO'
   AND (l.competencia < date_trunc('month', v.data_inicio)::date
     OR (v.data_fim IS NOT NULL AND l.competencia > v.data_fim));

-- 4) Compensações nunca superam o valor liquidado do lançamento original.
--    Esperado: zero linhas.
SELECT c.lancamento_original_id,
       sum(c.valor) AS compensado,
       max(l.valor_total) AS valor_original
  FROM financeiro_compensacoes c
  JOIN financeiro_lancamentos l ON l.id = c.lancamento_original_id
 WHERE upper(c.status) NOT IN ('FALHOU', 'CANCELADA')
 GROUP BY c.lancamento_original_id
HAVING sum(c.valor) > max(l.valor_total);

-- 5) Crédito de cliente é razão de movimentos: saldo agregado por cliente.
SELECT * FROM vw_cliente_credito_saldo_v1 ORDER BY saldo DESC;

-- 6) Estados por relógio não persistidos. Esperado: zero linhas.
SELECT id, status FROM financeiro_lancamentos WHERE upper(status) = 'OVERDUE'
UNION ALL
SELECT id, status FROM financeiro_parcelas WHERE upper(status) = 'OVERDUE';

-- 7) Pagos/parciais jamais cancelados por cima (auditoria de sanidade das
--    compensações: cancelamentos de pagos devem inexistir).
SELECT a.entidade_id, a.acao, a.detalhe, a.created_at
  FROM auditoria_eventos a
 WHERE a.entidade_tipo = 'financeiro_lancamentos'
   AND a.acao LIKE 'COMPENSACAO_%'
 ORDER BY a.created_at DESC LIMIT 50;
