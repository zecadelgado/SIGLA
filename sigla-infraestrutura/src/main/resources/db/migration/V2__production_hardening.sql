CREATE TABLE IF NOT EXISTS notificacoes (
  id varchar(80) PRIMARY KEY,
  type varchar(32) NOT NULL,
  title varchar(120) NOT NULL,
  message varchar(500) NOT NULL,
  related_entity_id varchar(64) NOT NULL,
  trigger_date date NOT NULL,
  status varchar(24) NOT NULL DEFAULT 'OPEN',
  read_at timestamptz,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notificacoes_status_trigger ON notificacoes(status, trigger_date);
CREATE INDEX IF NOT EXISTS idx_notificacoes_related_entity ON notificacoes(related_entity_id);

CREATE OR REPLACE FUNCTION sigla_touch_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
DECLARE
  target_table text;
BEGIN
  FOREACH target_table IN ARRAY ARRAY[
    'cadastro',
    'usuarios',
    'cliente_indicacoes',
    'contratos',
    'ordens_servico',
    'agenda_eventos',
    'certificados',
    'financeiro_lancamentos',
    'notificacoes'
  ]
  LOOP
    IF EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = 'public'
        AND table_name = target_table
        AND column_name = 'updated_at'
    )
    AND NOT EXISTS (
      SELECT 1
      FROM pg_trigger
      WHERE tgname = 'trg_' || target_table || '_updated_at'
    ) THEN
      EXECUTE format(
        'CREATE TRIGGER %I BEFORE UPDATE ON %I FOR EACH ROW EXECUTE FUNCTION sigla_touch_updated_at()',
        'trg_' || target_table || '_updated_at',
        target_table
      );
    END IF;
  END LOOP;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_cadastro_cpf_nonblank ON cadastro(cpf) WHERE cpf IS NOT NULL AND cpf <> '';
CREATE UNIQUE INDEX IF NOT EXISTS ux_cadastro_cnpj_nonblank ON cadastro(cnpj) WHERE cnpj IS NOT NULL AND cnpj <> '';
CREATE UNIQUE INDEX IF NOT EXISTS ux_cadastro_email_nonblank ON cadastro(lower(email)) WHERE email IS NOT NULL AND email <> '';

CREATE INDEX IF NOT EXISTS idx_cadastro_tipo_ativo ON cadastro(tipo, ativo);
CREATE INDEX IF NOT EXISTS idx_cliente_responsaveis_cliente ON cliente_responsaveis(cliente_id);
CREATE INDEX IF NOT EXISTS idx_cliente_indicacoes_indicador ON cliente_indicacoes(cliente_indicador_id);
CREATE INDEX IF NOT EXISTS idx_contratos_cliente_status ON contratos(cliente_id, status);
CREATE INDEX IF NOT EXISTS idx_contratos_data_fim ON contratos(data_fim);
CREATE INDEX IF NOT EXISTS idx_ordens_cliente_status ON ordens_servico(cliente_id, status);
CREATE INDEX IF NOT EXISTS idx_ordens_data_agendada ON ordens_servico(data_agendada);
CREATE INDEX IF NOT EXISTS idx_agenda_cliente_inicio ON agenda_eventos(cliente_id, data_inicio);
CREATE INDEX IF NOT EXISTS idx_agenda_status_inicio ON agenda_eventos(status, data_inicio);
CREATE INDEX IF NOT EXISTS idx_certificados_cliente_validade ON certificados(cliente_id, data_validade);
CREATE INDEX IF NOT EXISTS idx_estoque_movimentacoes_produto_data ON estoque_movimentacoes(produto_id, data_movimentacao);
CREATE INDEX IF NOT EXISTS idx_financeiro_lancamentos_status_vencimento ON financeiro_lancamentos(status, data_vencimento);
CREATE INDEX IF NOT EXISTS idx_financeiro_lancamentos_cliente ON financeiro_lancamentos(cliente_id);
CREATE INDEX IF NOT EXISTS idx_financeiro_parcelas_lancamento ON financeiro_parcelas(lancamento_id);
CREATE INDEX IF NOT EXISTS idx_os_produtos_ordem ON ordem_servico_produtos(ordem_servico_id);
CREATE INDEX IF NOT EXISTS idx_os_anexos_ordem ON ordem_servico_anexos(ordem_servico_id);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_cadastro_tipo') THEN
    ALTER TABLE cadastro ADD CONSTRAINT chk_cadastro_tipo CHECK (tipo IN ('CLIENTE', 'FUNCIONARIO'));
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_usuarios_tipo') THEN
    ALTER TABLE usuarios ADD CONSTRAINT chk_usuarios_tipo CHECK (tipo IN ('ADMIN', 'OPERADOR', 'FINANCEIRO', 'TECNICO'));
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_produtos_valores_quantidades') THEN
    ALTER TABLE produtos ADD CONSTRAINT chk_produtos_valores_quantidades CHECK (
      COALESCE(valor_custo, 0) >= 0
      AND COALESCE(valor_venda, 0) >= 0
      AND COALESCE(quantidade_atual, 0) >= 0
      AND COALESCE(quantidade_minima, 0) >= 0
    );
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_movimentacoes_quantidade_valor') THEN
    ALTER TABLE estoque_movimentacoes ADD CONSTRAINT chk_movimentacoes_quantidade_valor CHECK (
      quantidade > 0
      AND COALESCE(valor_unitario, 0) >= 0
      AND COALESCE(valor_total, 0) >= 0
    );
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_financeiro_valor_total') THEN
    ALTER TABLE financeiro_lancamentos ADD CONSTRAINT chk_financeiro_valor_total CHECK (COALESCE(valor_total, 0) >= 0);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_financeiro_parcela_valor') THEN
    ALTER TABLE financeiro_parcelas ADD CONSTRAINT chk_financeiro_parcela_valor CHECK (COALESCE(valor_parcela, 0) >= 0);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_ordem_valor_servico') THEN
    ALTER TABLE ordens_servico ADD CONSTRAINT chk_ordem_valor_servico CHECK (COALESCE(valor_servico, 0) >= 0);
  END IF;
END $$;

INSERT INTO usuarios (nome, usuario, email, senha, tipo, ativo)
VALUES (
  'Administrador',
  'admin',
  'admin@sigla.local',
  '$2b$10$Xb14BzH2mgGtLeMPiIFY5.6C75KKjb6vEGIsxQoiJ6fOaqdz.Omey',
  'ADMIN',
  true
)
ON CONFLICT (usuario) DO NOTHING;

UPDATE usuarios
SET senha = '$2b$10$Xb14BzH2mgGtLeMPiIFY5.6C75KKjb6vEGIsxQoiJ6fOaqdz.Omey',
    tipo = 'ADMIN',
    ativo = true,
    updated_at = now()
WHERE usuario = 'admin'
  AND senha = 'sigla123';

INSERT INTO financeiro_formas_pagamento (nome, ativo)
VALUES ('PIX', true), ('DINHEIRO', true), ('BOLETO', true), ('CARTAO', true)
ON CONFLICT (nome) DO NOTHING;

INSERT INTO financeiro_categorias (tipo, nome, ativo)
VALUES
  ('ENTRY', 'SERVICOS', true),
  ('ENTRY', 'CONTRATOS', true),
  ('EXPENSE', 'COMBUSTIVEL', true),
  ('EXPENSE', 'PRODUTOS', true),
  ('EXPENSE', 'ALIMENTACAO', true),
  ('EXPENSE', 'EXTRAS', true);
