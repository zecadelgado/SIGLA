-- Feature flags de cutover. Financeiro completo, RLS e faturamento mensal por
-- RPC permanecem fora desta fase.
CREATE TABLE sigla_feature_flags (
  chave varchar(120) PRIMARY KEY,
  habilitada boolean NOT NULL,
  descricao text,
  updated_at timestamptz NOT NULL DEFAULT now()
);
INSERT INTO sigla_feature_flags(chave, habilitada, descricao) VALUES
  ('os_fonte_operacional', true, 'Agenda delega comandos operacionais para a OS.'),
  ('agenda_sync_bidirecional_legado', false, 'Sincronizacao bidirecional desativada na Fase 3.'),
  ('materializar_ocorrencias_contratuais', true, 'Uma OS por ocorrencia contratual futura.')
ON CONFLICT (chave) DO UPDATE SET habilitada = EXCLUDED.habilitada,
  descricao = EXCLUDED.descricao, updated_at = now();

CREATE OR REPLACE FUNCTION trg_exigir_flag_ocorrencia_contratual() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
  IF NEW.origem_tipo='CONTRATO'
     AND NOT COALESCE((SELECT habilitada FROM sigla_feature_flags
                       WHERE chave='materializar_ocorrencias_contratuais'), false) THEN
    RAISE EXCEPTION 'materializacao de ocorrencias contratuais desabilitada pela feature flag'
      USING ERRCODE='23514';
  END IF;
  RETURN NEW;
END;
$$;
CREATE TRIGGER exigir_flag_ocorrencia_contratual
BEFORE INSERT ON ocorrencias_operacionais
FOR EACH ROW EXECUTE FUNCTION trg_exigir_flag_ocorrencia_contratual();

-- Impede que qualquer cliente legado transforme uma OS concluida em cancelada.
CREATE OR REPLACE FUNCTION trg_bloquear_cancelamento_os_concluida() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
  IF upper(OLD.status) IN ('CONCLUIDA','COMPLETED')
     AND upper(NEW.status) IN ('CANCELADA','CANCELLED') THEN
    RAISE EXCEPTION 'OS concluida nao pode ser cancelada; registre anulacao auditavel'
      USING ERRCODE = '23514';
  END IF;
  RETURN NEW;
END;
$$;
CREATE TRIGGER bloquear_cancelamento_os_concluida
BEFORE UPDATE OF status ON ordens_servico
FOR EACH ROW EXECUTE FUNCTION trg_bloquear_cancelamento_os_concluida();

-- Operacoes atomicas e idempotentes de estoque. A reserva reduz o disponivel;
-- o consumo apenas converte a reserva e nao baixa o produto uma segunda vez.
CREATE OR REPLACE FUNCTION reservar_estoque_os_v1(
  p_os_id uuid, p_produto_id uuid, p_quantidade numeric, p_chave varchar
) RETURNS text LANGUAGE plpgsql AS $$
DECLARE v_disponivel numeric; v_estado text;
BEGIN
  IF p_quantidade IS NULL OR p_quantidade <= 0 OR scale(p_quantidade) > 4 THEN
    RAISE EXCEPTION 'quantidade de reserva invalida' USING ERRCODE = '22023';
  END IF;
  SELECT tipo_movimentacao INTO v_estado FROM estoque_movimentacoes
    WHERE chave_idempotencia = p_chave;
  IF FOUND THEN RETURN 'JA_RESERVADA'; END IF;
  PERFORM 1 FROM ordens_servico WHERE id = p_os_id
    AND upper(status) NOT IN ('CONCLUIDA','COMPLETED','CANCELADA','CANCELLED') FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'OS nao permite reserva' USING ERRCODE = '23514'; END IF;
  SELECT COALESCE(quantidade_atual_decimal, quantidade_atual) INTO v_disponivel
    FROM produtos WHERE id = p_produto_id FOR UPDATE;
  IF v_disponivel IS NULL OR v_disponivel < p_quantidade THEN
    RAISE EXCEPTION 'saldo insuficiente' USING ERRCODE = 'P0001';
  END IF;
  UPDATE produtos SET quantidade_atual_decimal = v_disponivel - p_quantidade,
    quantidade_atual = v_disponivel - p_quantidade, updated_at = now()
    WHERE id = p_produto_id;
  INSERT INTO estoque_movimentacoes(produto_id,tipo_movimentacao,quantidade,
    quantidade_decimal,ordem_servico_id,chave_idempotencia,observacoes)
  VALUES(p_produto_id,'RESERVA_OS',p_quantidade,p_quantidade,p_os_id,p_chave,
    'Reserva atomica ao iniciar OS');
  UPDATE ordem_servico_produtos SET quantidade_reservada = COALESCE(quantidade_solicitada, quantidade),
    estado_estoque = 'RESERVADA', chave_idempotencia = COALESCE(chave_idempotencia,p_chave), versao = versao + 1
    WHERE ordem_servico_id = p_os_id AND produto_id = p_produto_id;
  RETURN 'RESERVADA';
END;
$$;

CREATE OR REPLACE FUNCTION consumir_reserva_os_v1(
  p_os_id uuid, p_produto_id uuid, p_quantidade numeric, p_chave varchar
) RETURNS text LANGUAGE plpgsql AS $$
BEGIN
  IF EXISTS (SELECT 1 FROM estoque_movimentacoes WHERE chave_idempotencia = p_chave) THEN
    RETURN 'JA_CONSUMIDA';
  END IF;
  PERFORM 1 FROM ordem_servico_produtos WHERE ordem_servico_id=p_os_id
    AND produto_id=p_produto_id AND estado_estoque='RESERVADA' FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'reserva inexistente' USING ERRCODE='23514'; END IF;
  INSERT INTO estoque_movimentacoes(produto_id,tipo_movimentacao,quantidade,
    quantidade_decimal,ordem_servico_id,chave_idempotencia,observacoes)
  VALUES(p_produto_id,'CONSUMO_RESERVA_OS',p_quantidade,p_quantidade,p_os_id,p_chave,
    'Conversao da reserva; sem segunda baixa');
  UPDATE ordem_servico_produtos SET quantidade_consumida=p_quantidade,
    estado_estoque='CONSUMIDA',versao=versao+1
    WHERE ordem_servico_id=p_os_id AND produto_id=p_produto_id;
  RETURN 'CONSUMIDA';
END;
$$;

CREATE OR REPLACE FUNCTION liberar_reserva_os_v1(
  p_os_id uuid, p_produto_id uuid, p_quantidade numeric, p_chave varchar
) RETURNS text LANGUAGE plpgsql AS $$
BEGIN
  IF EXISTS (SELECT 1 FROM estoque_movimentacoes WHERE chave_idempotencia=p_chave) THEN
    RETURN 'JA_LIBERADA';
  END IF;
  PERFORM 1 FROM ordem_servico_produtos WHERE ordem_servico_id=p_os_id
    AND produto_id=p_produto_id AND estado_estoque='RESERVADA' FOR UPDATE;
  IF NOT FOUND THEN RETURN 'SEM_RESERVA'; END IF;
  UPDATE produtos SET quantidade_atual_decimal=COALESCE(quantidade_atual_decimal,quantidade_atual)+p_quantidade,
    quantidade_atual=COALESCE(quantidade_atual_decimal,quantidade_atual)+p_quantidade,updated_at=now()
    WHERE id=p_produto_id;
  INSERT INTO estoque_movimentacoes(produto_id,tipo_movimentacao,quantidade,
    quantidade_decimal,ordem_servico_id,chave_idempotencia,observacoes)
  VALUES(p_produto_id,'DEVOLUCAO_RESERVA_OS',p_quantidade,p_quantidade,p_os_id,p_chave,
    'Liberacao idempotente por cancelamento');
  UPDATE ordem_servico_produtos SET quantidade_reservada=0,estado_estoque='DEVOLVIDA',versao=versao+1
    WHERE ordem_servico_id=p_os_id AND produto_id=p_produto_id;
  RETURN 'LIBERADA';
END;
$$;

-- Base da anulacao: registra pedido auditavel, sem cancelar a OS, estornar
-- estoque ou executar efeitos financeiros nesta fase.
CREATE OR REPLACE FUNCTION registrar_base_anulacao_os_v1(
  p_os_id uuid, p_motivo text, p_usuario_id uuid, p_chave varchar
) RETURNS uuid LANGUAGE plpgsql AS $$
DECLARE v_id uuid;
BEGIN
  IF btrim(COALESCE(p_motivo,''))='' THEN RAISE EXCEPTION 'motivo obrigatorio'; END IF;
  PERFORM 1 FROM ordens_servico WHERE id=p_os_id AND upper(status) IN ('CONCLUIDA','COMPLETED');
  IF NOT FOUND THEN RAISE EXCEPTION 'anulacao aplica-se somente a OS concluida' USING ERRCODE='23514'; END IF;
  INSERT INTO os_anulacoes(ordem_servico_id,motivo,chave_idempotencia,anulado_por)
  VALUES(p_os_id,p_motivo,p_chave,p_usuario_id)
  ON CONFLICT (chave_idempotencia) DO UPDATE SET motivo=os_anulacoes.motivo
  RETURNING id INTO v_id;
  RETURN v_id;
END;
$$;

-- Toda OS contratual futura recebe uma ocorrencia propria. As chaves derivadas
-- da OS tornam retries idempotentes; o vinculo com agenda e completado quando
-- a projecao for gravada pelo produtor da OS.
CREATE OR REPLACE FUNCTION trg_materializar_ocorrencia_os_contratual() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE v_ocorrencia uuid; v_vigencia uuid;
BEGIN
  IF NOT COALESCE((SELECT habilitada FROM sigla_feature_flags
                   WHERE chave='materializar_ocorrencias_contratuais'), false) THEN
    RETURN NEW;
  END IF;
  IF NEW.contrato_id IS NULL OR NEW.data_agendada IS NULL THEN RETURN NEW; END IF;
  SELECT id INTO v_vigencia FROM contrato_vigencias
    WHERE contrato_id=NEW.contrato_id
      AND (NEW.data_agendada AT TIME ZONE 'America/Sao_Paulo')::date >= data_inicio
      AND (data_fim IS NULL OR (NEW.data_agendada AT TIME ZONE 'America/Sao_Paulo')::date <= data_fim)
    ORDER BY versao DESC LIMIT 1;
  INSERT INTO ocorrencias_operacionais(contrato_vigencia_id,ordem_servico_id,
    inicio_previsto,fim_previsto,timezone,origem_tipo,chave_idempotencia)
  VALUES(v_vigencia,NEW.id,NEW.data_agendada,NEW.data_agendada+interval '1 hour',
    'America/Sao_Paulo','CONTRATO','OCORRENCIA:OS:'||NEW.id)
  ON CONFLICT (ordem_servico_id) DO UPDATE SET inicio_previsto=EXCLUDED.inicio_previsto,
    fim_previsto=EXCLUDED.fim_previsto, contrato_vigencia_id=EXCLUDED.contrato_vigencia_id
  RETURNING id INTO v_ocorrencia;
  UPDATE ordens_servico SET ocorrencia_id=v_ocorrencia WHERE id=NEW.id AND ocorrencia_id IS DISTINCT FROM v_ocorrencia;
  RETURN NEW;
END;
$$;
CREATE TRIGGER materializar_ocorrencia_os_contratual
AFTER INSERT OR UPDATE OF data_agendada, contrato_id ON ordens_servico
FOR EACH ROW WHEN (NEW.contrato_id IS NOT NULL)
EXECUTE FUNCTION trg_materializar_ocorrencia_os_contratual();

CREATE OR REPLACE FUNCTION trg_vincular_agenda_a_ocorrencia_os() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE v_ocorrencia uuid;
BEGIN
  IF NEW.ordem_servico_id IS NULL THEN RETURN NEW; END IF;
  SELECT id INTO v_ocorrencia FROM ocorrencias_operacionais WHERE ordem_servico_id=NEW.ordem_servico_id;
  IF v_ocorrencia IS NULL THEN RETURN NEW; END IF;
  UPDATE agenda_eventos SET ocorrencia_id=v_ocorrencia, origem_tipo='OPERACIONAL',
    timezone='America/Sao_Paulo', recorrencia='nenhuma'
    WHERE id=NEW.id AND (ocorrencia_id IS DISTINCT FROM v_ocorrencia OR origem_tipo IS DISTINCT FROM 'OPERACIONAL'
      OR timezone IS DISTINCT FROM 'America/Sao_Paulo' OR recorrencia IS DISTINCT FROM 'nenhuma');
  UPDATE ocorrencias_operacionais SET agenda_evento_id=NEW.id,
    inicio_previsto=NEW.data_inicio, fim_previsto=NEW.data_fim
    WHERE id=v_ocorrencia;
  RETURN NEW;
END;
$$;
CREATE TRIGGER vincular_agenda_a_ocorrencia_os
AFTER INSERT OR UPDATE OF ordem_servico_id, data_inicio, data_fim ON agenda_eventos
FOR EACH ROW WHEN (NEW.ordem_servico_id IS NOT NULL)
EXECUTE FUNCTION trg_vincular_agenda_a_ocorrencia_os();

CREATE OR REPLACE VIEW vw_ordens_servico_operacionais_v1 AS
SELECT o.*,
  CASE WHEN upper(o.status) IN ('AGENDADA','SCHEDULED','ABERTA','OPEN')
         AND o.data_agendada < (now() AT TIME ZONE 'America/Sao_Paulo')
       THEN 'ATRASADA' ELSE o.status END AS status_efetivo
FROM ordens_servico o;
