# Fase 6 — Matriz de acesso e identidade

Como cada ator se conecta ao banco e o que pode fazer após a V27 (RLS com
negação por padrão). Papéis funcionais (ADMIN/OPERADOR/FINANCEIRO/TECNICO)
são papéis **da aplicação** (tabela `usuarios`), validados dentro das RPCs —
não são roles PostgreSQL.

## Conexões

| Ator | Conexão | Papel PostgreSQL |
|---|---|---|
| Desktop Java (SIGLA) | JDBC direto (pooler) | `postgres` (BYPASSRLS) |
| Flyway (migrations) | JDBC direto na inicialização do desktop | `postgres` |
| Edge Function `notificacoes-agendador` | `SUPABASE_DB_URL` (driver postgres) | `postgres`/`service_role` (BYPASSRLS) |
| Supabase Auth (login/recuperação) | API GoTrue (não toca o schema public) | — |
| Cliente anônimo (chave `anon`) | PostgREST | `anon` |
| Usuário autenticado (JWT) | PostgREST | `authenticated` |

## Matriz tabela/operação/papel (após V27)

| Objeto | postgres (desktop/Flyway) | service_role (job técnico) | anon | authenticated |
|---|---|---|---|---|
| Tabelas de negócio (todas em `public`) | tudo (bypass RLS) | tudo (grants padrão Supabase + BYPASSRLS) — uso legítimo: leitura p/ notificações e escrita em `notificacoes*` | **nada** (REVOKE + RLS sem policy) | **nada** (REVOKE + RLS sem policy) |
| `rpc_faturar_mensalidades_v1` | EXECUTE (owner) | EXECUTE (grant explícito) | negado | negado |
| `desvincular_os_contratual_v1` (SECURITY DEFINER) | EXECUTE (owner) | EXECUTE (grant explícito) | negado | **negado (V27 revoga o grant da V24)** |
| Demais RPCs de negócio (reservar/consumir/liberar/anular/renovar/compensar/concluir_reembolso/reconciliar) | EXECUTE (owner) | negado (sem necessidade hoje) | negado | negado |
| `flyway_schema_history` | tudo | leitura via grants padrão | nada | nada |

Perfis funcionais dentro das RPCs:

| RPC | Exigência de perfil (validada na função) |
|---|---|
| `desvincular_os_contratual_v1` | ADMIN ativo |
| `anular_os_concluida_v1` | ADMIN ativo |
| `compensar_lancamento_v1` / `concluir_reembolso_v1` | ADMIN ou FINANCEIRO ativo |
| `renovar_contrato_v1` | usuário registrado como `criado_por` (auditoria) |
| `rpc_faturar_mensalidades_v1` | ator opcional auditado; origem restrita a DESKTOP/EDGE_FUNCTION/PG_CRON/MANUAL |

## Decisões de segurança

- **Identidade não confiável do cliente**: `anon`/`authenticated` não têm
  EXECUTE em nenhuma RPC administrativa — o `p_usuario_id` só pode ser
  informado pelos caminhos técnicos (desktop como `postgres`, jobs como
  `service_role`), e ainda assim é validado contra `usuarios.tipo/ativo`.
- **`search_path` fixo** em todas as funções `public` (inclui as SECURITY
  DEFINER), aplicado dinamicamente pela V27.
- **Owner sem login para SECURITY DEFINER**: adiado deliberadamente. Só
  existe uma função SECURITY DEFINER (`desvincular_os_contratual_v1`) e
  nenhum papel não-bypass tem EXECUTE nela; trocar o owner hoje exigiria
  policies RLS para o novo owner sem ganho de segurança prático. Reavaliar
  se algum dia `authenticated` ganhar EXECUTE em função DEFINER.
- **`service_role` mantém acesso a tabelas**: a Edge Function de notificações
  lê/escreve tabelas sem RPC correspondente (fluxo de notificações). O
  faturamento — que tem RPC — passou a usar exclusivamente a RPC.
- **Pendência herdada (crítica, fora desta fase)**: rotacionar as chaves
  `anon`/`service_role` e a senha do banco, pois RLS não limita
  `service_role`.
