// Edge Function: notificacoes-agendador
// -----------------------------------------------------------------------------
// Runtime externo "sempre-ligado" do SIGLA. Roda o ciclo de notificacoes mesmo com
// nenhum desktop aberto: gera as notificacoes devidas (contrato/certificado/visita,
// com escalonamento 30/15/7/1) e dispara as que chegaram ao horario, via webhook n8n.
//
// Espelha a logica Java de referencia:
//   - CasoDeUsoGerarNotificacoes (geracao + reconciliacao + dedup por dia)
//   - RenderizadorTemplate ({{var}}), TelefoneWhatsapp (normalizacao/validacao)
//   - AdaptadorEnvioWhatsappN8n.toJson (mesmo payload p/ o n8n)
//   - DiasLembrete (conjunto de dias)
//
// ESCOPO: contrato, certificado e visita (serviços agendados) + dispatch.
// FATURAMENTO (Fase 6): esta função apenas DISPARA a autoridade única
// rpc_faturar_mensalidades_v1 (origem EDGE_FUNCTION, chave idempotente); não
// replica a regra em SQL/TS. Em SIGLA_AGENDADOR_DRY_RUN=true nada é escrito.
// Parcela em atraso (INSTALLMENT_OVERDUE) segue no desktop por ora.
//
// SEGURANCA: aplicar e validar SEMPRE no projeto Supabase de HOMOLOGACAO antes de
// habilitar o pg_cron em producao. Segredos vem de variaveis de ambiente (Function
// Secrets), nunca de tabela. Ver README.md.
// -----------------------------------------------------------------------------

import postgres from "https://deno.land/x/postgresjs@v3.4.5/mod.js";

const DB_URL = Deno.env.get("SUPABASE_DB_URL") ?? Deno.env.get("SIGLA_DB_URL") ?? "";
const WEBHOOK_URL = Deno.env.get("SIGLA_NOTIFICACOES_WEBHOOK_URL") ?? "";
const WEBHOOK_TOKEN = Deno.env.get("SIGLA_NOTIFICACOES_WEBHOOK_TOKEN") ?? "";
const ENVIO_HABILITADO = (Deno.env.get("SIGLA_NOTIFICACOES_WHATSAPP_ENABLED") ?? "false") === "true";
const MODO_TESTE = (Deno.env.get("SIGLA_NOTIFICACOES_MODO_TESTE") ?? "true") === "true";
const HORA_ENVIO = parseInt(Deno.env.get("SIGLA_NOTIFICACOES_HORA_ENVIO") ?? "8", 10);
const TIMEOUT_MS = parseInt(Deno.env.get("SIGLA_NOTIFICACOES_TIMEOUT_MS") ?? "15000", 10);
const MAX_TENTATIVAS = parseInt(Deno.env.get("SIGLA_NOTIFICACOES_MAX_TENTATIVAS") ?? "5", 10);
// Fase 6: modo seco para homologacao — nenhuma escrita e feita.
const DRY_RUN = (Deno.env.get("SIGLA_AGENDADOR_DRY_RUN") ?? "false") === "true";

const STATUS_ATIVOS = ["PENDING", "SENT", "OPEN"];

// ----------------------------- helpers de dominio ----------------------------

function normalizarTelefone(tel: string | null): string {
  if (!tel) return "";
  let d = tel.replace(/\D/g, "").replace(/^0+/, "");
  if (!d) return "";
  const ddi = "55";
  if (d.length === 10 || d.length === 11) return ddi + d;
  if (d.startsWith(ddi)) return d;
  return ddi + d;
}

function telefoneValido(tel: string | null): boolean {
  const n = normalizarTelefone(tel);
  return n.length >= 12 && n.length <= 15;
}

function renderizar(template: string | null, vars: Record<string, string>): string {
  if (!template) return "";
  return template.replace(/\{\{\s*([a-zA-Z0-9_]+)\s*\}\}/g, (_, k) => vars[k] ?? "");
}

function formatData(d: Date | string | null): string {
  if (!d) return "";
  const dt = typeof d === "string" ? new Date(d) : d;
  const dia = String(dt.getUTCDate()).padStart(2, "0");
  const mes = String(dt.getUTCMonth() + 1).padStart(2, "0");
  return `${dia}/${mes}/${dt.getUTCFullYear()}`;
}

function isoDate(d: Date): string {
  return d.toISOString().slice(0, 10);
}

function parseConjunto(csv: string | null): number[] | null {
  if (csv === null || csv === undefined) return null;
  if (csv.trim() === "") return [];
  return [...new Set(csv.split(",").map((s) => parseInt(s.trim(), 10)).filter((n) => Number.isFinite(n) && n > 0))]
    .sort((a, b) => b - a);
}

/** Antecedencias efetivas: conjunto se configurado, senao fallback do int legado quando ativo. */
function diasEfetivos(conjunto: number[] | null, ativo: boolean, intLegado: number | null): number[] {
  if (conjunto !== null) return conjunto;
  return ativo && intLegado && intLegado > 0 ? [intLegado] : [];
}

function subtraiDias(base: Date, dias: number): Date {
  const d = new Date(base);
  d.setUTCDate(d.getUTCDate() - dias);
  return d;
}

function ladosDe(destinatario: string): ("CLIENTE" | "FUNCIONARIO")[] {
  if (destinatario === "AMBOS") return ["CLIENTE", "FUNCIONARIO"];
  if (destinatario === "FUNCIONARIO") return ["FUNCIONARIO"];
  return ["CLIENTE"];
}

// --------------------------------- tipos -------------------------------------

interface Config {
  id: string;
  event_type: string;
  titulo: string;
  template_mensagem: string;
  destinatario: string;
  origem_tipo: string;
  canal: string;
  fonte_telefone: string;
  telefone_informado: string | null;
  dias_antecedencia: number | null;
  criado_por: string | null;
}

interface Pessoa {
  id: string;
  nome: string;
  telefone: string;
  email: string;
  cidade: string;
}

interface Plano {
  cfg: Config;
  lado: "CLIENTE" | "FUNCIONARIO";
  triggerDate: string; // yyyy-mm-dd
}

// --------------------------------- runner ------------------------------------

Deno.serve(async (req) => {
  if (!DB_URL) {
    return json({ erro: "SUPABASE_DB_URL nao configurada" }, 500);
  }
  const sql = postgres(DB_URL, { prepare: false });
  const hoje = new Date(isoDate(new Date()) + "T00:00:00.000Z");
  try {
    const configsPorEvento = await carregarConfigs(sql);
    const pessoas = await carregarPessoas(sql);

    let mensalidadesGeradas = 0;
    try {
      mensalidadesGeradas = await faturarMensalidadesContrato(sql);
    } catch (e) {
      // Faturamento tem autoridade propria (RPC); falha nele nao pode
      // derrubar o ciclo de notificacoes.
      console.error("Faturamento falhou; ciclo de notificacoes continua", e);
    }
    if (DRY_RUN) {
      return json({ ok: true, dryRun: true, hoje: isoDate(hoje), mensalidadesGeradas: 0, geradas: 0, enviadas: 0, modoTeste: MODO_TESTE, envioHabilitado: ENVIO_HABILITADO });
    }
    let geradas = 0;
    geradas += await processarContratos(sql, hoje, configsPorEvento.get("CONTRACT_EXPIRING") ?? [], pessoas);
    geradas += await processarCertificados(sql, hoje, configsPorEvento.get("CERTIFICATE_EXPIRING") ?? [], pessoas);
    geradas += await processarVisitas(sql, hoje, configsPorEvento.get("VISIT_UPCOMING") ?? [], pessoas);

    const enviadas = await dispatchDue(sql);
    await registrarExecucao(sql, isoDate(hoje));

    return json({ ok: true, hoje: isoDate(hoje), mensalidadesGeradas, geradas, enviadas, modoTeste: MODO_TESTE, envioHabilitado: ENVIO_HABILITADO });
  } catch (e) {
    console.error("Falha no ciclo de notificacoes", e);
    return json({ erro: String(e) }, 500);
  } finally {
    await sql.end({ timeout: 5 });
  }
});

// ------------------------------- carregamento --------------------------------

async function carregarConfigs(sql: postgres.Sql): Promise<Map<string, Config[]>> {
  const rows = await sql<Config[]>`
    select id, event_type, titulo, template_mensagem, destinatario, origem_tipo, canal,
           fonte_telefone, telefone_informado, dias_antecedencia, criado_por
      from notificacao_configuracoes
     where ativo = true and automatico = true`;
  const mapa = new Map<string, Config[]>();
  for (const c of rows) {
    const lista = mapa.get(c.event_type) ?? [];
    lista.push(c);
    mapa.set(c.event_type, lista);
  }
  return mapa;
}

async function carregarPessoas(sql: postgres.Sql): Promise<Map<string, Pessoa>> {
  const rows = await sql`
    select id::text as id, coalesce(nome, '') as nome, coalesce(telefone_principal, '') as telefone,
           coalesce(email, '') as email, coalesce(cidade, '') as cidade
      from cadastro`;
  const mapa = new Map<string, Pessoa>();
  for (const p of rows) mapa.set(p.id, p as Pessoa);
  return mapa;
}

/**
 * Fase 6: a Edge Function nao replica regra de negocio em SQL/TS — ela apenas
 * dispara a autoridade unica rpc_faturar_mensalidades_v1 com chave idempotente
 * derivada da data de referencia em America/Sao_Paulo e origem EDGE_FUNCTION.
 * Em modo seco (SIGLA_AGENDADOR_DRY_RUN=true) nada e escrito: apenas loga
 * quantas mensalidades seriam criadas.
 */
async function faturarMensalidadesContrato(sql: postgres.Sql): Promise<number> {
  const dataReferencia = new Date().toLocaleDateString("en-CA", { timeZone: "America/Sao_Paulo" });
  const chave = `FATURAMENTO:EDGE:${dataReferencia}`;
  const inicio = Date.now();
  if (DRY_RUN) {
    const previa = await sql<{ pendentes: string }[]>`
      select count(*)::text as pendentes
        from contratos c
        join contrato_vigencias v on v.contrato_id = c.id
       where upper(coalesce(c.status, '')) in ('ACTIVE', 'ATIVO')
         and ${dataReferencia}::date >= v.data_inicio
         and (v.data_fim is null or ${dataReferencia}::date <= v.data_fim)
         and coalesce(v.valor_mensal, 0) > 0
         and not exists (
           select 1 from financeiro_lancamentos l
            where l.chave_idempotencia = 'MENSALIDADE:' || c.id || ':'
              || to_char(date_trunc('month', ${dataReferencia}::date), 'YYYY-MM'))`;
    console.log(JSON.stringify({
      etapa: "faturamento", dryRun: true, chave, dataReferencia,
      criariam: Number(previa[0]?.pendentes ?? 0), duracaoMs: Date.now() - inicio,
    }));
    return 0;
  }
  try {
    const resultados = await sql<{ resultado: string }[]>`
      select resultado from rpc_faturar_mensalidades_v1(${dataReferencia}::date, ${chave}, 'EDGE_FUNCTION', null)`;
    const criadas = resultados.filter((r) => r.resultado === "CRIADA").length;
    console.log(JSON.stringify({
      etapa: "faturamento", dryRun: false, chave, dataReferencia,
      criadas, existentes: resultados.length - criadas, duracaoMs: Date.now() - inicio,
    }));
    return criadas;
  } catch (e) {
    console.error(JSON.stringify({
      etapa: "faturamento", chave, dataReferencia, erro: String(e), duracaoMs: Date.now() - inicio,
    }));
    throw e;
  }
}

// ------------------------------- processadores -------------------------------

async function processarContratos(sql: postgres.Sql, hoje: Date, configs: Config[], pessoas: Map<string, Pessoa>): Promise<number> {
  if (configs.length === 0) return 0;
  const rows = await sql`
    select id::text as id, cliente_id::text as cliente_id, coalesce(descricao, '') as descricao,
           data_fim, coalesce(alerta_ativo, true) as alerta_ativo, coalesce(dias_alerta_fim, 0) as dias_alerta_fim,
           alerta_dias_conjunto, coalesce(status, 'ACTIVE') as status, coalesce(tipo_contrato, '') as tipo
      from contratos where data_fim is not null`;
  let geradas = 0;
  for (const c of rows) {
    const cliente = pessoas.get(c.cliente_id);
    const fim = new Date(c.data_fim + "T00:00:00.000Z");
    const elegivel = c.status !== "CANCELLED" && c.status !== "EXPIRED" && fim >= hoje;
    const planos: Plano[] = [];
    if (elegivel) {
      const dias = diasEfetivos(parseConjunto(c.alerta_dias_conjunto), c.alerta_ativo, c.dias_alerta_fim);
      for (const cfg of configs) {
        for (const d of dias) {
          const trigger = isoDate(subtraiDias(fim, d));
          for (const lado of ladosDe(cfg.destinatario)) planos.push({ cfg, lado, triggerDate: trigger });
        }
      }
    }
    const vars = {
      cliente_nome: cliente?.nome ?? "",
      cliente_telefone: cliente?.telefone ?? "",
      cliente_email: cliente?.email ?? "",
      endereco: cliente?.cidade ?? "",
      contrato_vencimento: formatData(c.data_fim),
      tipo_servico: c.descricao || c.tipo,
    };
    geradas += await reconciliarEGerar(sql, c.id, "CONTRACT_EXPIRING", planos, hoje, cliente, null, vars);
  }
  return geradas;
}

async function processarCertificados(sql: postgres.Sql, hoje: Date, configs: Config[], pessoas: Map<string, Pessoa>): Promise<number> {
  if (configs.length === 0) return 0;
  const rows = await sql`
    select id::text as id, cliente_id::text as cliente_id, coalesce(descricao, '') as descricao,
           data_validade, coalesce(alerta_ativo, true) as alerta_ativo, coalesce(dias_alerta, 0) as dias_alerta,
           alerta_dias_conjunto, coalesce(status, 'ACTIVE') as status
      from certificados where data_validade is not null`;
  let geradas = 0;
  for (const c of rows) {
    const cliente = pessoas.get(c.cliente_id);
    const validade = new Date(c.data_validade + "T00:00:00.000Z");
    const elegivel = c.status !== "REPLACED" && c.status !== "EXPIRED" && validade >= hoje;
    const planos: Plano[] = [];
    if (elegivel) {
      const dias = diasEfetivos(parseConjunto(c.alerta_dias_conjunto), c.alerta_ativo, c.dias_alerta);
      for (const cfg of configs) {
        for (const d of dias) {
          const trigger = isoDate(subtraiDias(validade, d));
          for (const lado of ladosDe(cfg.destinatario)) planos.push({ cfg, lado, triggerDate: trigger });
        }
      }
    }
    const vars = {
      cliente_nome: cliente?.nome ?? "",
      cliente_telefone: cliente?.telefone ?? "",
      cliente_email: cliente?.email ?? "",
      endereco: cliente?.cidade ?? "",
      certificado_vencimento: formatData(c.data_validade),
      tipo_servico: c.descricao,
    };
    geradas += await reconciliarEGerar(sql, c.id, "CERTIFICATE_EXPIRING", planos, hoje, cliente, null, vars);
  }
  return geradas;
}

async function processarVisitas(sql: postgres.Sql, hoje: Date, configs: Config[], pessoas: Map<string, Pessoa>): Promise<number> {
  if (configs.length === 0) return 0;
  // Apenas visitas operacionais (nao os eventos sinteticos de vencimento de contrato/certificado).
  // No schema, o "serviceType" do dominio e persistido na coluna tipo_evento (nao ha tipo_servico).
  const rows = await sql`
    select id::text as id, cliente_id::text as cliente_id, responsavel_id::text as responsavel_id,
           data_inicio, coalesce(status, 'SCHEDULED') as status, coalesce(lembrete_ativo, false) as lembrete_ativo,
           dias_antecedencia_lembrete, lembrete_dias_conjunto, coalesce(tipo_evento, '') as tipo_servico,
           coalesce(titulo, '') as titulo
      from agenda_eventos
     where contrato_id is null and certificado_id is null
       and coalesce(tipo_evento,'') not in ('contrato_vencimento','certificado_vencimento')`;
  let geradas = 0;
  for (const v of rows) {
    const cliente = pessoas.get(v.cliente_id);
    const funcionario = v.responsavel_id ? pessoas.get(v.responsavel_id) : undefined;
    const dataVisita = new Date(String(v.data_inicio).slice(0, 10) + "T00:00:00.000Z");
    const agendavel = v.status === "SCHEDULED" && dataVisita >= hoje;
    const planos: Plano[] = [];
    if (agendavel) {
      const dias = diasEfetivos(parseConjunto(v.lembrete_dias_conjunto), v.lembrete_ativo, v.dias_antecedencia_lembrete);
      for (const cfg of configs) {
        for (const d of dias) {
          const trigger = isoDate(subtraiDias(dataVisita, d));
          for (const lado of ladosDe(cfg.destinatario)) planos.push({ cfg, lado, triggerDate: trigger });
        }
      }
    }
    const vars = {
      cliente_nome: cliente?.nome ?? "",
      cliente_telefone: cliente?.telefone ?? "",
      cliente_email: cliente?.email ?? "",
      endereco: cliente?.cidade ?? "",
      funcionario_nome: funcionario?.nome ?? "",
      funcionario_telefone: funcionario?.telefone ?? "",
      data_visita: formatData(String(v.data_inicio).slice(0, 10)),
      tipo_servico: v.tipo_servico || v.titulo,
    };
    geradas += await reconciliarEGerar(sql, v.id, "VISIT_UPCOMING", planos, hoje, cliente, funcionario, vars);
  }
  return geradas;
}

// ---------------------------- reconciliacao/geracao --------------------------

async function reconciliarEGerar(
  sql: postgres.Sql,
  entityId: string,
  type: string,
  planos: Plano[],
  hoje: Date,
  cliente: Pessoa | undefined,
  funcionario: Pessoa | undefined,
  vars: Record<string, string>,
): Promise<number> {
  const esperado = new Set(planos.map((p) => `${p.lado}|${p.triggerDate}`));

  const pendentes = await sql`
    select id::text as id, recipient_type, trigger_date::text as trigger_date
      from notificacoes where related_entity_id = ${entityId} and type = ${type} and status = 'PENDING'`;
  for (const p of pendentes) {
    if (!esperado.has(`${p.recipient_type}|${p.trigger_date}`)) {
      await sql`update notificacoes set status = 'CANCELLED' where id = ${p.id}`;
    }
  }

  let criadas = 0;
  const jaVistos = new Set<string>();
  for (const plano of planos) {
    const chave = `${plano.lado}|${plano.triggerDate}`;
    if (jaVistos.has(chave)) continue;
    jaVistos.add(chave);
    if (plano.triggerDate > isoDate(hoje)) continue; // ainda nao chegou o dia

    const dup = await sql`
      select 1 from notificacoes
       where type = ${type} and related_entity_id = ${entityId}
         and recipient_type = ${plano.lado} and trigger_date = ${plano.triggerDate}
         and status in ${sql(STATUS_ATIVOS)} limit 1`;
    if (dup.length > 0) continue;

    const nova = construir(plano, entityId, type, cliente, funcionario, vars);
    if (!nova) continue;
    await inserir(sql, nova);
    criadas++;
  }
  return criadas;
}

function construir(
  plano: Plano,
  entityId: string,
  type: string,
  cliente: Pessoa | undefined,
  funcionario: Pessoa | undefined,
  vars: Record<string, string>,
) {
  const cfg = plano.cfg;
  const alvo = plano.lado === "CLIENTE" ? cliente : funcionario;
  if (!alvo || !alvo.nome) return null;

  let telefone = "";
  if (cfg.fonte_telefone === "INFORMADO") telefone = cfg.telefone_informado ?? "";
  else if (cfg.destinatario === "AMBOS") telefone = plano.lado === "CLIENTE" ? (cliente?.telefone ?? "") : (funcionario?.telefone ?? "");
  else telefone = cfg.fonte_telefone === "CLIENTE" ? (cliente?.telefone ?? "") : (funcionario?.telefone ?? "");

  if (!telefoneValido(telefone)) return null;
  const fone = normalizarTelefone(telefone);

  let mensagem = renderizar(cfg.template_mensagem, vars);
  if (!mensagem.trim()) mensagem = cfg.titulo;

  const senderType = cfg.origem_tipo === "FUNCIONARIO" ? "FUNCIONARIO" : cfg.origem_tipo === "CLIENTE" ? "CLIENTE" : "SISTEMA";
  const senderName = senderType === "FUNCIONARIO" ? (funcionario?.nome ?? "") : senderType === "CLIENTE" ? (cliente?.nome ?? "") : "SIGLA";

  return {
    id: crypto.randomUUID(),
    type,
    title: cfg.titulo,
    message: mensagem,
    related_entity_id: entityId,
    trigger_date: plano.triggerDate,
    scheduled_for: `${plano.triggerDate}T${String(HORA_ENVIO).padStart(2, "0")}:00:00`,
    recipient_type: plano.lado,
    recipient_name: alvo.nome,
    recipient_phone: fone,
    customer_id: cliente?.id ?? "",
    employee_id: funcionario?.id ?? "",
    sender_type: senderType,
    sender_name: senderName,
    template_id: cfg.id,
    canal: cfg.canal ?? "WHATSAPP_N8N",
    created_by: cfg.criado_por || "SISTEMA",
    metadata: vars,
  };
}

async function inserir(sql: postgres.Sql, n: ReturnType<typeof construir>) {
  if (!n) return;
  await sql`
    insert into notificacoes
      (id, type, title, message, related_entity_id, trigger_date, status, recipient_type, recipient_name,
       recipient_phone, customer_id, employee_id, sender_type, sender_name, template_id, scheduled_for,
       source, channel, attempts, metadata, created_by)
    values
      (${n.id}, ${n.type}, ${n.title}, ${n.message}, ${n.related_entity_id}, ${n.trigger_date}, 'PENDING',
       ${n.recipient_type}, ${n.recipient_name}, ${n.recipient_phone}, ${n.customer_id || null},
       ${n.employee_id || null}, ${n.sender_type}, ${n.sender_name}, ${n.template_id}, ${n.scheduled_for},
       'SIGLA', ${n.canal}, 0, ${JSON.stringify(n.metadata)}, ${n.created_by})`;
}

// --------------------------------- dispatch ----------------------------------

async function dispatchDue(sql: postgres.Sql): Promise<number> {
  const falhas = await sql`
    select * from notificacoes where status = 'FAILED' and attempts < ${MAX_TENTATIVAS}`;
  const pendentes = await sql`
    select * from notificacoes
     where status = 'PENDING'
       and (scheduled_for <= now() or (scheduled_for is null and trigger_date <= current_date))`;

  let enviadas = 0;
  for (const n of [...pendentes, ...falhas]) {
    if (await enviarEAtualizar(sql, n)) enviadas++;
  }
  return enviadas;
}

async function enviarEAtualizar(sql: postgres.Sql, n: Record<string, unknown>): Promise<boolean> {
  const r = await enviarWebhook(n);
  if (r === "ENVIADO" || r === "SIMULADO") {
    await sql`update notificacoes set status = 'SENT', sent_at = now(), last_error = null where id = ${n.id as string}`;
    return true;
  }
  if (r === "DESABILITADO") return false; // mantem PENDING
  await sql`update notificacoes set status = 'FAILED', attempts = coalesce(attempts,0) + 1, last_error = ${r} where id = ${n.id as string}`;
  return false;
}

async function enviarWebhook(n: Record<string, unknown>): Promise<string> {
  if (!ENVIO_HABILITADO) return "DESABILITADO";
  if (MODO_TESTE) return "SIMULADO";
  if (!WEBHOOK_URL) return "URL do webhook nao configurada";

  const payload = {
    eventId: n.id,
    eventType: n.type,
    source: n.source ?? "SIGLA",
    recipientType: n.recipient_type ?? "",
    recipientName: n.recipient_name ?? "",
    recipientPhone: n.recipient_phone ?? "",
    senderType: n.sender_type ?? "",
    senderName: n.sender_name ?? "",
    customerId: n.customer_id ?? "",
    employeeId: n.employee_id ?? "",
    relatedEntityId: n.related_entity_id ?? "",
    templateId: n.template_id ?? "",
    message: n.message ?? "",
    scheduledFor: n.scheduled_for ?? "",
    modoTeste: MODO_TESTE,
    metadata: typeof n.metadata === "string" ? JSON.parse(n.metadata as string) : (n.metadata ?? {}),
  };

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), TIMEOUT_MS);
  try {
    const headers: Record<string, string> = { "Content-Type": "application/json" };
    if (WEBHOOK_TOKEN) headers["Authorization"] = `Bearer ${WEBHOOK_TOKEN}`;
    const resp = await fetch(WEBHOOK_URL, { method: "POST", headers, body: JSON.stringify(payload), signal: controller.signal });
    return resp.ok ? "ENVIADO" : `Webhook retornou HTTP ${resp.status}`;
  } catch (e) {
    return e instanceof DOMException && e.name === "AbortError" ? "Timeout do webhook" : `Falha de envio: ${String(e)}`;
  } finally {
    clearTimeout(timer);
  }
}

// ---------------------------------- ledger -----------------------------------

async function registrarExecucao(sql: postgres.Sql, data: string) {
  await sql`
    insert into notificacao_execucao_log (data, executado_em, origem)
    values (${data}, now(), 'EDGE')
    on conflict (data) do update set executado_em = now(), origem = 'EDGE'`;
}

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}
