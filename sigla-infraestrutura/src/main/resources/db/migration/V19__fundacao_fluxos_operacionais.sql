CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Identidade estavel do contrato e suas vigencias imutaveis/versionadas.
CREATE TABLE contrato_vigencias (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  contrato_id uuid NOT NULL REFERENCES contratos(id),
  versao integer NOT NULL,
  data_inicio date NOT NULL,
  data_fim date,
  valor_mensal numeric(19,2) NOT NULL DEFAULT 0,
  tipo_contrato text NOT NULL,
  renovacao_tipo text NOT NULL DEFAULT 'INICIAL',
  renovacao_autorizada boolean NOT NULL DEFAULT false,
  vigencia_anterior_id uuid REFERENCES contrato_vigencias(id),
  chave_idempotencia varchar(160) NOT NULL,
  criado_por uuid REFERENCES usuarios(id),
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT chk_contrato_vigencia_periodo CHECK (data_fim IS NULL OR data_fim >= data_inicio),
  CONSTRAINT chk_contrato_vigencia_valor CHECK (valor_mensal >= 0),
  CONSTRAINT ux_contrato_vigencia_versao UNIQUE (contrato_id, versao),
  CONSTRAINT ux_contrato_vigencia_chave UNIQUE (chave_idempotencia),
  CONSTRAINT ex_contrato_vigencia_sem_sobreposicao EXCLUDE USING gist
    (contrato_id WITH =, daterange(data_inicio, COALESCE(data_fim + 1, 'infinity'::date), '[)') WITH &&)
);

-- Nova escrita usa escala 4; colunas legadas continuam disponiveis para leitura.
ALTER TABLE produtos
  ADD COLUMN quantidade_atual_decimal numeric(19,4),
  ADD COLUMN quantidade_minima_decimal numeric(19,4);

ALTER TABLE estoque_movimentacoes
  ADD COLUMN quantidade_decimal numeric(19,4),
  ADD COLUMN chave_idempotencia varchar(180),
  ADD COLUMN movimento_compensado_id uuid REFERENCES estoque_movimentacoes(id),
  ADD COLUMN versao integer NOT NULL DEFAULT 0;

ALTER TABLE ordem_servico_produtos
  ADD COLUMN quantidade_solicitada numeric(19,4),
  ADD COLUMN quantidade_reservada numeric(19,4) NOT NULL DEFAULT 0,
  ADD COLUMN quantidade_consumida numeric(19,4) NOT NULL DEFAULT 0,
  ADD COLUMN estado_estoque text NOT NULL DEFAULT 'PENDENTE',
  ADD COLUMN chave_idempotencia varchar(180),
  ADD COLUMN versao integer NOT NULL DEFAULT 0;

ALTER TABLE ordem_servico_produtos ADD CONSTRAINT chk_os_produtos_quantidades_decimais CHECK (
  COALESCE(quantidade_solicitada, 0) >= 0 AND quantidade_reservada >= 0 AND quantidade_consumida >= 0
) NOT VALID;
ALTER TABLE ordem_servico_produtos ADD CONSTRAINT chk_os_produtos_estado_estoque CHECK (
  estado_estoque IN ('PENDENTE','RESERVADA','CONSUMIDA','DEVOLVIDA','COMPENSADA')
) NOT VALID;
CREATE UNIQUE INDEX ux_estoque_movimentacoes_chave ON estoque_movimentacoes(chave_idempotencia)
  WHERE chave_idempotencia IS NOT NULL;
CREATE UNIQUE INDEX ux_os_produtos_chave ON ordem_servico_produtos(chave_idempotencia)
  WHERE chave_idempotencia IS NOT NULL;

-- Ocorrencia materializada liga inequivocamente vigencia, agenda e OS.
CREATE TABLE ocorrencias_operacionais (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  contrato_vigencia_id uuid REFERENCES contrato_vigencias(id),
  ordem_servico_id uuid NOT NULL REFERENCES ordens_servico(id),
  agenda_evento_id uuid REFERENCES agenda_eventos(id),
  sequencia integer NOT NULL DEFAULT 1,
  inicio_previsto timestamptz NOT NULL,
  fim_previsto timestamptz,
  timezone varchar(64) NOT NULL DEFAULT 'America/Sao_Paulo',
  origem_tipo text NOT NULL,
  chave_idempotencia varchar(180) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT chk_ocorrencia_periodo CHECK (fim_previsto IS NULL OR fim_previsto > inicio_previsto),
  CONSTRAINT chk_ocorrencia_origem CHECK (origem_tipo IN ('CONTRATO','MANUAL','CERTIFICADO')),
  CONSTRAINT ux_ocorrencia_os UNIQUE (ordem_servico_id),
  CONSTRAINT ux_ocorrencia_agenda UNIQUE (agenda_evento_id),
  CONSTRAINT ux_ocorrencia_chave UNIQUE (chave_idempotencia)
);

ALTER TABLE agenda_eventos
  ADD COLUMN origem_tipo text,
  ADD COLUMN contrato_vigencia_id uuid REFERENCES contrato_vigencias(id),
  ADD COLUMN ocorrencia_id uuid REFERENCES ocorrencias_operacionais(id),
  ADD COLUMN timezone varchar(64),
  ADD COLUMN versao integer NOT NULL DEFAULT 0,
  ADD COLUMN chave_idempotencia varchar(180);
ALTER TABLE agenda_eventos ADD CONSTRAINT chk_agenda_origem_tipo CHECK (
  origem_tipo IS NULL OR origem_tipo IN ('OPERACIONAL','CONTRATO','CERTIFICADO','MANUAL')
) NOT VALID;
CREATE UNIQUE INDEX ux_agenda_chave_idempotencia ON agenda_eventos(chave_idempotencia)
  WHERE chave_idempotencia IS NOT NULL;

ALTER TABLE ordens_servico
  ADD COLUMN contrato_vigencia_id uuid REFERENCES contrato_vigencias(id),
  ADD COLUMN ocorrencia_id uuid REFERENCES ocorrencias_operacionais(id),
  ADD COLUMN versao integer NOT NULL DEFAULT 0,
  ADD COLUMN chave_idempotencia varchar(180);
CREATE UNIQUE INDEX ux_os_chave_idempotencia ON ordens_servico(chave_idempotencia)
  WHERE chave_idempotencia IS NOT NULL;

-- Origem, competencia civil local e unicidade financeira.
ALTER TABLE financeiro_lancamentos
  ADD COLUMN origem_tipo text,
  ADD COLUMN origem_id uuid,
  ADD COLUMN contrato_vigencia_id uuid REFERENCES contrato_vigencias(id),
  ADD COLUMN competencia date,
  ADD COLUMN chave_idempotencia varchar(180),
  ADD COLUMN lancamento_original_id uuid REFERENCES financeiro_lancamentos(id),
  ADD COLUMN versao integer NOT NULL DEFAULT 0;
ALTER TABLE financeiro_lancamentos ADD CONSTRAINT chk_financeiro_origem_tipo CHECK (
  origem_tipo IS NULL OR origem_tipo IN ('ORDEM_SERVICO','CONTRATO','MANUAL','COMPENSACAO')
) NOT VALID;
ALTER TABLE financeiro_lancamentos ADD CONSTRAINT chk_financeiro_competencia CHECK (
  competencia IS NULL OR competencia = date_trunc('month', competencia)::date
) NOT VALID;
CREATE UNIQUE INDEX ux_financeiro_chave_idempotencia ON financeiro_lancamentos(chave_idempotencia)
  WHERE chave_idempotencia IS NOT NULL;
CREATE UNIQUE INDEX ux_financeiro_origem_competencia ON financeiro_lancamentos(
  origem_tipo, origem_id, COALESCE(contrato_vigencia_id, '00000000-0000-0000-0000-000000000000'::uuid), competencia
) WHERE origem_tipo IS NOT NULL AND origem_id IS NOT NULL AND competencia IS NOT NULL
    AND status <> 'CANCELLED';

CREATE TABLE historico_status (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  entidade_tipo text NOT NULL,
  entidade_id uuid NOT NULL,
  status_anterior text,
  status_novo text NOT NULL,
  evento_tipo text NOT NULL DEFAULT 'TRANSICAO',
  motivo text,
  usuario_id uuid REFERENCES usuarios(id),
  comando_id uuid,
  chave_idempotencia varchar(180) NOT NULL,
  ocorrido_em timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT chk_historico_evento CHECK (evento_tipo IN ('TRANSICAO','ANULACAO','REABERTURA')),
  CONSTRAINT ux_historico_chave UNIQUE (chave_idempotencia)
);
CREATE INDEX idx_historico_entidade ON historico_status(entidade_tipo, entidade_id, ocorrido_em);

CREATE TABLE financeiro_compensacoes (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  lancamento_original_id uuid NOT NULL REFERENCES financeiro_lancamentos(id),
  lancamento_compensatorio_id uuid REFERENCES financeiro_lancamentos(id),
  tipo text NOT NULL,
  valor numeric(19,2) NOT NULL,
  motivo text NOT NULL,
  status text NOT NULL DEFAULT 'SOLICITADA',
  meio text,
  chave_idempotencia varchar(180) NOT NULL,
  criado_por uuid REFERENCES usuarios(id),
  created_at timestamptz NOT NULL DEFAULT now(),
  processed_at timestamptz,
  CONSTRAINT chk_compensacao_tipo CHECK (tipo IN ('ESTORNO','CREDITO','REEMBOLSO')),
  CONSTRAINT chk_compensacao_status CHECK (status IN ('SOLICITADA','CONFIRMADA','FALHOU','CANCELADA')),
  CONSTRAINT chk_compensacao_valor CHECK (valor > 0),
  CONSTRAINT ux_compensacao_chave UNIQUE (chave_idempotencia)
);

CREATE TABLE cliente_credito_movimentos (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  cliente_id uuid NOT NULL REFERENCES cadastro(id),
  compensacao_id uuid REFERENCES financeiro_compensacoes(id),
  tipo text NOT NULL,
  valor numeric(19,2) NOT NULL,
  chave_idempotencia varchar(180) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT chk_credito_tipo CHECK (tipo IN ('CREDITO','UTILIZACAO','COMPENSACAO')),
  CONSTRAINT chk_credito_valor CHECK (valor > 0),
  CONSTRAINT ux_credito_chave UNIQUE (chave_idempotencia)
);
CREATE INDEX idx_credito_cliente ON cliente_credito_movimentos(cliente_id, created_at);

CREATE TABLE os_anulacoes (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  ordem_servico_id uuid NOT NULL REFERENCES ordens_servico(id),
  motivo text NOT NULL,
  historico_id uuid REFERENCES historico_status(id),
  chave_idempotencia varchar(180) NOT NULL,
  anulado_por uuid REFERENCES usuarios(id),
  anulado_em timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ux_os_anulacao_os UNIQUE (ordem_servico_id),
  CONSTRAINT ux_os_anulacao_chave UNIQUE (chave_idempotencia)
);
