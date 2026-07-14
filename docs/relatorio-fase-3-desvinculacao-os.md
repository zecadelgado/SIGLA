# Relatório da Fase 3 — correção final da desvinculação de OS contratual

Data: 2026-07-13. Branch: `homolog`.

## 1. Problema corrigido

A RPC `desvincular_os_contratual_v1` (SECURITY DEFINER, V24) aceitava qualquer
`p_usuario_id` não nulo: um chamador com `EXECUTE` podia forjar o UUID de um
administrador e desvincular uma OS contratual sem ser ADMIN.

## 2. Correção aplicada

Arquivo: `sigla-infraestrutura/src/main/resources/db/migration/V24__protege_os_contratual.sql`

- A RPC agora carrega o usuário de `usuarios` e rejeita com `SQLSTATE 23514`:
  - usuário inexistente (`usuario da desvinculacao nao existe`);
  - usuário com `tipo` diferente de `ADMIN` (OPERADOR/FINANCEIRO/TECNICO);
  - `ADMIN` com `ativo = false`.
- Mantidos, sem alteração: desvinculação restrita a OS `AGENDADA`, futura, não
  iniciada e não concluída; auditoria `OS_CONTRATO_DESVINCULADO_ADMIN`; bloqueio
  do `UPDATE` comum de `contrato_id` para `NULL` (tabela de autorizações por
  transação + trigger `validar_os_contratual`).

A V24 foi editada em vez de criar V25 porque **nunca foi aplicada em ambiente
persistente**: verificação somente leitura na produção Supabase
(`jhderjgvzaxgzvcbkeol`) confirmou `flyway_schema_history` com máximo **V17**
(17 migrations). V18–V24 permanecem inéditas, como afirmado no plano (§3.3).

Correção acessória para o build passar (falha pré-existente no HEAD):
`ServicoConsultaOrdemServicoTest.FakeOrdemServico` não implementava o método
`desvincularContratoAdministrativamente` exigido pela interface
`CasoDeUsoOrdemServico` — implementado no fake (lança
`UnsupportedOperationException`, padrão do arquivo).

## 3. Testes adicionados

`sigla-infraestrutura/src/integrationTest/java/br/com/sigla/infraestrutura/migration/FundacaoSchemaPostgreSQLIT.java`

Novo teste `rpcDesvinculacaoRejeitaUsuarioNaoAdminOuInativoEAutorizaSomenteAdminAtivo`:

- rejeita com `23514`: usuário inexistente, `OPERADOR`, `FINANCEIRO`,
  `TECNICO` e `ADMIN` inativo;
- após as rejeições, a OS permanece vinculada ao contrato e não existe
  auditoria de desvinculação;
- `ADMIN` ativo desvincula com sucesso; `contrato_id` fica `NULL` e a
  auditoria registra ação, motivo e `usuario_id` do ADMIN.

Cenário de ADMIN ativo + auditoria também continua coberto pelo teste
pré-existente `bloqueiaUpdateQueDesvinculaContratoEExigeRpcAdministrativaAuditavel`.

## 4. Execução real contra PostgreSQL (SIGLA_IT_JDBC_URL)

Docker/Testcontainers indisponível na máquina; não havia PostgreSQL local.
Foi provisionado um **PostgreSQL 17.5 real** (binários oficiais
io.zonky.test.postgres `embedded-postgres-binaries-windows-amd64:17.5.0`,
mesma major da produção) em `localhost:54329`, e a integração rodou com:

```
SIGLA_IT_JDBC_URL=jdbc:postgresql://localhost:54329/postgres
SIGLA_IT_DB_USER=postgres
SIGLA_IT_DB_PASSWORD=postgres
```

O banco de produção Supabase **não** foi usado para testes.

## 5. Comandos executados e resultados reais

| Comando | Resultado |
|---|---|
| `./mvnw.cmd test` | BUILD SUCCESS — 170 testes (17 domínio + 93 aplicação + 18 infraestrutura + 2 relatórios + 40 interface), 0 falhas, 0 ignorados |
| `./mvnw.cmd -pl sigla-infraestrutura -am verify` (com `SIGLA_IT_JDBC_URL`) | BUILD SUCCESS — `FundacaoSchemaPostgreSQLIT`: **15 testes executados, 0 falhas, 0 erros, 0 ignorados** (7,7 s) contra PostgreSQL 17.5 real; Flyway aplicou V1→V24 do zero |
| `git diff --check` | limpo (sem erros de whitespace) |

Nenhum teste foi ignorado por falta de Docker: o fallback Testcontainers não
foi acionado porque `SIGLA_IT_JDBC_URL` estava definida.

## 6. Pendências e fora de escopo

- **Fora de escopo desta fase (reservado às Fases 4–6):** BigDecimal ponta a
  ponta no estoque, faturamento mensal por RPC, RLS completo, deploy Supabase.
- V18–V24 continuam **não publicadas** em produção (produção está em V17);
  a publicação ocorrerá na homologação/cutover da Fase 6.
- Ruído pré-existente do repositório: diretórios `target/` estão versionados e
  ficam modificados após cada build (sugerida tarefa separada de .gitignore).
