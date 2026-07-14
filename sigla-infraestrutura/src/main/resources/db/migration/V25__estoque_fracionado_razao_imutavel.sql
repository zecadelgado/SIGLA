-- Fase 4 — estoque fracionado, atomico e reconciliavel.
--
-- A razao (estoque_movimentacoes) passa a ser IMUTAVEL e vira a unica
-- autoridade de saldo: todo INSERT aplica o delta no saldo materializado de
-- produtos via trigger, sob o lock de linha do proprio UPDATE + CHECK de nao
-- negatividade. RPCs e aplicacao nunca mais regravam saldo diretamente.

-- 1) Backfill/compatibilidade de dados legados (idempotente).
UPDATE produtos SET quantidade_atual_decimal = quantidade_atual
 WHERE quantidade_atual_decimal IS NULL AND quantidade_atual IS NOT NULL;
UPDATE produtos SET quantidade_minima_decimal = quantidade_minima
 WHERE quantidade_minima_decimal IS NULL AND quantidade_minima IS NOT NULL;
UPDATE estoque_movimentacoes SET quantidade_decimal = quantidade
 WHERE quantidade_decimal IS NULL;
UPDATE ordem_servico_produtos SET quantidade_solicitada = quantidade
 WHERE quantidade_solicitada IS NULL AND quantidade IS NOT NULL;

-- 2) Escala oficial numeric(19,4) para escritas novas.
ALTER TABLE estoque_movimentacoes ADD CONSTRAINT chk_movimentacao_escala_quantidade
  CHECK (scale(quantidade) <= 4) NOT VALID;
ALTER TABLE ordem_servico_produtos ADD CONSTRAINT chk_os_produto_escala_quantidade
  CHECK (quantidade IS NULL OR scale(quantidade) <= 4) NOT VALID;
ALTER TABLE produtos ADD CONSTRAINT chk_produtos_saldo_decimal_nao_negativo
  CHECK (COALESCE(quantidade_atual_decimal, 0) >= 0) NOT VALID;

-- 3) Classificacao dos movimentos: o delta que cada tipo aplica no saldo.
--    CONSUMO_RESERVA_OS e neutro: converte reserva ja baixada, sem segunda baixa.
CREATE OR REPLACE FUNCTION fn_estoque_delta_ou_nulo_v1(p_tipo text, p_quantidade numeric)
RETURNS numeric LANGUAGE sql IMMUTABLE AS $$
  SELECT CASE upper(btrim(COALESCE(p_tipo, '')))
    WHEN 'ENTRADA' THEN p_quantidade
    WHEN 'INBOUND' THEN p_quantidade
    WHEN 'COMPRA' THEN p_quantidade
    WHEN 'DEVOLUCAO_RESERVA_OS' THEN p_quantidade
    WHEN 'ESTORNO_CONSUMO_OS' THEN p_quantidade
    WHEN 'SAIDA' THEN -p_quantidade
    WHEN 'OUTBOUND' THEN -p_quantidade
    WHEN 'USO_OS' THEN -p_quantidade
    WHEN 'AJUSTE' THEN -p_quantidade
    WHEN 'RESERVA_OS' THEN -p_quantidade
    WHEN 'CONSUMO_RESERVA_OS' THEN 0
    ELSE NULL
  END;
$$;

CREATE OR REPLACE FUNCTION fn_estoque_delta_v1(p_tipo text, p_quantidade numeric)
RETURNS numeric LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE v_delta numeric;
BEGIN
  v_delta := fn_estoque_delta_ou_nulo_v1(p_tipo, p_quantidade);
  IF v_delta IS NULL THEN
    RAISE EXCEPTION 'tipo de movimentacao de estoque desconhecido: %', p_tipo USING ERRCODE = '23514';
  END IF;
  RETURN v_delta;
END;
$$;

-- 4) Baseline: preserva o legado sem inventar movimentos. Para cada produto
--    existente, baseline = saldo materializado atual - soma da razao existente.
--    Invariante permanente: saldo = baseline + soma(razao).
CREATE TABLE estoque_saldos_baseline (
  produto_id uuid PRIMARY KEY REFERENCES produtos(id) ON DELETE CASCADE,
  saldo_baseline numeric(19,4) NOT NULL,
  origem text NOT NULL DEFAULT 'MIGRACAO_V25',
  created_at timestamptz NOT NULL DEFAULT now()
);

INSERT INTO estoque_saldos_baseline (produto_id, saldo_baseline, origem)
SELECT p.id,
       COALESCE(p.quantidade_atual_decimal, p.quantidade_atual, 0)
         - COALESCE((SELECT SUM(COALESCE(fn_estoque_delta_ou_nulo_v1(m.tipo_movimentacao,
                                    COALESCE(m.quantidade_decimal, m.quantidade)), 0))
                       FROM estoque_movimentacoes m
                      WHERE m.produto_id = p.id), 0),
       'MIGRACAO_V25'
  FROM produtos p
ON CONFLICT (produto_id) DO NOTHING;

CREATE OR REPLACE FUNCTION trg_baseline_produto_novo() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
  INSERT INTO estoque_saldos_baseline (produto_id, saldo_baseline, origem)
  VALUES (NEW.id, COALESCE(NEW.quantidade_atual_decimal, NEW.quantidade_atual, 0), 'CADASTRO')
  ON CONFLICT (produto_id) DO NOTHING;
  RETURN NEW;
END;
$$;
CREATE TRIGGER baseline_produto_novo
AFTER INSERT ON produtos
FOR EACH ROW EXECUTE FUNCTION trg_baseline_produto_novo();

-- 5) Saldo materializado mantido exclusivamente pela razao. O UPDATE serializa
--    escritores concorrentes no lock de linha do produto e o CHECK de nao
--    negatividade garante que dois consumidores nao levam o ultimo saldo.
CREATE OR REPLACE FUNCTION trg_aplicar_movimento_estoque() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE v_delta numeric;
BEGIN
  v_delta := fn_estoque_delta_v1(NEW.tipo_movimentacao, COALESCE(NEW.quantidade_decimal, NEW.quantidade));
  IF v_delta <> 0 THEN
    UPDATE produtos
       SET quantidade_atual_decimal = COALESCE(quantidade_atual_decimal, quantidade_atual, 0) + v_delta,
           quantidade_atual = COALESCE(quantidade_atual_decimal, quantidade_atual, 0) + v_delta,
           updated_at = now()
     WHERE id = NEW.produto_id;
    IF NOT FOUND THEN
      RAISE EXCEPTION 'produto % inexistente para movimento de estoque', NEW.produto_id USING ERRCODE = '23514';
    END IF;
  END IF;
  RETURN NEW;
END;
$$;
CREATE TRIGGER aplicar_movimento_estoque
AFTER INSERT ON estoque_movimentacoes
FOR EACH ROW EXECUTE FUNCTION trg_aplicar_movimento_estoque();

-- 6) Razao imutavel: correcao e sempre por movimento compensatorio novo.
CREATE OR REPLACE FUNCTION trg_bloquear_alteracao_razao_estoque() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
  RAISE EXCEPTION 'razao de estoque e imutavel; registre movimento compensatorio' USING ERRCODE = '23514';
END;
$$;
CREATE TRIGGER bloquear_alteracao_razao_estoque
BEFORE UPDATE OR DELETE ON estoque_movimentacoes
FOR EACH ROW EXECUTE FUNCTION trg_bloquear_alteracao_razao_estoque();

-- 7) RPCs de OS deixam de regravar saldo (o trigger da razao aplica o delta).
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
  PERFORM 1 FROM produtos WHERE id = p_produto_id FOR UPDATE;
  INSERT INTO estoque_movimentacoes(produto_id,tipo_movimentacao,quantidade,
    quantidade_decimal,ordem_servico_id,chave_idempotencia,observacoes)
  VALUES(p_produto_id,'DEVOLUCAO_RESERVA_OS',p_quantidade,p_quantidade,p_os_id,p_chave,
    'Liberacao idempotente por cancelamento');
  UPDATE ordem_servico_produtos SET quantidade_reservada=0,estado_estoque='DEVOLVIDA',versao=versao+1
    WHERE ordem_servico_id=p_os_id AND produto_id=p_produto_id;
  RETURN 'LIBERADA';
END;
$$;

-- 8) Anulacao de OS concluida: compensacao de estoque auditavel e idempotente.
--    Efeitos financeiros da anulacao pertencem a Fase 5 (compensacoes).
CREATE OR REPLACE FUNCTION anular_os_concluida_v1(
  p_os_id uuid, p_motivo text, p_usuario_id uuid, p_chave varchar
) RETURNS uuid LANGUAGE plpgsql AS $$
DECLARE
  v_os ordens_servico%ROWTYPE;
  v_usuario usuarios%ROWTYPE;
  v_anulacao uuid;
  v_historico uuid;
  r record;
BEGIN
  IF NULLIF(btrim(p_motivo), '') IS NULL THEN
    RAISE EXCEPTION 'motivo da anulacao e obrigatorio' USING ERRCODE = '23514';
  END IF;
  IF NULLIF(btrim(COALESCE(p_chave, '')), '') IS NULL THEN
    RAISE EXCEPTION 'chave de idempotencia da anulacao e obrigatoria' USING ERRCODE = '23514';
  END IF;
  IF p_usuario_id IS NULL THEN
    RAISE EXCEPTION 'usuario administrador da anulacao e obrigatorio' USING ERRCODE = '23514';
  END IF;
  SELECT * INTO v_usuario FROM usuarios WHERE id = p_usuario_id;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'usuario da anulacao nao existe' USING ERRCODE = '23514';
  END IF;
  IF upper(COALESCE(v_usuario.tipo, '')) <> 'ADMIN' OR NOT COALESCE(v_usuario.ativo, false) THEN
    RAISE EXCEPTION 'somente ADMIN ativo pode anular OS concluida' USING ERRCODE = '23514';
  END IF;

  SELECT * INTO STRICT v_os FROM ordens_servico WHERE id = p_os_id FOR UPDATE;
  IF upper(COALESCE(v_os.status, '')) NOT IN ('CONCLUIDA','COMPLETED') THEN
    RAISE EXCEPTION 'anulacao aplica-se somente a OS concluida' USING ERRCODE = '23514';
  END IF;

  SELECT id INTO v_anulacao FROM os_anulacoes WHERE chave_idempotencia = p_chave;
  IF FOUND THEN
    RETURN v_anulacao;
  END IF;
  IF EXISTS (SELECT 1 FROM os_anulacoes WHERE ordem_servico_id = p_os_id) THEN
    RAISE EXCEPTION 'OS ja possui anulacao registrada' USING ERRCODE = '23514';
  END IF;

  INSERT INTO historico_status(entidade_tipo, entidade_id, status_anterior, status_novo,
    evento_tipo, motivo, usuario_id, chave_idempotencia)
  VALUES ('ordens_servico', p_os_id, v_os.status, v_os.status, 'ANULACAO',
    btrim(p_motivo), p_usuario_id, 'ANULACAO:' || p_chave)
  RETURNING id INTO v_historico;

  INSERT INTO os_anulacoes(ordem_servico_id, motivo, historico_id, chave_idempotencia, anulado_por)
  VALUES (p_os_id, btrim(p_motivo), v_historico, p_chave, p_usuario_id)
  RETURNING id INTO v_anulacao;

  FOR r IN (
    SELECT m.produto_id,
           SUM(CASE WHEN upper(m.tipo_movimentacao) IN ('CONSUMO_RESERVA_OS','USO_OS')
                      THEN COALESCE(m.quantidade_decimal, m.quantidade)
                    WHEN upper(m.tipo_movimentacao) = 'ESTORNO_CONSUMO_OS'
                      THEN -COALESCE(m.quantidade_decimal, m.quantidade)
                    ELSE 0 END) AS consumido,
           (array_agg(m.id ORDER BY m.created_at)
              FILTER (WHERE upper(m.tipo_movimentacao) IN ('CONSUMO_RESERVA_OS','USO_OS')))[1] AS movimento_original
      FROM estoque_movimentacoes m
     WHERE m.ordem_servico_id = p_os_id
     GROUP BY m.produto_id
  ) LOOP
    IF r.consumido > 0 THEN
      PERFORM 1 FROM produtos WHERE id = r.produto_id FOR UPDATE;
      INSERT INTO estoque_movimentacoes(produto_id, tipo_movimentacao, quantidade,
        quantidade_decimal, ordem_servico_id, chave_idempotencia, movimento_compensado_id, observacoes)
      VALUES (r.produto_id, 'ESTORNO_CONSUMO_OS', r.consumido, r.consumido, p_os_id,
        'ANULACAO:' || p_chave || ':' || r.produto_id, r.movimento_original,
        'Compensacao de consumo por anulacao de OS concluida; motivo: ' || btrim(p_motivo));
    END IF;
  END LOOP;

  UPDATE ordem_servico_produtos SET estado_estoque='COMPENSADA', versao=versao+1
   WHERE ordem_servico_id = p_os_id AND estado_estoque = 'CONSUMIDA';

  INSERT INTO auditoria_eventos(entidade_tipo, entidade_id, acao, detalhe, usuario_id)
  VALUES ('ordens_servico', p_os_id::text, 'OS_CONCLUIDA_ANULADA',
    'motivo=' || btrim(p_motivo) || '; chave=' || p_chave, p_usuario_id);

  RETURN v_anulacao;
END;
$$;

-- 9) Reconciliacao do saldo materializado contra a razao (+ baseline legado).
CREATE OR REPLACE VIEW vw_estoque_reconciliacao_v1 AS
SELECT p.id AS produto_id,
       p.nome,
       COALESCE(p.quantidade_atual_decimal, p.quantidade_atual, 0) AS saldo_materializado,
       COALESCE(b.saldo_baseline, 0) + COALESCE(l.saldo_razao, 0) AS saldo_razao,
       COALESCE(p.quantidade_atual_decimal, p.quantidade_atual, 0)
         - (COALESCE(b.saldo_baseline, 0) + COALESCE(l.saldo_razao, 0)) AS divergencia,
       COALESCE(l.movimentos_tipo_desconhecido, 0) AS movimentos_tipo_desconhecido
  FROM produtos p
  LEFT JOIN estoque_saldos_baseline b ON b.produto_id = p.id
  LEFT JOIN (
    SELECT m.produto_id,
           SUM(COALESCE(fn_estoque_delta_ou_nulo_v1(m.tipo_movimentacao,
                 COALESCE(m.quantidade_decimal, m.quantidade)), 0)) AS saldo_razao,
           COUNT(*) FILTER (WHERE fn_estoque_delta_ou_nulo_v1(m.tipo_movimentacao,
                 COALESCE(m.quantidade_decimal, m.quantidade)) IS NULL) AS movimentos_tipo_desconhecido
      FROM estoque_movimentacoes m
     GROUP BY m.produto_id
  ) l ON l.produto_id = p.id;

CREATE OR REPLACE FUNCTION reconciliar_estoque_v1()
RETURNS SETOF vw_estoque_reconciliacao_v1 LANGUAGE sql STABLE AS $$
  SELECT * FROM vw_estoque_reconciliacao_v1
   WHERE divergencia <> 0 OR movimentos_tipo_desconhecido > 0;
$$;
