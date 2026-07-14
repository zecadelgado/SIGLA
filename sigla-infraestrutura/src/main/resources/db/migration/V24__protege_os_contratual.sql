-- Bloqueios finais da Fase 3: nenhuma escrita direta pode criar ou mover uma
-- OS para contrato cancelado/vencido, cliente divergente ou fora da vigencia.
CREATE OR REPLACE FUNCTION trg_validar_os_contratual() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
  v_contrato contratos%ROWTYPE;
  v_data_os date;
  v_hoje date := (CURRENT_TIMESTAMP AT TIME ZONE 'America/Sao_Paulo')::date;
BEGIN
  IF TG_OP = 'UPDATE' AND OLD.contrato_id IS NOT NULL AND NEW.contrato_id IS NULL THEN
    DELETE FROM sigla_os_desvinculacao_autorizacoes
     WHERE transacao_id = txid_current() AND ordem_servico_id = OLD.id;
    IF NOT FOUND THEN
      RAISE EXCEPTION 'contrato da OS nao pode ser removido por UPDATE comum' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
  END IF;

  IF NEW.contrato_id IS NULL THEN
    RETURN NEW;
  END IF;

  SELECT * INTO STRICT v_contrato FROM contratos WHERE id = NEW.contrato_id;
  IF upper(COALESCE(v_contrato.status, '')) IN ('CANCELLED', 'CANCELADO') THEN
    RAISE EXCEPTION 'contrato cancelado nao permite OS' USING ERRCODE = '23514';
  END IF;
  IF upper(COALESCE(v_contrato.status, '')) IN ('EXPIRED', 'EXPIRADO')
     OR (v_contrato.data_fim IS NOT NULL AND v_contrato.data_fim < v_hoje) THEN
    RAISE EXCEPTION 'contrato vencido nao permite OS' USING ERRCODE = '23514';
  END IF;
  IF NEW.cliente_id IS DISTINCT FROM v_contrato.cliente_id THEN
    RAISE EXCEPTION 'cliente da OS diverge do cliente do contrato' USING ERRCODE = '23514';
  END IF;
  IF NEW.data_agendada IS NULL THEN
    RAISE EXCEPTION 'data da OS contratual e obrigatoria' USING ERRCODE = '23514';
  END IF;

  v_data_os := (NEW.data_agendada AT TIME ZONE 'America/Sao_Paulo')::date;
  IF v_data_os < v_contrato.data_inicio
     OR (v_contrato.data_fim IS NOT NULL AND v_data_os > v_contrato.data_fim) THEN
    RAISE EXCEPTION 'data da OS fora da vigencia do contrato' USING ERRCODE = '23514';
  END IF;
  RETURN NEW;
END;
$$;

CREATE TABLE sigla_os_desvinculacao_autorizacoes (
  transacao_id bigint NOT NULL,
  ordem_servico_id uuid NOT NULL,
  PRIMARY KEY (transacao_id, ordem_servico_id)
);

REVOKE ALL ON sigla_os_desvinculacao_autorizacoes FROM PUBLIC;
REVOKE ALL ON sigla_os_desvinculacao_autorizacoes FROM anon, authenticated, service_role;

CREATE TRIGGER validar_os_contratual
BEFORE INSERT OR UPDATE OF cliente_id, contrato_id, data_agendada ON ordens_servico
FOR EACH ROW
EXECUTE FUNCTION trg_validar_os_contratual();

CREATE OR REPLACE FUNCTION desvincular_os_contratual_v1(
  p_ordem_servico_id uuid,
  p_motivo text,
  p_usuario_id uuid
) RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
  v_os ordens_servico%ROWTYPE;
  v_usuario usuarios%ROWTYPE;
BEGIN
  IF NULLIF(btrim(p_motivo), '') IS NULL THEN
    RAISE EXCEPTION 'motivo da desvinculacao e obrigatorio' USING ERRCODE = '23514';
  END IF;
  IF p_usuario_id IS NULL THEN
    RAISE EXCEPTION 'usuario administrador da desvinculacao e obrigatorio' USING ERRCODE = '23514';
  END IF;

  -- SECURITY DEFINER: o chamador nao pode forjar autorizacao administrativa
  -- apenas informando um UUID; o usuario precisa existir e ser ADMIN ativo.
  SELECT * INTO v_usuario FROM usuarios WHERE id = p_usuario_id;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'usuario da desvinculacao nao existe' USING ERRCODE = '23514';
  END IF;
  IF upper(COALESCE(v_usuario.tipo, '')) <> 'ADMIN' OR NOT COALESCE(v_usuario.ativo, false) THEN
    RAISE EXCEPTION 'somente ADMIN ativo pode desvincular OS contratual' USING ERRCODE = '23514';
  END IF;

  SELECT * INTO STRICT v_os
    FROM ordens_servico
   WHERE id = p_ordem_servico_id
   FOR UPDATE;

  IF v_os.contrato_id IS NULL THEN
    RAISE EXCEPTION 'OS nao possui contrato para desvincular' USING ERRCODE = '23514';
  END IF;
  IF upper(COALESCE(v_os.status, '')) <> 'AGENDADA'
     OR v_os.data_inicio IS NOT NULL
     OR v_os.data_fim IS NOT NULL
     OR COALESCE(v_os.foi_feito, false) THEN
    RAISE EXCEPTION 'OS iniciada ou concluida nunca pode ser desvinculada' USING ERRCODE = '23514';
  END IF;
  IF v_os.data_agendada IS NULL
     OR v_os.data_agendada <= (CURRENT_TIMESTAMP AT TIME ZONE 'America/Sao_Paulo') THEN
    RAISE EXCEPTION 'somente OS futura pode ser desvinculada' USING ERRCODE = '23514';
  END IF;

  INSERT INTO sigla_os_desvinculacao_autorizacoes(transacao_id, ordem_servico_id)
  VALUES (txid_current(), p_ordem_servico_id);

  UPDATE ordens_servico SET contrato_id = NULL WHERE id = p_ordem_servico_id;

  INSERT INTO auditoria_eventos(entidade_tipo, entidade_id, acao, detalhe, usuario_id)
  VALUES (
    'ordens_servico',
    p_ordem_servico_id::text,
    'OS_CONTRATO_DESVINCULADO_ADMIN',
    'contrato_id=' || v_os.contrato_id::text || '; motivo=' || btrim(p_motivo),
    p_usuario_id
  );

  RETURN p_ordem_servico_id;
END;
$$;

REVOKE ALL ON FUNCTION desvincular_os_contratual_v1(uuid, text, uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION desvincular_os_contratual_v1(uuid, text, uuid) TO authenticated, service_role;
