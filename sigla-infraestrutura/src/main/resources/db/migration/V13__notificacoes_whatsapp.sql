-- Sistema configuravel de notificacoes por WhatsApp (via webhook n8n).
-- Migracao estritamente aditiva: cria a tabela de configuracoes/templates e
-- expande `notificacoes` (e `ordens_servico`) com os campos de envio. Nada e removido.

-- 1) Configuracoes / templates de notificacao
CREATE TABLE IF NOT EXISTS notificacao_configuracoes (
  id                 varchar(120) PRIMARY KEY,
  event_type         varchar(32)  NOT NULL,
  nome               varchar(200) NOT NULL,
  titulo             varchar(200) NOT NULL,
  template_mensagem  text         NOT NULL,
  destinatario       varchar(24)  NOT NULL,
  origem_tipo        varchar(24)  NOT NULL DEFAULT 'SISTEMA',
  canal              varchar(32)  NOT NULL DEFAULT 'WHATSAPP_N8N',
  fonte_telefone     varchar(24)  NOT NULL,
  telefone_informado varchar(32),
  automatico         boolean      NOT NULL DEFAULT true,
  dias_antecedencia  integer,
  ativo              boolean      NOT NULL DEFAULT true,
  criado_por         varchar(120),
  created_at         timestamp    NOT NULL DEFAULT now(),
  updated_at         timestamp    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notif_config_evento_ativo ON notificacao_configuracoes(event_type, ativo);

-- 2) Campos ricos de envio em `notificacoes`
ALTER TABLE notificacoes ALTER COLUMN id TYPE varchar(120);
ALTER TABLE notificacoes ALTER COLUMN title TYPE varchar(200);
ALTER TABLE notificacoes ALTER COLUMN message TYPE text;

ALTER TABLE notificacoes ADD COLUMN IF NOT EXISTS recipient_type  varchar(24);
ALTER TABLE notificacoes ADD COLUMN IF NOT EXISTS recipient_name  varchar(200);
ALTER TABLE notificacoes ADD COLUMN IF NOT EXISTS recipient_phone varchar(32);
ALTER TABLE notificacoes ADD COLUMN IF NOT EXISTS sender_type     varchar(24);
ALTER TABLE notificacoes ADD COLUMN IF NOT EXISTS sender_name     varchar(200);
ALTER TABLE notificacoes ADD COLUMN IF NOT EXISTS customer_id     varchar(64);
ALTER TABLE notificacoes ADD COLUMN IF NOT EXISTS employee_id     varchar(64);
ALTER TABLE notificacoes ADD COLUMN IF NOT EXISTS template_id     varchar(120);
ALTER TABLE notificacoes ADD COLUMN IF NOT EXISTS scheduled_for   timestamp;
ALTER TABLE notificacoes ADD COLUMN IF NOT EXISTS source          varchar(32) DEFAULT 'SIGLA';
ALTER TABLE notificacoes ADD COLUMN IF NOT EXISTS channel         varchar(32);
ALTER TABLE notificacoes ADD COLUMN IF NOT EXISTS attempts        integer DEFAULT 0;
ALTER TABLE notificacoes ADD COLUMN IF NOT EXISTS last_error      text;
ALTER TABLE notificacoes ADD COLUMN IF NOT EXISTS metadata        text;
ALTER TABLE notificacoes ADD COLUMN IF NOT EXISTS sent_at         timestamp;
ALTER TABLE notificacoes ADD COLUMN IF NOT EXISTS created_by      varchar(120);

CREATE INDEX IF NOT EXISTS idx_notificacoes_status_scheduled ON notificacoes(status, scheduled_for);

-- 3) Opt-in de notificacao em ordens de servico (alinhado a agenda_eventos)
ALTER TABLE ordens_servico ADD COLUMN IF NOT EXISTS lembrete_ativo             boolean DEFAULT false;
ALTER TABLE ordens_servico ADD COLUMN IF NOT EXISTS dias_antecedencia_lembrete integer DEFAULT 1;
ALTER TABLE ordens_servico ADD COLUMN IF NOT EXISTS notificar_cliente          boolean DEFAULT false;
ALTER TABLE ordens_servico ADD COLUMN IF NOT EXISTS notificar_funcionario      boolean DEFAULT false;
