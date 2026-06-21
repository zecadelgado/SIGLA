# Notificações WhatsApp — Workflow n8n (Uazap)

Este diretório contém o **workflow do n8n** que recebe o webhook enviado pelo SIGLA e dispara a
mensagem via **API da Uazap**. O SIGLA nunca chama a Uazap diretamente — ele só faz `POST` neste
webhook; o n8n valida, envia e devolve sucesso/falha.

Arquivo para importar: [`sigla-notificacoes-whatsapp.json`](sigla-notificacoes-whatsapp.json)

## Fluxo

```
SIGLA ──POST──▶ [Webhook SIGLA] ─▶ [Validar token] ─▶ [Enviar Uazap] ─▶ [Responder OK] ──▶ SIGLA
                                                            │
                                                     (erro) └────────────▶ HTTP 500 ──▶ SIGLA marca FAILED
```

1. **Webhook SIGLA** — recebe o `POST` (responseMode = "Respond to Webhook").
2. **Validar token** — confere `Authorization: Bearer <token>` contra `SIGLA_NOTIFICACOES_WEBHOOK_TOKEN`
   (pula a checagem se o token não estiver configurado / se o n8n bloquear `$env` no Code node).
3. **Enviar Uazap** — `POST` para a API da Uazap com `number` e `text`.
4. **Responder OK** — retorna `200` com `{status:"sent", eventId, uazap:...}`. Se o envio falhar, a
   execução aborta e o n8n responde erro → o SIGLA registra **FAILED** (e permite reprocessar).

## Como importar

1. No n8n: **Workflows ▸ Import from File** e selecione `sigla-notificacoes-whatsapp.json`.
2. Defina as variáveis de ambiente do n8n (Settings ▸ Variables ou env do servidor):
   - `UAZAP_BASE_URL` — URL base da sua instância Uazap (ex.: `https://sua-instancia.uazapi.com`)
   - `UAZAP_TOKEN` — token/instância da Uazap
   - `SIGLA_NOTIFICACOES_WEBHOOK_TOKEN` — (opcional) mesmo token configurado no SIGLA, para autenticar o webhook
3. **Salve e ative** o workflow.
4. Copie a **Production URL** do node *Webhook SIGLA* (algo como
   `https://seu-n8n/webhook/sigla-notificacoes`).

## Como ligar no SIGLA

Configure as variáveis de ambiente do SIGLA (ver `application-infra.yml`):

```
SIGLA_NOTIFICACOES_WEBHOOK_URL=https://seu-n8n/webhook/sigla-notificacoes
SIGLA_NOTIFICACOES_WEBHOOK_TOKEN=<mesmo token do n8n, se usar>
SIGLA_NOTIFICACOES_WHATSAPP_ENABLED=true
SIGLA_NOTIFICACOES_MODO_TESTE=false
```

> Enquanto `WHATSAPP_ENABLED=false` ou `MODO_TESTE=true`, o SIGLA **não** envia de verdade
> (em dev, modo-teste já vem ligado). Para um teste real, use o botão **"Enviar teste"** na tela
> *Notificações* informando o seu próprio número.

## ⚠️ Ajuste o node "Enviar Uazap" para a sua versão da Uazap

A URL/headers/body do node são um **placeholder** comum (uazapi). Confirme no painel/documentação
da sua Uazap e ajuste se necessário. O workflow assume:

- **URL:** `{{UAZAP_BASE_URL}}/send/text`
- **Header:** `token: {{UAZAP_TOKEN}}`
- **Body (JSON):** `{ "number": "<telefone>", "text": "<mensagem>" }`

Se a sua API usar outro caminho/campos (ex.: `instance`, `phone`, `message`, ou header
`Authorization`), basta editar o node — o resto do fluxo continua igual.

## Payload que o SIGLA envia

```json
{
  "eventId": "id-unico-da-notificacao",
  "eventType": "VISIT_UPCOMING",
  "source": "SIGLA",
  "recipientType": "CLIENTE",
  "recipientName": "Nome do cliente",
  "recipientPhone": "5599999999999",
  "senderType": "SISTEMA",
  "senderName": "SIGLA",
  "customerId": "id-do-cliente",
  "employeeId": "id-do-funcionario",
  "relatedEntityId": "id-da-visita-ou-contato",
  "templateId": "id-do-template",
  "message": "Mensagem final personalizada",
  "scheduledFor": "2026-06-18T08:00:00",
  "modoTeste": false,
  "metadata": { "cliente_nome": "...", "data_visita": "20/06/2026", "...": "..." }
}
```

Dentro do n8n, esses campos ficam em `{{ $json.body.* }}` (ex.: `{{ $json.body.recipientPhone }}`,
`{{ $json.body.message }}`). A `message` já vem pronta (renderizada pelo SIGLA), mas você pode
recompor/complementar usando `metadata` se quiser.

## Teste rápido (sem o SIGLA)

`curl` direto no webhook para validar o envio pela Uazap:

```bash
curl -X POST "https://seu-n8n/webhook/sigla-notificacoes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer SEU_TOKEN" \
  -d '{"eventId":"teste-1","eventType":"MANUAL","recipientPhone":"5599999999999","message":"Teste SIGLA via n8n"}'
```

Esperado: `200` com `{"status":"sent",...}` e a mensagem chegando no WhatsApp.
