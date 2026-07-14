# Relatório da Fase 6 — Supabase, segurança e homologação

Data: 2026-07-14. Branch: `homolog`. Pré-condições: Fases 3, 4 e 5 concluídas,
validadas em PostgreSQL 17.5 real e commitadas.

**Ponto de parada respeitado (§7.7):** homologação concluída em projeto Supabase
**isolado**; produção (`jhderjgvzaxgzvcbkeol`) **não** foi tocada (exceto uma
leitura de `flyway_schema_history` na Fase 3), `pg_cron` **não** foi ativado e
nenhum deploy definitivo foi feito. As ações que exigem aprovação estão
listadas em §8.

## 1. Entregas de código (V27 + Edge + docs)

- **V27__rls_negacao_padrao_fase6.sql** (aditiva): RLS habilitado em todas as
  tabelas `public`; `REVOKE` de tabela/sequência/função para `anon` e
  `authenticated`; `search_path` fixo em todas as funções `public` (inclui as
  SECURITY DEFINER); `EXECUTE` das RPCs (`rpc_faturar_mensalidades_v1`,
  `desvincular_os_contratual_v1`) concedido **somente a `service_role`** —
  `authenticated` perdeu o grant que a V24 dava na desvinculação. Auditoria do
  faturamento: coluna `origem`/`ator` em `sigla_faturamento_execucoes` + evento
  `FATURAMENTO_EXECUTADO`; a RPC ganhou `p_origem`/`p_ator` com default (o
  desktop não muda).
- **Edge Function** `notificacoes-agendador`: deixa de replicar regra de
  negócio — apenas dispara `rpc_faturar_mensalidades_v1` (origem
  `EDGE_FUNCTION`, chave idempotente por data em `America/Sao_Paulo`), com modo
  seco (`SIGLA_AGENDADOR_DRY_RUN`) e logs estruturados.
- **Docs/scripts**: `docs/fase6-matriz-acessos.md` (matriz por perfil),
  `docs/fase6-plano-cutover-rollback.md` (checklist, feature flags, rollback),
  `scripts/fase6-pg-cron-preparado.sql` (agendamento preparado, não ativado).

## 2. Homologação em projeto Supabase isolado

- Projeto **`sigla-homologacao`** (`ctbmgxhamwyoytolnddy`), região `sa-east-1`,
  custo **US$ 0/mês** (free) — criado após confirmação de custo. Para caber no
  limite de 2 projetos free, o projeto `NeoBenesys` foi **pausado**
  (reversível; religável no dashboard) com sua aprovação.
- **Migrations V1→V27 aplicadas na ordem completa** via `apply_migration`
  (lotes que reproduzem o conteúdo Flyway). Resultado: **31 tabelas, todas com
  RLS habilitado; 0 funções SECURITY DEFINER sem `search_path` fixo**.

## 3. Validações executadas na homologação (dados descartáveis, já removidos)

| Validação | Resultado |
|---|---|
| RLS por papel | `anon` e `authenticated` **negados (42501)** em `contratos`, `financeiro_lancamentos`, `estoque_movimentacoes` e nas RPCs de faturamento e desvinculação |
| Fluxo transversal completo | contrato → vigência → OS coberta/extra → reserva → consumo → cobrança → pagamento → estorno → faturamento: OS coberta com **0 cobranças próprias**, estoque 10→7, **1 estorno** espelho, OS extra herdando vigência, mensalidade única, **reconciliação de estoque sem divergência** |
| Faturamento por `service_role` | executa via grant mínimo (papel técnico fatura pela RPC, sem escrita direta) |
| Concorrência/retry do faturamento | retry com outra chave → `JA_EXISTENTE`, **mensalidade não duplicada**; origem/ator gravados; **disparo auditado** |
| Imutabilidade da razão | `DELETE` na `estoque_movimentacoes` bloqueado (limpeza exigiu `session_replication_role=replica`, confirmando a garantia) |
| Advisors de segurança | **0 achados ERROR**. 31 INFO `rls_enabled_no_policy` (esperados: é o modelo de acesso — RLS sem policy nega anon/authenticated, app passa por bypass). 1 WARN `btree_gist` no schema public (pré-existente da V19, idêntico à produção) |
| Edge Function | deploy **ACTIVE** com `verify_jwt` (bundle + type-check do runtime Edge). Versão de homologação focada no disparo do faturamento; comportamento SQL do modo seco já validado diretamente |
| Base de homologação | dados de teste (prefixo `9999`) **removidos**; base limpa |

## 4. Testes locais (regressão, PostgreSQL 17.5 real)

- `./mvnw.cmd test`: 173 unidade, 0 falhas.
- `./mvnw.cmd -pl sigla-infraestrutura verify` com `SIGLA_IT_JDBC_URL`:
  **V1→V27 do zero; `FundacaoSchemaPostgreSQLIT`: 33 testes, 0 falhas, 0
  ignorados** — inclui RLS negando `anon`/`authenticated` em tabelas e RPCs,
  faturamento por `service_role`, e auditoria de origem/ator.
- `git diff --check`: limpo.

## 5. Commits (branch `homolog`)

`f93d819` Fase 3 · `7d0ebcf` Fase 4 · `22b08f1` Fase 5 · `aa3998b` Fase 6.

## 6. Matriz de acessos (resumo — detalhe em `fase6-matriz-acessos.md`)

`postgres` (desktop/Flyway) e `service_role` (jobs): acesso pleno por bypass.
`anon`/`authenticated`: **nada** (RLS sem policy + REVOKE). Perfis funcionais
(ADMIN/OPERADOR/FINANCEIRO/TECNICO) são validados **dentro das RPCs**
(`usuarios.tipo/ativo`), nunca por UUID informado pelo cliente.

## 7. Reconciliação

`reconciliar_estoque_v1()` sem divergência no fluxo de homologação; scripts
`scripts/fase5-reconciliacao.sql` e `scripts/fase5-diagnostico-financeiro-contratos.sql`
disponíveis para rodar sobre cópia/dados anonimizados no cutover.

## 8. Ações que ainda exigem aprovação explícita (produção)

1. Aplicar V18→V27 na produção `jhderjgvzaxgzvcbkeol` (via Flyway do desktop na
   inicialização, ou Flyway CLI antecipado). Produção está em V17.
2. Deploy da Edge Function completa (repo `index.ts`, com notificações) na
   produção, definir a Function Secret `SUPABASE_DB_URL`, rodar primeiro ciclo
   em `SIGLA_AGENDADOR_DRY_RUN=true` e então ativar escrita.
3. Ativar o agendador (cron do Supabase ou `scripts/fase6-pg-cron-preparado.sql`).
4. Rotacionar chaves `anon`/`service_role` e senha do banco (pendência de
   segurança herdada — RLS não limita `service_role`).
5. Religar/pausar definitivamente o projeto `NeoBenesys` e descartar o projeto
   de homologação `sigla-homologacao` quando não for mais necessário.

## 9. Observações

- A homologação foi populada via `apply_migration` (que registra em
  `supabase_migrations`, não em `flyway_schema_history`). Isso é adequado para
  homologação; **o cutover de produção usa o Flyway real do desktop**, que já
  tem V1–V17 aplicadas e aplicará V18→V27 com os checksums do repositório.
- `var/os_java.pdf`/`var/visita_java.pdf` continuam mudando a cada build (efeito
  pré-existente do teste de formulários sobre arquivos versionados).
