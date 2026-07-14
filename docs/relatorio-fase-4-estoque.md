# Relatório da Fase 4 — estoque fracionado, atômico, imutável e reconciliável

Data: 2026-07-13. Branch: `homolog`. Pré-condição: Fase 3 finalizada e validada
(ver `relatorio-fase-3-desvinculacao-os.md`).

## 1. Arquitetura implementada

A razão de estoque (`estoque_movimentacoes`) passou a ser a **única autoridade
de saldo**:

- **Razão imutável**: trigger bloqueia `UPDATE`/`DELETE` na tabela
  (`SQLSTATE 23514`); correção só por movimento compensatório novo.
- **Saldo materializado por trigger**: todo `INSERT` na razão aplica o delta em
  `produtos.quantidade_atual_decimal`/`quantidade_atual` sob o lock de linha do
  próprio UPDATE; `CHECK` de não negatividade impede que dois consumidores
  levem o último saldo (atomicidade garantida no PostgreSQL, não no Java).
- **Classificação dos tipos** (`fn_estoque_delta_v1`): ENTRADA/INBOUND/COMPRA/
  DEVOLUCAO_RESERVA_OS/ESTORNO_CONSUMO_OS aumentam; SAIDA/OUTBOUND/USO_OS/
  AJUSTE/RESERVA_OS diminuem; **CONSUMO_RESERVA_OS é neutro** (converte reserva
  já baixada — sem segunda baixa). Idêntica à semântica do domínio Java
  (`ItemEstoque.MovementType`).
- **Baseline de legado** (`estoque_saldos_baseline`): para cada produto,
  `baseline = saldo materializado na migração − soma da razão pré-existente`;
  produtos novos ganham baseline no INSERT (trigger). Invariante permanente:
  `saldo = baseline + soma(razão)` — sem inventar movimentos para o legado.
- **Reconciliação**: `vw_estoque_reconciliacao_v1` e `reconciliar_estoque_v1()`
  comparam saldo materializado × (baseline + razão) e listam divergências e
  movimentos de tipo desconhecido.
- **RPCs reescritas** (`reservar_estoque_os_v1`, `liberar_reserva_os_v1`):
  mantêm validações/lock/idempotência, mas não regravam mais o saldo — o
  trigger da razão aplica. `consumir_reserva_os_v1` inalterada.
- **Anulação de OS concluída** (`anular_os_concluida_v1`): exige ADMIN ativo
  (mesma regra da Fase 3), idempotente por chave, gera `ESTORNO_CONSUMO_OS`
  com `movimento_compensado_id` apontando o consumo original, marca
  `ordem_servico_produtos` como `COMPENSADA`, grava `historico_status`
  (evento ANULACAO), `os_anulacoes` e auditoria. Efeitos financeiros da
  anulação ficam para a Fase 5 (compensações).

## 2. Migration nova (aditiva)

`V25__estoque_fracionado_razao_imutavel.sql` — backfill idempotente
(`COALESCE`) das colunas decimais de `produtos`, `estoque_movimentacoes` e
`ordem_servico_produtos`; CHECKs de escala ≤ 4 e saldo decimal ≥ 0 (`NOT
VALID`: valida só escritas novas, sem travar legado); baseline; triggers;
funções. Nenhuma migration existente foi editada nesta fase. As colunas
legadas de quantidade já eram `numeric` — nenhum estreitamento de tipo.

## 3. Conversão BigDecimal ponta a ponta (sem int/double/summingInt)

- **Domínio**: `ItemEstoque` (quantity, minimumQuantity, amount) e
  `OrdemServico.ProdutoUsado.quantidade` agora `BigDecimal`, com validação de
  escala ≤ 4 (`ESCALA_QUANTIDADE`).
- **Portas**: `RegisterItemEstoqueCommand`, `RecordInventoryMovementCommand`,
  `InventoryMovementView`, `AdicionarProdutoOrdemCommand` → `BigDecimal`.
- **Casos de uso**: `CasoDeUsoGerenciarEstoque` e
  `CasoDeUsoGerenciarOrdemServico` sem `summingInt`/`mapToInt` no fluxo de
  estoque (reduções `BigDecimal::add`, comparações `compareTo`).
- **Persistência**: `ItemEstoqueEntidade` perdeu a element-collection do
  Hibernate (que apagava e regravava a razão a cada save — incompatível com
  razão imutável); movimentos agora são `EstoqueMovimentacaoEntidade`
  (append-only, com `chave_idempotencia` = id do movimento). `save(produto)`
  virou UPDATE cadastral que **não toca saldo** (elimina o
  read-modify-write não atômico); INSERT só para produto novo (saldo inicial).
- **Interface**: `ValidadorEntrada.quantidadePositiva/quantidadeNaoNegativa`
  (aceita vírgula, escala ≤ 4, mensagens agregadas), `FormatadorQuantidade`
  (exibição sem zeros à direita), telas de estoque/produto/movimentação/OS.

## 4. Transação única estoque + financeiro

`CasoDeUsoGerenciarEstoque.recordMovement` agora é `@Transactional`: o
movimento (razão + saldo via trigger) e a despesa (`registerExpense`) ocorrem
na mesma transação PostgreSQL; falha em qualquer um desfaz os dois.
`start()` da OS também virou `@Transactional` (reserva + persistência juntas).
Regra financeira preservada e testada: **compra/entrada gera exatamente uma
despesa; reserva, consumo, devolução e estorno não geram despesa de aquisição**.

## 5. Testes realmente executados

| Comando | Resultado |
|---|---|
| `./mvnw.cmd test` (e `clean test`) | BUILD SUCCESS — 173 testes de unidade (19+94+18+2+40), 0 falhas |
| `./mvnw.cmd -pl sigla-infraestrutura -am verify` com `SIGLA_IT_JDBC_URL` | BUILD SUCCESS — Flyway **V1→V25 aplicado do zero** em PostgreSQL 17.5 real; `FundacaoSchemaPostgreSQLIT`: **22 testes, 0 falhas, 0 ignorados** |
| `git diff --check` | limpo |

Cenários de integração novos (PostgreSQL 17.5 real, `localhost:54329`):

1. `razaoDeEstoqueEImutavel` — UPDATE/DELETE na razão → `23514`.
2. `triggerMaterializaSaldoENaoPermiteFicarNegativo` — entradas/saídas
   fracionadas materializam saldo; saída de 0,0001 sem saldo → `23514`.
3. `movimentacaoManualConcorrenteNaoDisputaOMesmoUltimoSaldo` — duas
   transações disputando a última unidade: exatamente uma vence, saldo 0.
4. `reservaFracionadaDeQuatroCasasMantemPrecisaoEReconciliacao` — reserva e
   consumo de **0,0001** com saldo exato 0,0004 e reconciliação sem divergência.
5. `movimentoDeEstoqueEFinanceiroCompartilhamTransacaoUnica` — rollback da
   transação desfaz movimento, saldo e lançamento.
6. `anulacaoDeOsConcluidaCompensaEstoqueSomenteAdminAtivoEIdempotente` —
   OPERADOR rejeitado; ADMIN ativo compensa consumo (saldo volta a 10) com
   `movimento_compensado_id`; retry pela mesma chave não duplica; segunda
   anulação por outra chave → `23514`.
7. `reconciliacaoDetectaSaldoMaterializadoForaDaRazao` — ajuste de saldo por
   fora da razão é detectado com a divergência exata.

Cenários pré-existentes revalidados com a nova arquitetura: reserva
concorrente do último item via RPC (`reservaConcorrenteNaoPermiteSaldoNegativo`),
consumo repetido sem segunda baixa e idempotência
(`consumoRepetidoNaoFazSegundaBaixaELiberacaoEIdempotente`), e toda a Fase 3.
Unidade: fração 0,0001 no domínio, rejeição de escala > 4, propagação da falha
do financeiro em `recordMovement` (pré-condição do rollback do Spring), e
despesa única por compra/saída sem despesa para reserva/consumo/devolução.

## 6. Fora de escopo desta fase (limites do plano, §5.4)

- Faturamento mensal por RPC (`rpc_faturar_mensalidades_v1`) → Fase 5.
- RLS completo e deploy Supabase → Fase 6.
- Interface financeira final → Fase 5/6.
- Compensação **financeira** da anulação de OS → Fase 5.

## 7. Pendências e observações

- V18–V25 continuam não publicadas em produção (produção está em V17).
- Teste de upgrade/backfill sobre cópia efêmea de produção está reservado à
  Fase 5 (§6.5 do plano); o backfill da V25 é `COALESCE`-idempotente e foi
  validado em esquema completo do zero.
- O rollback Java estoque↔financeiro apoia-se em `@Transactional` sobre o
  mesmo datasource; a atomicidade equivalente foi comprovada no nível do banco
  (cenário 5). Um IT com contexto Spring completo fica como melhoria futura.
- `ordem_servico_produtos` continua regravada pela element-collection da OS
  (estado `estado_estoque` é mantido pelas RPCs/anulação); a razão imutável e
  o saldo NÃO dependem dessa tabela.
