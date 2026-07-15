# Relatório do cutover de produção — SIGLA (Fases 3–6)

Data: 2026-07-15. Produção: projeto Supabase `jhderjgvzaxgzvcbkeol` ("Siglas
Project"). Aprovação explícita do usuário registrada em chat ("aprovado, pode
publicar").

## 1. O que foi publicado

- **Migrations V18→V28** aplicadas na ordem completa via API administrativa,
  com o conteúdo idêntico ao validado localmente (PostgreSQL 17.5 real, 34
  testes de integração) e na homologação (`sigla-homologacao`).
- **`flyway_schema_history` registrada** para V18–V28 com os checksums reais do
  Flyway do build (`installed_by = cutover-fase6`). Método validado antes:
  os checksums locais de V2–V17 conferiram 100% com os registros históricos da
  produção (V1 é baseline, sem checksum). O desktop novo valida e **não**
  tenta reaplicar nada; o desktop antigo (V17) ignora as versões futuras.
- **Edge Function `notificacoes-agendador` v3** (deploy em produção, mesma
  slug, `verify_jwt` e segredos preservados): o faturamento agora dispara a
  autoridade única `rpc_faturar_mensalidades_v1` (origem `EDGE_FUNCTION`,
  chave idempotente por data em `America/Sao_Paulo`); falha no faturamento não
  derruba o ciclo de notificações; modo seco disponível
  (`SIGLA_AGENDADOR_DRY_RUN`).

## 2. V28 — convivência com o produtor legado (criada para este cutover)

Descoberta no ensaio: em produção (V17) as mensalidades legadas não tinham
`contrato_id` — apenas o **id determinístico** (UUIDv3 de
`contrato-mensalidade:<contrato>:<AAAA-MM>`) e o marcador nas observações.
A V28 usa isso como **prova estrutural**: o contrato/competência extraídos do
marcador só são aceitos quando o UUID recomputado bate exatamente com o id da
linha. Com isso:

- **Backfill**: as 2 mensalidades legadas de produção foram formalizadas
  (origem `CONTRATO`, competência, chave `MENSALIDADE:<contrato>:<AAAA-MM>`,
  vigência) — 0 linhas restantes sem identidade.
- **Normalização em gravação**: enquanto houver desktop antigo rodando o
  produtor Java, o insert legado recebe a identidade formal na hora; se a RPC
  já faturou o mês, a chave única rejeita a duplicata; se o legado chega
  primeiro, a RPC responde `JA_EXISTENTE`. **Nunca há duas mensalidades ativas
  do mesmo contrato/mês na transição.**

## 3. Verificação pós-migração (produção)

| Checagem | Resultado |
|---|---|
| Tabelas com RLS | 32/32 (inclui `flyway_schema_history`, já habilitada antes) |
| Vigências iniciais | 7/7 contratos |
| Agenda com intervalo inválido | 1 evento isolado em `agenda_eventos_legado_inconsistencias` (como previsto no pré-check) |
| Mensalidades legadas | 2 formalizadas; 0 sem chave |
| Regra de cobrança de OS | 5 OS classificadas por evidência; 3 ambíguas em `vw_os_contratuais_sem_regra_v1` (conciliação manual) |
| Estoque | 14 baselines; `reconciliar_estoque_v1()` = 0 divergências |
| OVERDUE persistido | 0 |
| Integridade | soma de linhas (cadastro+contratos+OS+lançamentos) = 55, idêntica ao pré-cutover |
| Funções sem `search_path` fixo | 0 (as 2 da V28 fixadas fora de banda, igual nos dois ambientes) |
| Advisors de segurança | 0 ERROR; INFO `rls_enabled_no_policy` = modelo de acesso desenhado; WARNs pré-existentes de plataforma (extensões em `public`, leaked-password-protection do Auth) |

**Prévia do faturamento** (somente leitura): 5 contratos ativos terão a
mensalidade de julho/2026 criada no próximo ciclo da Edge — as legadas
formalizadas são de meses anteriores, então não há duplicação possível.

## 4. O que NÃO foi executado (e por quê)

- **Geração manual de mensalidades**: bloqueada pelo classificador de
  segurança do Claude Code (escrita financeira real). Sem prejuízo: o ciclo
  agendado da Edge fará a primeira execução; a prévia acima confirma o
  resultado esperado. Alternativa manual:
  `select * from rpc_faturar_mensalidades_v1(current_date, 'FATURAMENTO:MANUAL:<data>', 'MANUAL', null);`
- **pg_cron**: não ativado (o agendamento existente da Edge já cobre;
  `scripts/fase6-pg-cron-preparado.sql` permanece como alternativa).

## 5. Rollback disponível

- `faturamento_rpc_v1 = OFF` interrompe o faturamento imediatamente (falha
  explícita, sem silêncio); religar + reinvocar com a mesma chave é
  idempotente.
- Migrations são aditivas: o desktop antigo (V17) continua operando sobre o
  schema novo; correções de dados só por compensação (estorno/crédito/
  reembolso e movimentos compensatórios) — `DELETE` de histórico é bloqueado
  por trigger.
- Edge Function: redeploy da versão anterior pelo dashboard (histórico de
  versões), se necessário.

## 6. Pós-cutover — acompanhamento e pendências

1. **Monitorar 3 dias**: logs da Edge (`etapa=faturamento`),
   `sigla_faturamento_execucoes`, `reconciliar_estoque_v1()` e
   `scripts/fase5-reconciliacao.sql`.
2. **Distribuir o desktop novo** (build da branch `homolog`): o antigo segue
   funcional (normalização V28 protege o faturamento), mas o novo traz
   BigDecimal, regra de cobrança na UI e o faturamento via RPC.
3. **Classificar as 3 OS contratuais ambíguas** em
   `vw_os_contratuais_sem_regra_v1` (decisão de negócio: coberta × extra).
4. **Rotacionar chaves `anon`/`service_role` e senha do banco** (pendência de
   segurança herdada; RLS não limita `service_role`).
5. Decidir destino do projeto de homologação `sigla-homologacao` (em paridade
   V28) e religar `NeoBenesys` se desejar (foi pausado para abrir a vaga free).

## 7. Commits (branch `homolog`)

`f93d819` F3 · `7d0ebcf` F4 · `22b08f1` F5 · `aa3998b` F6 · `d6216e7`
relatório homolog · `23e87e0` V28 cutover.
