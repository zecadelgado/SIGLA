-- Fase 5 — diagnóstico de financeiro e contratos (SOMENTE LEITURA).
-- Rodar em homologação/cópia antes e depois de aplicar V18–V26.
-- Nenhuma inferência é feita: itens listados aqui exigem decisão humana ou
-- ficam aguardando classificação explícita.

-- 1) Contratos sem vigência (a V26 cria a vigência inicial estrutural;
--    sobra aqui somente contrato sem data_inicio).
SELECT c.id, c.cliente_id, c.status, c.data_inicio, c.data_fim
  FROM contratos c
 WHERE NOT EXISTS (SELECT 1 FROM contrato_vigencias v WHERE v.contrato_id = c.id);

-- 2) OS contratuais legadas ainda sem regra explícita de cobrança
--    (relatório de conciliação — não gerar/cancelar lançamento automaticamente).
SELECT * FROM vw_os_contratuais_sem_regra_v1 ORDER BY data_agendada;

-- 3) Lançamentos legados sem origem formal (anteriores à V26; escritas novas
--    sempre recebem origem pela trigger).
SELECT l.id, l.tipo, l.descricao, l.cliente_id, l.ordem_servico_id, l.contrato_id,
       l.valor_total, l.status, l.created_at
  FROM financeiro_lancamentos l
 WHERE l.origem_tipo IS NULL
 ORDER BY l.created_at;

-- 4) Possíveis cobranças duplicadas legadas da mesma OS (a trigger bloqueia
--    só escritas novas; duplicatas antigas exigem compensação manual).
SELECT l.ordem_servico_id, count(*) AS cobrancas_ativas,
       array_agg(l.id ORDER BY l.created_at) AS lancamentos
  FROM financeiro_lancamentos l
 WHERE l.tipo = 'ENTRY' AND l.ordem_servico_id IS NOT NULL
   AND upper(COALESCE(l.status, '')) <> 'CANCELLED'
 GROUP BY l.ordem_servico_id
HAVING count(*) > 1;

-- 5) Mensalidades legadas duplicadas por contrato/mês (sem origem formal,
--    identificadas pelo texto-marcador do produtor Java antigo — apenas para
--    triagem humana, nunca para ação automática).
SELECT l.contrato_id, date_trunc('month', l.data_vencimento) AS mes,
       count(*) AS lancamentos
  FROM financeiro_lancamentos l
 WHERE l.contrato_id IS NOT NULL AND l.origem_tipo IS NULL AND l.tipo = 'ENTRY'
 GROUP BY l.contrato_id, date_trunc('month', l.data_vencimento)
HAVING count(*) > 1;

-- 6) Compensações pendentes de conclusão (reembolsos solicitados).
SELECT c.id, c.lancamento_original_id, c.tipo, c.valor, c.status, c.created_at
  FROM financeiro_compensacoes c
 WHERE c.status = 'SOLICITADA'
 ORDER BY c.created_at;

-- 7) Execuções recentes do faturamento (autoridade única).
SELECT * FROM sigla_faturamento_execucoes ORDER BY executado_em DESC LIMIT 30;
