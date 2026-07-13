-- Fase 3: antes de ativar os novos comandos, preserva e isola eventos legados
-- cujo fim nao e posterior ao inicio. O registro continua consultavel para
-- conciliacao, mas deixa de participar das projecoes e conflitos operacionais.
ALTER TABLE agenda_eventos
  ADD COLUMN legado_isolado boolean NOT NULL DEFAULT false,
  ADD COLUMN legado_isolado_motivo text;

CREATE TABLE agenda_eventos_legado_inconsistencias (
  agenda_evento_id uuid PRIMARY KEY REFERENCES agenda_eventos(id),
  tipo text NOT NULL,
  inicio_original timestamptz NOT NULL,
  fim_original timestamptz,
  detectado_em timestamptz NOT NULL DEFAULT now(),
  resolvido_em timestamptz,
  resolvido_por uuid REFERENCES usuarios(id),
  observacoes text
);

INSERT INTO agenda_eventos_legado_inconsistencias(
  agenda_evento_id, tipo, inicio_original, fim_original, observacoes
)
SELECT id, 'INTERVALO_INVALIDO', data_inicio, data_fim,
       'Isolado automaticamente antes do cutover da Fase 3; requer conciliacao manual.'
FROM agenda_eventos
WHERE data_fim IS NOT NULL AND data_fim <= data_inicio
ON CONFLICT (agenda_evento_id) DO NOTHING;

UPDATE agenda_eventos
SET legado_isolado = true,
    legado_isolado_motivo = 'INTERVALO_INVALIDO'
WHERE data_fim IS NOT NULL AND data_fim <= data_inicio;

ALTER TABLE agenda_eventos
  ADD CONSTRAINT chk_agenda_intervalo_valido
  CHECK (legado_isolado OR data_fim IS NULL OR data_fim > data_inicio) NOT VALID;

CREATE OR REPLACE VIEW vw_agenda_operacional_v1 AS
SELECT a.id, a.ordem_servico_id, a.ocorrencia_id, a.responsavel_id,
       a.data_inicio, a.data_fim,
       COALESCE(a.timezone, 'America/Sao_Paulo'::varchar(64))::varchar(64) AS timezone,
       a.status AS status_factual,
       CASE
         WHEN a.status IN ('CANCELLED','COMPLETED') THEN a.status
         WHEN a.data_fim < now() THEN 'MISSED'
         ELSE a.status
       END AS status_efetivo
FROM agenda_eventos a
WHERE a.origem_tipo = 'OPERACIONAL' AND NOT a.legado_isolado;
