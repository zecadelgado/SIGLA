DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_cadastro_tipo') THEN
    ALTER TABLE cadastro DROP CONSTRAINT chk_cadastro_tipo;
  END IF;

  ALTER TABLE cadastro ADD CONSTRAINT chk_cadastro_tipo CHECK (
    tipo IN ('CLIENTE', 'FUNCIONARIO', 'pessoa_fisica', 'pessoa_juridica', 'PESSOA_FISICA', 'PESSOA_JURIDICA')
  );
END $$;

DROP INDEX IF EXISTS ux_cadastro_cpf_nonblank;
DROP INDEX IF EXISTS ux_cadastro_cnpj_nonblank;
DROP INDEX IF EXISTS ux_cadastro_email_nonblank;

CREATE UNIQUE INDEX IF NOT EXISTS ux_cadastro_cpf_ativo_nonblank
  ON cadastro(regexp_replace(cpf, '\D', '', 'g'))
  WHERE ativo IS TRUE AND cpf IS NOT NULL AND cpf <> '';

CREATE UNIQUE INDEX IF NOT EXISTS ux_cadastro_cnpj_ativo_nonblank
  ON cadastro(regexp_replace(cnpj, '\D', '', 'g'))
  WHERE ativo IS TRUE AND cnpj IS NOT NULL AND cnpj <> '';

CREATE UNIQUE INDEX IF NOT EXISTS ux_cadastro_email_ativo_nonblank
  ON cadastro(lower(email))
  WHERE ativo IS TRUE AND email IS NOT NULL AND email <> '';

CREATE INDEX IF NOT EXISTS idx_cliente_indicacoes_status_data
  ON cliente_indicacoes(status, data_indicacao);
