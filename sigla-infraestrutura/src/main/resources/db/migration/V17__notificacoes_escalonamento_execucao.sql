-- Escalonamento de lembretes (30/15/7/1 dias) + controle de execucao do agendador.
-- Migracao estritamente aditiva: cria colunas de "conjunto de dias" mantendo os campos int
-- atuais como fallback/compat, a tabela de ledger de execucao e o intervalo de re-lembrete.
-- Nada e removido.

-- 1) Conjunto de dias de antecedencia (CSV, ex.: "30,15,7,1"). Vazio/null = lembrete desligado.
--    Os campos int existentes continuam validos como fallback quando o conjunto vier vazio.
ALTER TABLE agenda_eventos ADD COLUMN IF NOT EXISTS lembrete_dias_conjunto varchar(40);
ALTER TABLE contratos      ADD COLUMN IF NOT EXISTS alerta_dias_conjunto   varchar(40);
ALTER TABLE certificados   ADD COLUMN IF NOT EXISTS alerta_dias_conjunto   varchar(40);
ALTER TABLE ordens_servico ADD COLUMN IF NOT EXISTS lembrete_dias_conjunto varchar(40);

-- Backfill: preserva o estado atual (so quando o lembrete/alerta esta ativo e ha um valor).
UPDATE agenda_eventos
   SET lembrete_dias_conjunto = dias_antecedencia_lembrete::text
 WHERE lembrete_dias_conjunto IS NULL
   AND lembrete_ativo = true
   AND dias_antecedencia_lembrete IS NOT NULL
   AND dias_antecedencia_lembrete > 0;

UPDATE contratos
   SET alerta_dias_conjunto = dias_alerta_fim::text
 WHERE alerta_dias_conjunto IS NULL
   AND alerta_ativo = true
   AND dias_alerta_fim > 0;

UPDATE certificados
   SET alerta_dias_conjunto = dias_alerta::text
 WHERE alerta_dias_conjunto IS NULL
   AND alerta_ativo = true
   AND dias_alerta > 0;

UPDATE ordens_servico
   SET lembrete_dias_conjunto = dias_antecedencia_lembrete::text
 WHERE lembrete_dias_conjunto IS NULL
   AND lembrete_ativo = true
   AND dias_antecedencia_lembrete IS NOT NULL
   AND dias_antecedencia_lembrete > 0;

-- 2) Ledger de execucao do agendador: idempotencia por dia + catch-up de dias perdidos.
CREATE TABLE IF NOT EXISTS notificacao_execucao_log (
  data          date        PRIMARY KEY,
  executado_em  timestamp   NOT NULL DEFAULT now(),
  origem        varchar(32) NOT NULL DEFAULT 'SIGLA'
);
