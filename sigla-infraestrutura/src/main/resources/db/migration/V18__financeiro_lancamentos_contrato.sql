ALTER TABLE financeiro_lancamentos
  ADD COLUMN IF NOT EXISTS contrato_id uuid REFERENCES contratos(id);

CREATE INDEX IF NOT EXISTS idx_financeiro_lancamentos_contrato
  ON financeiro_lancamentos (contrato_id);
