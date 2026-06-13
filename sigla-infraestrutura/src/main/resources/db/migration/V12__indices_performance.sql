-- V12 - Indices de performance
-- Objetivo: acelerar as consultas com WHERE introduzidas na otimizacao (buscas por
-- CPF/CNPJ/e-mail normalizados, status, datas e chaves estrangeiras usadas em validacoes
-- e contagens de vinculo). Indices sao seguros e nao-destrutivos (revertiveis com DROP INDEX).

-- Cadastro (clientes/funcionarios): unicidade de CPF/CNPJ/e-mail e filtros por tipo/ativo.
-- Indices funcionais batem exatamente com as expressoes das queries EXISTS (digitos / lower).
CREATE INDEX IF NOT EXISTS idx_cadastro_tipo ON cadastro (tipo);
CREATE INDEX IF NOT EXISTS idx_cadastro_ativo ON cadastro (ativo);
CREATE INDEX IF NOT EXISTS idx_cadastro_cpf_digits
    ON cadastro (regexp_replace(coalesce(cpf, ''), '\D', '', 'g')) WHERE ativo;
CREATE INDEX IF NOT EXISTS idx_cadastro_cnpj_digits
    ON cadastro (regexp_replace(coalesce(cnpj, ''), '\D', '', 'g')) WHERE ativo;
CREATE INDEX IF NOT EXISTS idx_cadastro_email_lower
    ON cadastro (lower(coalesce(email, ''))) WHERE ativo;

-- Ordens de servico: filtros por cliente, status, data e responsaveis.
CREATE INDEX IF NOT EXISTS idx_ordens_servico_cliente ON ordens_servico (cliente_id);
CREATE INDEX IF NOT EXISTS idx_ordens_servico_status ON ordens_servico (status);
CREATE INDEX IF NOT EXISTS idx_ordens_servico_data_agendada ON ordens_servico (data_agendada);
CREATE INDEX IF NOT EXISTS idx_ordens_servico_responsavel ON ordens_servico (responsavel_interno_id);
CREATE INDEX IF NOT EXISTS idx_ordens_servico_executado_por ON ordens_servico (executado_por_id);
CREATE INDEX IF NOT EXISTS idx_ordens_servico_contrato ON ordens_servico (contrato_id);

-- Lancamentos financeiros: busca por OS (findByOrdemServicoId), cliente, status, tipo e vencimento.
CREATE INDEX IF NOT EXISTS idx_fin_lancamentos_ordem_servico ON financeiro_lancamentos (ordem_servico_id);
CREATE INDEX IF NOT EXISTS idx_fin_lancamentos_cliente ON financeiro_lancamentos (cliente_id);
CREATE INDEX IF NOT EXISTS idx_fin_lancamentos_status ON financeiro_lancamentos (status);
CREATE INDEX IF NOT EXISTS idx_fin_lancamentos_tipo ON financeiro_lancamentos (tipo);
CREATE INDEX IF NOT EXISTS idx_fin_lancamentos_data_venc ON financeiro_lancamentos (data_vencimento);

-- Agenda: conflito por responsavel (findByResponsibleId) e filtros por cliente/periodo.
CREATE INDEX IF NOT EXISTS idx_agenda_responsavel ON agenda_eventos (responsavel_id);
CREATE INDEX IF NOT EXISTS idx_agenda_cliente ON agenda_eventos (cliente_id);
CREATE INDEX IF NOT EXISTS idx_agenda_data_inicio ON agenda_eventos (data_inicio);

-- Estoque: SKU normalizado (existsActiveSku) e movimentacoes por OS/cliente/funcionario.
CREATE INDEX IF NOT EXISTS idx_produtos_ativo ON produtos (ativo);
CREATE INDEX IF NOT EXISTS idx_produtos_sku_lower
    ON produtos (lower(trim(coalesce(sku, '')))) WHERE ativo;
CREATE INDEX IF NOT EXISTS idx_estoque_mov_produto ON estoque_movimentacoes (produto_id);
CREATE INDEX IF NOT EXISTS idx_estoque_mov_ordem_servico ON estoque_movimentacoes (ordem_servico_id);
CREATE INDEX IF NOT EXISTS idx_estoque_mov_cliente ON estoque_movimentacoes (cliente_id);
CREATE INDEX IF NOT EXISTS idx_estoque_mov_funcionario ON estoque_movimentacoes (funcionario_id);

-- Vinculos usados nas contagens de hasLinkedRecords (cliente/funcionario).
CREATE INDEX IF NOT EXISTS idx_contratos_cliente ON contratos (cliente_id);
CREATE INDEX IF NOT EXISTS idx_certificados_cliente ON certificados (cliente_id);
CREATE INDEX IF NOT EXISTS idx_cliente_indicacoes_indicador ON cliente_indicacoes (cliente_indicador_id);
