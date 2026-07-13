-- Eventos operacionais ativos do mesmo responsavel nao podem se sobrepor.
-- A constraint e DEFERRABLE para permitir reagendamento atomico em uma transacao.
ALTER TABLE agenda_eventos
  ADD CONSTRAINT ex_agenda_operacional_responsavel_horario
  EXCLUDE USING gist (
    responsavel_id WITH =,
    tstzrange(data_inicio, COALESCE(data_fim, 'infinity'::timestamptz), '[)') WITH &&
  )
  WHERE (
    responsavel_id IS NOT NULL
    AND origem_tipo = 'OPERACIONAL'
    AND status NOT IN ('CANCELLED','COMPLETED')
    AND (data_fim IS NULL OR data_fim > data_inicio)
  )
  DEFERRABLE INITIALLY IMMEDIATE;
