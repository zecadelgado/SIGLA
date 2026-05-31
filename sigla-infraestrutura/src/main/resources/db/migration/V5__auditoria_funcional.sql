CREATE TABLE IF NOT EXISTS auditoria_eventos (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  entidade_tipo text NOT NULL,
  entidade_id text NOT NULL,
  acao text NOT NULL,
  detalhe text,
  usuario_id uuid REFERENCES usuarios(id),
  created_at timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_auditoria_eventos_entidade
  ON auditoria_eventos (entidade_tipo, entidade_id);

CREATE INDEX IF NOT EXISTS idx_auditoria_eventos_created_at
  ON auditoria_eventos (created_at);
