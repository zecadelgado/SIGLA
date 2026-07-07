# notificacoes-agendador (Edge Function)

Runtime externo **sempre-ligado** do SIGLA: gera e dispara as notificações de
vencimento (contrato/certificado) e de serviços agendados (visitas) **mesmo com
nenhum desktop aberto**, via webhook n8n → Uazap.

Espelha a lógica Java (`CasoDeUsoGerarNotificacoes`, `RenderizadorTemplate`,
`TelefoneWhatsapp`, `DiasLembrete`, `AdaptadorEnvioWhatsappN8n`). O payload enviado ao
n8n é idêntico ao do desktop.

> ⚠️ **Aplicar e validar primeiro no projeto Supabase de HOMOLOGAÇÃO.** Só habilitar o
> `pg_cron` em produção depois de conferir os resultados. O banco é produção — ver as
> memórias `banco-producao-nao-alterar` / `supabase-projeto-rls`.

## Pré-requisitos (uma vez)

A migração `V17__notificacoes_escalonamento_execucao.sql` precisa estar aplicada
(colunas `*_dias_conjunto` e a tabela `notificacao_execucao_log`). O Flyway do desktop
aplica no startup; aponte o datasource para o homolog antes.

## Escopo atual

- ✅ Geração: `CONTRACT_EXPIRING`, `CERTIFICATE_EXPIRING`, `VISIT_UPCOMING` (com
  escalonamento 30/15/7/1) + reconciliação/dedup por dia.
- ✅ Dispatch: envia `PENDING` vencidas e reprocessa `FAILED` (até `MAX_TENTATIVAS`).
- ⏳ Pendente (fazer após validar schema financeiro em homolog): `INSTALLMENT_OVERDUE`
  (parcela em atraso) e faturamento de mensalidade — hoje continuam no desktop.

## Deploy

```bash
supabase link --project-ref <REF_DO_HOMOLOG>
supabase functions deploy notificacoes-agendador
```

## Segredos (Function Secrets — nunca em tabela)

```bash
supabase secrets set \
  SIGLA_NOTIFICACOES_WEBHOOK_URL="https://webhookn8n.lero.pro/webhook/sigla-notificacoes" \
  SIGLA_NOTIFICACOES_WEBHOOK_TOKEN="<token-ou-vazio>" \
  SIGLA_NOTIFICACOES_WHATSAPP_ENABLED="false" \
  SIGLA_NOTIFICACOES_MODO_TESTE="true" \
  SIGLA_NOTIFICACOES_HORA_ENVIO="8"
```

`SUPABASE_DB_URL` já é injetada automaticamente pelo runtime das Edge Functions.

**Validação segura:** comece com `WHATSAPP_ENABLED=false` (nada sai) ou
`MODO_TESTE=true` (simula). Rode manualmente e confira as linhas em `notificacoes`
(status/scheduled_for) e o `notificacao_execucao_log`. Só então ligue o envio real.

## Invocação manual (teste)

```bash
curl -i -X POST "https://<REF>.functions.supabase.co/notificacoes-agendador" \
  -H "Authorization: Bearer <ANON_OU_SERVICE_KEY>"
```

## Agendamento com pg_cron + pg_net

Rode no **SQL editor do homolog** (a função é idempotente: gerar + disparar a cada
execução não duplica, graças ao dedup por dia). Uma execução horária cobre o disparo
no horário e o catch-up de dias não processados.

```sql
create extension if not exists pg_cron;
create extension if not exists pg_net;

-- Guarde a URL e a chave de serviço em Vault (ou substitua abaixo diretamente).
select cron.schedule(
  'sigla-notificacoes-horaria',
  '0 * * * *',  -- de hora em hora
  $$
  select net.http_post(
    url    := 'https://<REF>.functions.supabase.co/notificacoes-agendador',
    headers:= jsonb_build_object(
                'Content-Type','application/json',
                'Authorization','Bearer <SERVICE_ROLE_KEY>'),
    body   := '{}'::jsonb
  );
  $$
);

-- Para remover:  select cron.unschedule('sigla-notificacoes-horaria');
```

## Anti-duplicação (desktop × edge)

Enquanto a Edge Function estiver ativa em produção, o agendador embutido no desktop
deve ficar **desligado** (`sigla.notificacoes.scheduler.enabled=false`, que é o default).
O desktop segue configurando templates e visualizando o histórico; quem envia é a Edge
Function. O dedup no banco é a rede de segurança caso ambos rodem.
