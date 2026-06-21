-- Soft-delete (inativar/reativar) para os responsaveis do cliente.
-- Migracao estritamente aditiva: adiciona a coluna `ativo` na tabela
-- cliente_responsaveis. Registros existentes ficam ativos por padrao.
ALTER TABLE cliente_responsaveis
    ADD COLUMN IF NOT EXISTS ativo boolean NOT NULL DEFAULT true;
