# Relatório da Fase 5 — financeiro e contratos

Data: 2026-07-14. Branch: `homolog`. Pré-condições atendidas: Fases 3 e 4
finalizadas e validadas (ver `relatorio-fase-3-desvinculacao-os.md` e
`relatorio-fase-4-estoque.md`). Nenhuma migration existente foi reescrita;
nada foi aplicado em produção (produção permanece em V17).

## 1. Migration nova (aditiva): V26__financeiro_contratos_fase5.sql

### Vigências como fonte de verdade
- Backfill estrutural: contrato sem vigência ganha a vigência `INICIAL`
  espelhando os próprios campos do contrato (sem inferência textual).
- `renovar_contrato_v1(...)`: renovação MANUAL/AUTOMATICA idempotente por
  chave; nova vigência versionada com `vigencia_anterior_id`; sobreposição
  bloqueada pela exclusion constraint (23P01); `contratos.data_fim` vira
  espelho de leitura; auditoria.
- Trigger `vincular_vigencia_os`: OS contratual referencia a vigência que
  cobre a `data_agendada` (mensalidades já referenciam a vigência pela RPC).

### Regra explícita de cobrança da OS contratual
- Coluna `ordens_servico.regra_cobranca` (`COBERTA_PELO_CONTRATO` |
  `COBRAR_EXTRA`) + CHECK; trigger exige a regra em toda OS contratual nova
  ou ao vincular contrato (23514).
- Backfill somente com evidência estrutural: contratual com conta a receber
  ativa → `COBRAR_EXTRA`; contratual concluída sem conta → `COBERTA`;
  demais legadas ficam NULL e aparecem em `vw_os_contratuais_sem_regra_v1`
  (relatório de conciliação — sem gerar/cancelar lançamento automaticamente).

### Origem formal do financeiro
- Trigger `definir_origem_lancamento`: todo lançamento novo recebe
  `origem_tipo` (`ORDEM_SERVICO`/`CONTRATO`/`MANUAL`; `COMPENSACAO` vem das
  RPCs), `origem_id`, e herda contrato/vigência da OS vinculada.
- Unicidade de cobrança automática por OS: trigger bloqueia segunda conta a
  receber ativa da mesma OS (23505) — legado não é tocado, aparece no script
  de diagnóstico.
- Unicidade de mensalidade: índice único parcial (contrato, competência)
  para linhas com origem formal + chave determinística
  `MENSALIDADE:<contrato>:<AAAA-MM>`.
- Índices de consulta por vigência, origem e OS.

### Estados financeiros: fatos × projeções
- Fatos persistidos: `PENDING`, `PARTIAL`, `PAID`, `CANCELLED`. `OVERDUE`
  persistido legado vira `PENDING` (backfill); trigger impede persistir
  `OVERDUE` novo (projeção por relógio em `America/Sao_Paulo`, já existente
  em `vw_financeiro_lancamentos_v1`).
- Trigger proíbe `PAID`/`PARTIAL` → `CANCELLED` (23514); reversão só por
  estorno/crédito/reembolso.

### Estorno, crédito e reembolso
- `compensar_lancamento_v1(...)`: exige ADMIN/FINANCEIRO ativo; idempotente
  por chave; nunca supera o valor liquidado menos o já compensado;
  ESTORNO gera lançamento compensatório espelho (`origem COMPENSACAO`,
  vinculado ao original); CRÉDITO movimenta a razão
  `cliente_credito_movimentos` (nunca saldo editável); REEMBOLSO nasce
  `SOLICITADA` e conclui por `concluir_reembolso_v1` (`CONFIRMADA`/`FALHOU`);
  crédito × reembolso mutuamente exclusivos salvo divisão explícita e
  auditada (`p_autorizar_divisao`). Auditoria em todas as operações.

### Autoridade única de mensalidade
- `rpc_faturar_mensalidades_v1` efetiva (mesma assinatura da V21): valida
  contrato ativo + vigência cobrindo a data, competência dentro da vigência,
  vencimento pelo dia de início da vigência, insere com chave idempotente e
  trata colisão de qualquer índice único como `JA_EXISTENTE` (dois
  disparadores concorrentes ⇒ 1 linha). Execuções registradas em
  `sigla_faturamento_execucoes`. Feature flag `faturamento_rpc_v1` (ON) gate
  da RPC; `mensalidade_produtor_java` registrada como OFF.

## 2. Mudanças Java

- **Produtor paralelo removido**: `CasoDeUsoGerenciarFaturamentoContrato`
  agora só delega à RPC via `PortaFaturamentoMensalidades` (adapter nativo +
  variante `@Profile("memoria")` no-op), com chave
  `FATURAMENTO:DESKTOP:<data-SP>`. `gerarMensalidadeContrato` no financeiro
  lança `IllegalStateException` — o desktop não grava mensalidade.
- **Regra de cobrança ponta a ponta**: `OrdemServico.regraCobranca`
  (enum, 22º componente com overloads compatíveis), commands de criar/editar,
  validação no caso de uso (contratual sem regra é rejeitada), entidade e
  adaptador persistem `regra_cobranca`; materialização de visitas contratuais
  declara `COBERTA_PELO_CONTRATO` explicitamente; a tela de nova OS pergunta
  a regra ao vincular contrato (diálogo em PT-BR).
- **Conclusão de OS**: coberta não gera conta própria; `COBRAR_EXTRA` gera no
  máximo uma cobrança (unicidade no banco) herdando contrato/vigência;
  contratual legada sem regra não fatura e fica na conciliação.
- **Guardas financeiras**: `cancel()` rejeita lançamento pago/parcial
  (estorno/crédito/reembolso primeiro); `statusPorParcelas` não persiste mais
  `OVERDUE` (fato `PARTIAL`/`PENDING`; vencido é projeção). O cancelamento
  prospectivo de contrato (`cancelarLancamentosPendentesDoContrato`) continua
  preservando pago/parcial/vencido e cancelando apenas pendente futuro
  (teste `encerramentoCancelaMensalidadeFutura...`).

## 3. Testes realmente executados (§6.5)

| Comando | Resultado |
|---|---|
| `./mvnw.cmd test` | BUILD SUCCESS — 172 testes de unidade (19+93+18+2+40), 0 falhas |
| `./mvnw.cmd -pl sigla-infraestrutura -am verify` com `SIGLA_IT_JDBC_URL` | BUILD SUCCESS — Flyway **V1→V26 do zero** em PostgreSQL 17.5 real; `FundacaoSchemaPostgreSQLIT`: **30 testes, 0 falhas, 0 ignorados** |
| `git diff --check` | limpo |

Cenários de integração novos (PostgreSQL real):

1. Mensalidade criada uma única vez por competência; retry com outra chave
   → `JA_EXISTENTE`; vencimento e vigência corretos; fora da vigência não
   fatura.
2. **Dois disparadores concorrentes → uma única linha** (threads reais).
3. Renovação idempotente pela chave, sem vigência sobreposta (23P01), sem
   mensalidade/vigência duplicada, contrato espelhando o novo fim.
4. OS contratual nova sem regra → 23514; com `COBRAR_EXTRA` recebe vigência.
5. Cobrança extra única por OS (23505 na segunda) herdando contrato/vigência.
6. `OVERDUE` persistido rejeitado; `PAID` → `CANCELLED` rejeitado.
7. Compensações: OPERADOR rejeitado; estorno idempotente com lançamento
   espelho; crédito na razão do cliente; crédito×reembolso exclusivos sem
   divisão autorizada; reembolso `SOLICITADA`→`CONFIRMADA`; teto do valor
   liquidado (23514).
8. **Upgrade em cópia efêmera**: banco descartável criado, migrado até V17
   (estado da produção), semeado com legado (contrato sem vigência, OS
   contratual ambígua e concluída, `OVERDUE` persistido, estoque sem colunas
   decimais), migrado até V26 — backfills conferidos (vigência inicial,
   `COBERTA` por evidência, conciliação da ambígua, `PENDING`, decimal,
   baseline sem divergência) — e a cópia descartada ao final.

Unidade: delegação do faturamento à RPC com chave idempotente; rejeição de
mensalidade direta; cancelamento de pago rejeitado + fluxo estorno→cancelar;
status factual sem `OVERDUE`; suíte inteira das Fases 3–4 revalidada.

## 4. Scripts versionados

- `scripts/fase5-diagnostico-financeiro-contratos.sql` — contratos sem
  vigência, OS contratuais sem regra (conciliação), lançamentos sem origem,
  duplicatas legadas por OS e por competência (triagem humana), reembolsos
  pendentes, execuções do faturamento.
- `scripts/fase5-reconciliacao.sql` — invariantes: estoque (baseline+razão),
  mensalidade única, competência dentro da vigência, compensações ≤
  liquidado, crédito como razão, ausência de `OVERDUE` persistido.

## 5. Dados ambíguos e como são tratados

- OS contratual legada pendente sem cobrança: **não classificada** — vai para
  `vw_os_contratuais_sem_regra_v1`; nada é gerado/cancelado automaticamente.
- Lançamentos legados sem origem formal e possíveis duplicatas antigas por
  OS/competência: listados no diagnóstico para decisão humana (compensação),
  nunca `DELETE`.

## 6. Reservado explicitamente para a Fase 6

- RLS global com negação por padrão, matriz de acessos por perfil e
  `GRANT EXECUTE` mínimo nas RPCs (hoje: `REVOKE ALL FROM PUBLIC` na RPC de
  faturamento; app conecta como `postgres`).
- Deploy/atualização da Edge Function `notificacoes-agendador` como
  disparadora da RPC (dry-run, logs, idempotência) e `pg_cron`.
- Publicação de V18–V26 em homologação Supabase e cutover em produção
  (com aprovação explícita).
- Auditoria completa de execução do faturamento (ator/origem do disparo).
- Adoção das RPCs de compensação/renovação pela interface desktop
  (interface financeira final).
- Anulação de OS concluída: efeitos financeiros via compensação (o estoque
  já compensa desde a Fase 4).

## 7. Observações

- `var/os_java.pdf` e `var/visita_java.pdf` mudam a cada build (efeito
  colateral pré-existente do teste de formulários sobre arquivos versionados).
- Diretórios `target/` versionados continuam sujando o working tree
  (sugerida tarefa separada de `.gitignore`).

## 8. Ponto de parada (§6.7 do plano)

**Parado para revisão.** Não foram iniciados: RLS, deploy Supabase, `pg_cron`
ou cutover. A Fase 6 só começa com aprovação explícita.
