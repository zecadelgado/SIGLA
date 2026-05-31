ALTER TABLE agenda_eventos
ADD COLUMN IF NOT EXISTS certificado_id uuid NULL;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'fk_agenda_eventos_certificado'
  ) THEN
    ALTER TABLE agenda_eventos
      ADD CONSTRAINT fk_agenda_eventos_certificado
      FOREIGN KEY (certificado_id)
      REFERENCES certificados(id);
  END IF;
END $$;
