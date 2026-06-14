CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS cadastro (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tipo text NOT NULL DEFAULT 'CLIENTE',
  nome text,
  razao_social text,
  nome_fantasia text,
  cnpj varchar,
  cpf varchar,
  telefone_principal varchar,
  email text,
  cep varchar,
  rua text,
  numero varchar,
  complemento text,
  bairro text,
  cidade text,
  estado varchar,
  observacoes text,
  ativo boolean DEFAULT true,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);

CREATE TABLE IF NOT EXISTS usuarios (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  nome text NOT NULL,
  usuario text NOT NULL UNIQUE,
  email text UNIQUE,
  senha text NOT NULL,
  tipo text DEFAULT 'ADMIN',
  ativo boolean DEFAULT true,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);

CREATE TABLE IF NOT EXISTS cliente_responsaveis (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  cliente_id uuid NOT NULL REFERENCES cadastro(id),
  nome text NOT NULL,
  cargo text,
  telefone varchar,
  email text,
  principal boolean DEFAULT false,
  created_at timestamptz DEFAULT now()
);

CREATE TABLE IF NOT EXISTS cliente_indicacoes (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  nome_indicado text NOT NULL,
  telefone varchar,
  cliente_indicador_id uuid REFERENCES cadastro(id),
  data_indicacao date DEFAULT CURRENT_DATE,
  status text DEFAULT 'NEW',
  observacoes text,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);

CREATE TABLE IF NOT EXISTS produtos (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  nome text NOT NULL,
  descricao text,
  sku text,
  unidade varchar DEFAULT 'UN',
  valor_custo numeric DEFAULT 0,
  valor_venda numeric DEFAULT 0,
  quantidade_atual numeric DEFAULT 0,
  quantidade_minima numeric DEFAULT 0,
  ativo boolean DEFAULT true,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);

CREATE TABLE IF NOT EXISTS contratos (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  cliente_id uuid NOT NULL REFERENCES cadastro(id),
  descricao text,
  tipo_contrato text DEFAULT 'MENSAL',
  data_inicio date NOT NULL,
  data_fim date,
  valor_mensal numeric DEFAULT 0,
  alerta_ativo boolean DEFAULT true,
  dias_alerta_fim integer DEFAULT 30,
  status text DEFAULT 'ATIVO',
  observacoes text,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ordens_servico (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  numero_os integer GENERATED ALWAYS AS IDENTITY UNIQUE NOT NULL,
  cliente_id uuid REFERENCES cadastro(id),
  contrato_id uuid REFERENCES contratos(id),
  titulo text NOT NULL,
  descricao text,
  tipo_servico text,
  status text DEFAULT 'SCHEDULED',
  data_agendada timestamptz,
  data_inicio timestamptz,
  data_fim timestamptz,
  responsavel_interno_id uuid REFERENCES cadastro(id),
  executado_por_id uuid REFERENCES cadastro(id),
  foi_feito boolean DEFAULT false,
  pago boolean DEFAULT false,
  valor_servico numeric DEFAULT 0,
  assinatura_cliente boolean DEFAULT false,
  observacoes text,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);

CREATE TABLE IF NOT EXISTS agenda_eventos (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  cliente_id uuid REFERENCES cadastro(id),
  ordem_servico_id uuid REFERENCES ordens_servico(id),
  contrato_id uuid REFERENCES contratos(id),
  titulo text NOT NULL,
  descricao text,
  tipo_evento text DEFAULT 'SERVICO',
  recorrencia text DEFAULT 'AVULSO',
  data_inicio timestamptz NOT NULL,
  data_fim timestamptz,
  dia_inteiro boolean DEFAULT false,
  status text DEFAULT 'SCHEDULED',
  prioridade text DEFAULT 'NORMAL',
  responsavel_id uuid REFERENCES cadastro(id),
  lembrete_ativo boolean DEFAULT false,
  dias_antecedencia_lembrete integer DEFAULT 1,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);

CREATE TABLE IF NOT EXISTS certificados (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  cliente_id uuid NOT NULL REFERENCES cadastro(id),
  ordem_servico_id uuid REFERENCES ordens_servico(id),
  descricao text,
  data_emissao date NOT NULL,
  data_validade date,
  intervalo_meses integer DEFAULT 6,
  alerta_ativo boolean DEFAULT true,
  dias_alerta integer DEFAULT 15,
  status text DEFAULT 'VALIDO',
  observacoes text,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);

CREATE TABLE IF NOT EXISTS estoque_movimentacoes (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  produto_id uuid NOT NULL REFERENCES produtos(id),
  tipo_movimentacao text NOT NULL,
  quantidade numeric NOT NULL,
  valor_unitario numeric DEFAULT 0,
  valor_total numeric DEFAULT 0,
  usuario_id uuid REFERENCES usuarios(id),
  funcionario_id uuid REFERENCES cadastro(id),
  cliente_id uuid REFERENCES cadastro(id),
  ordem_servico_id uuid REFERENCES ordens_servico(id),
  quem_pegou text,
  quem_comprou text,
  destino_descricao text,
  observacoes text,
  data_movimentacao timestamptz DEFAULT now(),
  created_at timestamptz DEFAULT now()
);

CREATE TABLE IF NOT EXISTS financeiro_categorias (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tipo text NOT NULL,
  nome text NOT NULL,
  ativo boolean DEFAULT true,
  created_at timestamptz DEFAULT now()
);

CREATE TABLE IF NOT EXISTS financeiro_formas_pagamento (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  nome text NOT NULL UNIQUE,
  ativo boolean DEFAULT true,
  created_at timestamptz DEFAULT now()
);

CREATE TABLE IF NOT EXISTS financeiro_lancamentos (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tipo text NOT NULL,
  categoria_id uuid REFERENCES financeiro_categorias(id),
  forma_pagamento_id uuid REFERENCES financeiro_formas_pagamento(id),
  descricao text,
  cliente_id uuid REFERENCES cadastro(id),
  ordem_servico_id uuid REFERENCES ordens_servico(id),
  valor_total numeric DEFAULT 0,
  data_emissao date DEFAULT CURRENT_DATE,
  data_vencimento date,
  data_pagamento date,
  status text DEFAULT 'PENDING',
  parcelado boolean DEFAULT false,
  quantidade_parcelas integer DEFAULT 1,
  observacoes text,
  criado_por uuid REFERENCES usuarios(id),
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);

CREATE TABLE IF NOT EXISTS financeiro_parcelas (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  lancamento_id uuid NOT NULL REFERENCES financeiro_lancamentos(id),
  numero_parcela integer NOT NULL,
  valor_parcela numeric DEFAULT 0,
  data_vencimento date,
  data_pagamento date,
  status text DEFAULT 'PENDING',
  created_at timestamptz DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ordem_servico_produtos (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  ordem_servico_id uuid NOT NULL REFERENCES ordens_servico(id),
  produto_id uuid NOT NULL REFERENCES produtos(id),
  quantidade numeric DEFAULT 0,
  valor_unitario numeric DEFAULT 0,
  valor_total numeric DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ordem_servico_anexos (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  ordem_servico_id uuid NOT NULL REFERENCES ordens_servico(id),
  tipo_anexo text DEFAULT 'IMAGEM',
  nome_arquivo text NOT NULL,
  caminho_storage text NOT NULL,
  mime_type text,
  tamanho_bytes bigint,
  descricao text,
  uploaded_by uuid REFERENCES usuarios(id),
  created_at timestamptz DEFAULT now()
);
