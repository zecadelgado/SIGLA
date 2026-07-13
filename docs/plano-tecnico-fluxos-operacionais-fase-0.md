# Plano técnico — contratos, agenda, OS, estoque e financeiro

**Fase:** 0 — auditoria técnica  
**Data:** 12/07/2026  
**Escopo desta fase:** leitura do estado atual, desenho proposto e plano de validação. Nenhuma regra de negócio ou migration foi alterada por esta auditoria.

## 1. Resumo executivo

O sistema já contém os cinco módulos, mas o fluxo transversal ainda não possui uma única fonte de verdade nem uma fronteira transacional clara. Hoje:

- contrato gera eventos recorrentes de agenda e mensalidades financeiras;
- agenda pode criar/atualizar OS, e OS também cria/atualiza agenda;
- conclusão/cancelamento de OS movimenta estoque e cria/cancela financeiro;
- pagamento financeiro atualiza o indicador de pagamento da OS;
- faturamento mensal de contrato existe no Java e também na Edge Function Supabase.

Isso cria risco de recursão ou gravações duplicadas, efeitos parciais quando uma etapa falha, concorrência sobre saldo de estoque e divergência entre execução desktop e execução agendada. A proposta para as próximas fases é manter entidades de negócio separadas, escolher a OS como agregado operacional, usar vínculos explícitos e idempotentes e executar cada comando transversal em uma transação PostgreSQL.

> Estado do checkout: já havia alterações locais anteriores em classes dos cinco fluxos, em `V1__create_sigla_schema.sql`, uma `V18__financeiro_lancamentos_contrato.sql` não rastreada e mudanças na Edge Function. Este documento descreve o diretório de trabalho encontrado; não atribui essas alterações à Fase 0.

## 2. Mapa dos fluxos atuais

### 2.1 Contrato

1. A interface `ControladorContratosCertificados` cria/edita contratos pelo `CasoDeUsoGerenciarContrato`.
2. O contrato persiste em `contratos`, ligado a `cadastro` por `cliente_id`.
3. Criação e atualização chamam sincronização de agenda; renovação reativa o contrato e recria eventos.
4. `CasoDeUsoGerenciarFaturamentoContrato` e `CasoDeUsoGerenciarFinanceiro.gerarMensalidadeContrato` geram mensalidade com chave determinística por contrato/competência.
5. `supabase/functions/notificacoes-agendador/index.ts` também fatura mensalidades e gera alertas de vencimento.

Problemas observados: produtor duplicado de mensalidade; recorrência derivada de enumerações limitadas; `status`, datas e regra de renovação não estão protegidos por transições no banco; o vínculo `financeiro_lancamentos.contrato_id` depende da migration local V18.

### 2.2 Agenda

1. `ControladorAgenda` agenda, edita, reagenda, conclui e cancela `VisitaAgendada`.
2. O caso de uso valida conflito em memória por responsável e intervalo.
3. Eventos operacionais são persistidos em `agenda_eventos`; eventos de contrato/certificado compartilham a mesma tabela.
4. Ao salvar evento operacional, `CasoDeUsoGerenciarAgenda` cria ou atualiza uma OS e traduz status da visita para status da OS.
5. Em sentido inverso, alterações de OS chamam `sincronizarAgenda`.

Problemas observados: propriedade bidirecional da sincronização; não há exclusão/constraint PostgreSQL contra sobreposição; recorrências são expandidas em memória, sem ocorrência materializada; `orderId` é opcional no domínio embora a relação operacional deva ser inequívoca.

### 2.3 Ordem de serviço

1. `ControladorNovaOrdemServico` cria a OS como `AGENDADA`; `ControladorOrdemServico` edita, inicia, conclui, cancela, adiciona produto e anexo.
2. O caso de uso impede alteração livre de status e impede edição de OS concluída/cancelada.
3. Início grava `EM_ANDAMENTO` e `data_inicio`.
4. Conclusão serializável grava `CONCLUIDA`, consome/reserva estoque conforme o fluxo encontrado, gera entrada financeira quando possível e sincroniza agenda.
5. Cancelamento serializável grava `CANCELADA`, devolve/estorna estoque, cancela lançamento financeiro associado e sincroniza agenda.
6. Financeiro pago/não pago atualiza `ordens_servico.pagamento_confirmado`.

Problemas observados: efeitos externos são coordenados diretamente pelo caso de uso, sem registro explícito de quais efeitos já ocorreram; `ABERTA` e `ATRASADA` existem, mas não há comandos/transições completos para todos os estados; vínculo de produto em `ordem_servico_produtos` não registra estado da reserva/consumo; valor financeiro pode depender de referências opcionais.

### 2.4 Estoque

1. `ControladorEstoque`, `ControladorNovoProduto` e `ControladorNovaMovimentacao` operam produto e movimentações.
2. `ItemEstoque` mantém quantidade agregada e lista de movimentos; entrada/compra/devolução aumentam e saída/uso/ajuste/reserva reduzem.
3. Movimentos persistem em `estoque_movimentacoes`, com referências opcionais a produto, OS, cliente, usuário e funcionário.
4. A OS usa tipos especiais (`RESERVA_OS`, `CONSUMO_RESERVA_OS`, `DEVOLUCAO_RESERVA_OS`, `ESTORNO_CONSUMO_OS`).

Problemas observados: `CONSUMO_RESERVA_OS` não altera quantidade, logo depende de reserva anterior correta; saldo é também armazenado em `produtos.quantidade_atual`; não há chave idempotente ou vínculo único por OS/produto/efeito; validação de saldo ocorre na aplicação e está sujeita a corrida entre desktops.

### 2.5 Financeiro

1. `ControladorFinanceiro` e `ControladorNovaTransacao` registram, filtram, pagam, cancelam e parcelam lançamentos.
2. O modelo principal usa `financeiro_lancamentos` e `financeiro_parcelas`, mas ainda coexistem portas/modelos legados de entrada, despesa e plano de parcelamento.
3. Conclusão de OS gera conta a receber; cancelamento da OS cancela o lançamento; pagamento sincroniza `pagamento_confirmado` da OS.
4. Contratos geram lançamentos mensais pelo desktop e pela Edge Function.

Problemas observados: dois modelos financeiros coexistentes; status de lançamento e parcela podem divergir; não há chave de origem formal no schema (a idempotência usa UUID/texto determinístico); `OVERDUE` pode ser estado persistido ou projeção por data, sem decisão única; produtores concorrentes de mensalidade.

## 3. Arquivos e classes afetados nas próximas fases

| Camada | Arquivos/classes principais |
|---|---|
| Domínio | `Contrato`, `VisitaAgendada`, `OrdemServico`, `ItemEstoque`, `LancamentoFinanceiro`, `EntradaFinanceira`, `DespesaFinanceira`, `PlanoParcelamento` |
| Aplicação | `CasoDeUsoGerenciarContrato`, `CasoDeUsoGerenciarFaturamentoContrato`, `CasoDeUsoGerenciarAgenda`, `CasoDeUsoGerenciarOrdemServico`, `CasoDeUsoGerenciarEstoque`, `CasoDeUsoGerenciarFinanceiro` e respectivas portas de entrada/saída |
| Persistência | `ContratoEntidade`, `VisitaAgendadaEntidade`, `OrdemServicoEntidade`, `ItemEstoqueEntidade`, `FinanceiroLancamentoEntidade`, `FinanceiroParcelaEntidade` e adaptadores dos cinco módulos |
| Interface | `ControladorContratosCertificados`, `ControladorAgenda`, `ControladorNovaOrdemServico`, `ControladorOrdemServico`, `ControladorServicos`, `ControladorEstoque`, `ControladorNovoProduto`, `ControladorNovaMovimentacao`, `ControladorFinanceiro`, `ControladorNovaTransacao`; FXML correspondentes |
| Banco | Flyway `V1`, `V2`, `V6`, `V7`, `V8`, `V12`, `V13`, `V15`, `V17`, V18 local e novas migrations aditivas futuras |
| Supabase | `supabase/functions/notificacoes-agendador/index.ts` e seu README; tabelas `notificacao_configuracoes`, `notificacoes`, `notificacao_execucao_log` |

Tabelas diretamente envolvidas: `contratos`, `agenda_eventos`, `ordens_servico`, `ordem_servico_produtos`, `ordem_servico_anexos`, `produtos`, `estoque_movimentacoes`, `financeiro_categorias`, `financeiro_formas_pagamento`, `financeiro_lancamentos`, `financeiro_parcelas`, `auditoria_eventos`. Tabelas de referência: `cadastro`, `usuarios`, `certificados`.

## 4. Modelo de dados proposto

O desenho deve ser implementado apenas por migrations novas; migrations já aplicadas não devem ser reescritas.

### 4.1 Vínculos e idempotência

- `agenda_eventos.ordem_servico_id`: manter FK e criar unicidade parcial para evento operacional ativo vinculado a uma OS. A OS será a fonte de status para eventos operacionais; agenda apenas solicita comandos da OS.
- `financeiro_lancamentos.contrato_id`: FK indexada, sem depender de texto em `observacoes`.
- `financeiro_lancamentos.origem_tipo`, `origem_id`, `competencia`: origem formal (`OS`, `CONTRATO`, `MANUAL`) e índice único parcial por origem/competência.
- `estoque_movimentacoes.chave_idempotencia`: chave única gerada por `os_id + produto_id + efeito + versão`, impedindo reserva/consumo/estorno duplicados.
- `ordem_servico_produtos`: acrescentar `quantidade_reservada`, `quantidade_consumida`, `estado_estoque` e versão otimista, ou substituir por uma entidade de alocação explícita. Quantidade deve usar tipo numérico coerente entre OS e estoque; hoje há `BigDecimal` na OS e `int` no estoque.
- `ordens_servico.versao`, `agenda_eventos.versao`, `financeiro_lancamentos.versao`: bloqueio otimista para concorrência de desktops.
- `historico_status`: tabela única auditável (`entidade_tipo`, `entidade_id`, `status_anterior`, `status_novo`, `motivo`, `usuario_id`, `ocorrido_em`, `chave_comando`) ou tabelas específicas se RLS exigir políticas distintas.

### 4.2 Fonte do saldo e estados derivados

- Definir `estoque_movimentacoes` como razão imutável; `produtos.quantidade_atual` pode permanecer como cache atualizado atomicamente, reconciliável pela soma dos movimentos.
- Status `OVERDUE` financeiro e `ATRASADA` da OS devem ser projeções calculadas por data, salvo se houver requisito de auditoria para persistir a transição.
- Status agregado do lançamento parcelado deve ser derivado das parcelas: todas pagas = `PAID`; parte paga = `PARTIAL`; nenhuma paga e vencida = `OVERDUE`; cancelamento explícito = `CANCELLED`.

### 4.3 Fronteira transacional

Um serviço de aplicação/orquestrador deve executar, na mesma transação PostgreSQL, a mudança de OS, os movimentos de estoque, a criação/cancelamento financeiro, a atualização do evento e o histórico. A Edge Function deve chamar uma função/RPC idempotente no banco ou ser a única produtora do job que lhe for atribuído; não deve repetir regras SQL paralelas às regras Java.

## 5. Regras propostas de transição de status

Estas regras são proposta para decisão, não alteração realizada.

### Contrato

| Origem | Destino | Condição/efeito |
|---|---|---|
| `DRAFT` | `ACTIVE` | cliente, vigência, frequência e valor válidos; agenda inicial criada idempotentemente |
| `DRAFT` | `CANCELLED` | motivo obrigatório; nenhum faturamento |
| `ACTIVE` | `EXPIRED` | fim da vigência ultrapassado; preferencialmente projeção/job único |
| `ACTIVE` | `CANCELLED` | motivo obrigatório; cancelar apenas competências futuras ainda não devidas |
| `EXPIRED` | `ACTIVE` | renovação explícita com nova vigência |
| `ACTIVE` | `ACTIVE` | renovação automática cria nova vigência/histórico, sem duplicar competência |

### Agenda

| Origem | Destino | Condição/efeito |
|---|---|---|
| `SCHEDULED` | `IN_PROGRESS` | para evento operacional, delegar `iniciar OS` |
| `SCHEDULED` | `CANCELLED` | motivo obrigatório; delegar cancelamento da OS quando vinculada |
| `SCHEDULED` | `MISSED` | horário expirado sem início; decisão sobre projeção versus persistência |
| `IN_PROGRESS` | `COMPLETED` | delegar conclusão da OS |
| `IN_PROGRESS` | `CANCELLED` | motivo obrigatório e política de estoque/financeiro definida |
| estados finais | qualquer outro | proibido, salvo comando administrativo auditado de reabertura |

### Ordem de serviço

| Origem | Destino | Condição/efeito |
|---|---|---|
| criação | `AGENDADA` | agenda vinculada criada/atualizada |
| `AGENDADA`/`ABERTA`/`ATRASADA` | `EM_ANDAMENTO` | responsável e data de início; reservar/confirmar alocação conforme decisão |
| `EM_ANDAMENTO` | `CONCLUIDA` | consumo idempotente do estoque, lançamento financeiro idempotente e agenda concluída |
| não final | `CANCELADA` | motivo; devolver reserva ou estornar consumo conforme estado; cancelar financeiro não liquidado |
| `CONCLUIDA`/`CANCELADA` | outro | proibido, salvo reabertura administrativa auditada e compensações explícitas |

### Estoque

Movimentação não muda de status: é imutável. Correção ocorre por movimento compensatório. Reserva reduz disponível, consumo converte reserva sem reduzir novamente, devolução repõe reserva, estorno de consumo repõe saldo. Cada efeito exige chave idempotente e trava atômica de saldo.

### Financeiro

| Origem | Destino | Condição/efeito |
|---|---|---|
| criação | `PENDING` | origem e vencimento válidos |
| `PENDING`/`OVERDUE` | `PARTIAL` | pagamento parcial/parcela paga |
| `PENDING`/`OVERDUE`/`PARTIAL` | `PAID` | valor integral liquidado; sincronizar OS |
| `PENDING`/`OVERDUE` | `CANCELLED` | motivo; proibido se houver pagamento sem estorno prévio |
| `PAID` | `CANCELLED` | proibido; usar estorno/lançamento compensatório |

## 6. Registro das decisões originalmente pendentes

> Este inventário pertence à Fase 0. As decisões solicitadas para a Fase 1.1 foram encerradas na seção 11, que prevalece em caso de divergência. Os itens 1 e 8 já foram fechados na Fase 1 (seção 10).

1. Qual é a fonte de verdade do evento operacional: OS (recomendado) ou agenda?
2. Reserva de estoque ocorre ao criar, ao iniciar ou somente ao concluir a OS?
3. Quantidades fracionárias são permitidas? O modelo atual mistura `BigDecimal` e `int`.
4. Cancelar OS concluída será proibido ou haverá estorno formal de estoque e financeiro?
5. Quem fatura contrato: desktop, Edge Function ou função PostgreSQL chamada por ambos? Recomenda-se um único produtor/RPC idempotente.
6. `OVERDUE`, `ATRASADA`, `EXPIRED` e `MISSED` serão persistidos ou calculados?
7. Renovação automática estende o mesmo contrato ou cria uma nova versão/vigência?
8. Uma OS de contrato gera cobrança própria além da mensalidade, ou a mensalidade já cobre todas as OS?
9. Como tratar lançamento pago quando contrato/OS é cancelado: crédito, estorno ou manutenção histórica?
10. RLS será ativado para tabelas de negócio? Hoje V9 não habilita RLS nelas e apenas `usuarios` tem RLS explícita.
11. A Edge Function pode acessar tabelas diretamente via credencial de serviço ou deve usar RPCs com contratos estáveis?
12. Qual timezone oficial para competência, vencimento e virada de status (`America/Sao_Paulo` recomendado para regra local)?

## 7. Estratégia de migração de dados

1. **Congelar baseline:** não editar V1–V18 já distribuídas; identificar checksums aplicados em homologação/produção e transformar diferenças locais em migrations aditivas.
2. **Diagnóstico somente leitura:** contagens, nulos, FKs órfãs, duplicidades OS-agenda, mensalidades duplicadas por contrato/competência, lançamentos por OS, produtos com saldo divergente e movimentos repetidos.
3. **Adicionar estrutura nullable:** novas colunas de origem, competência, idempotência, versão e histórico sem bloquear a aplicação antiga.
4. **Backfill determinístico:** inferir `contrato_id` primeiro por FK existente e, apenas como fallback controlado, por marcador legado em observações; associar evento/OS; gerar chaves de movimentos; registrar linhas não inferíveis em tabela/arquivo de exceções.
5. **Reconciliar:** comparar saldo materializado com razão, parcelas com agregado, OS com agenda e lançamentos com origem. Nenhum ajuste destrutivo; usar movimentos/lançamentos compensatórios.
6. **Adicionar constraints `NOT VALID`:** FKs, checks e índices únicos parciais; corrigir exceções; executar `VALIDATE CONSTRAINT` depois.
7. **Dupla leitura/compatibilidade curta:** aplicação nova entende colunas novas e legadas; escrita passa a preencher o novo modelo.
8. **Cutover por feature flag:** eleger o único sincronizador/faturador e desativar o produtor antigo.
9. **Rollback:** rollback de código/flag; migrations estruturais permanecem compatíveis. Correções financeiras/estoque são compensatórias, nunca `DELETE` de histórico.

Consultas de diagnóstico e scripts de backfill devem ser versionados separadamente da migration DDL e executados primeiro em clone anonimizado de produção.

## 8. Plano de testes PostgreSQL/Supabase

### 8.1 Camadas

- **Unidade:** matriz completa de transições, idempotência, arredondamento/parcelas, recorrência e compensações.
- **Integração PostgreSQL real:** Testcontainers PostgreSQL na mesma major do Supabase; Flyway do zero e upgrade a partir de snapshot; repositórios reais e transações concorrentes.
- **Contrato de schema:** comparar entidades Spring Data com `information_schema`, FKs, checks, índices, tipos e nullability.
- **Supabase homologação:** aplicar migrations com CLI/Flyway em projeto isolado; validar RLS com `anon`, `authenticated` e `service_role`; testar Edge Function em modo seco e ativo.
- **Fim a fim:** contrato → agenda → OS → reserva/consumo → financeiro → pagamento; repetir cada comando para provar idempotência; simular falha no meio para provar rollback.

### 8.2 Cenários obrigatórios

1. Dois desktops iniciam/concluem a mesma OS simultaneamente: apenas um comando efetivo.
2. Duas OS reservam o último saldo: uma vence, sem saldo negativo.
3. Conclusão repetida não duplica movimento nem lançamento.
4. Cancelamento após reserva devolve uma vez; após consumo gera compensação uma vez.
5. Agenda e OS não entram em ciclo de atualização e terminam com status coerente.
6. Desktop e Edge Function tentam gerar a mesma mensalidade: uma única linha por contrato/competência.
7. Parcela parcial, total, atrasada, cancelada e estornada mantêm agregado correto.
8. Upgrade com dados legados preserva contagens, totais financeiros e saldos.
9. Timezone na virada do dia/mês não antecipa atraso nem duplica competência.
10. RLS impede acesso cruzado indevido e permite o job autorizado.

### 8.3 Critérios de aceite

- migrations aplicam do zero e sobre snapshot sem edição de versões antigas;
- nenhuma FK órfã, saldo negativo ou duplicidade por chave de negócio;
- invariantes permanecem após execução concorrente e retry;
- totais antes/depois conciliados e exceções documentadas;
- logs de auditoria permitem reconstruir cada transição e compensação.

## 9. Testes existentes executados e limitações

Comando executado em 12/07/2026:

```powershell
.\mvnw.cmd test
```

Resultado: **BUILD SUCCESS**, 160 testes, 0 falhas, 0 erros, 0 ignorados, em aproximadamente 17,4 s de Maven (19,6 s de processo). Todos os sete módulos do reactor concluíram com sucesso; o launcher não possui testes.

Limitações verificadas:

- não existe `src/integrationTest` com testes reais, embora infraestrutura configure a pasta;
- não há Testcontainers nem teste contra PostgreSQL/Supabase real;
- Flyway não é aplicado/validado pela suíte;
- repositórios dos fluxos são testados majoritariamente com fakes/in-memory;
- não há teste de concorrência, isolamento, retry ou rollback entre OS, estoque, agenda e financeiro;
- não há teste automatizado da Edge Function nem paridade Java/TypeScript;
- testes Supabase existentes cobrem o adaptador de Auth com HTTP simulado, não banco/RLS/Edge Runtime;
- testes JavaFX validam controladores/recursos, não o fluxo completo com persistência real;
- warnings indicam Mockito self-attach, Java agent dinâmico e acesso nativo JavaFX que exigirão ajuste para JDKs futuros.

## 10. Fase 1 — decisões técnicas e invariantes obrigatórios

**Status:** aprovado como baseline técnico em conjunto com o fechamento da Fase 1.1, em 12/07/2026.  
**Limite desta fase:** somente decisão e documentação. Nenhum código, teste executável, schema ou migration é alterado pela Fase 1.

As decisões abaixo são normativas para as fases seguintes. Quando o estado legado não permitir inferência segura, o dado deve ser preservado e encaminhado para conciliação; não se deve fabricar vínculo, cobertura, cobrança ou movimento retroativo.

### 10.1 OS contratual é operacional e participa de agenda/notificações

**Decisão técnica:** a OS contratual é uma OS operacional completa, não um registro auxiliar do contrato. Ela deve possuir vínculo explícito com o contrato e com a ocorrência contratual que a originou, participar do mesmo ciclo operacional das OS avulsas e ser considerada pela agenda e pelas notificações de execução.

**Regra recomendada:** toda ocorrência contratual materializada deve produzir uma OS identificada como contratual. A OS é a fonte de verdade do estado operacional; a agenda projeta data, responsável, origem e estado da execução, e as notificações usam a ocorrência/OS como destinatário funcional. Mensalidade, vigência e cobrança continuam sob responsabilidade do contrato/financeiro, não da agenda.

**Impacto em dados existentes:** contratos e eventos recorrentes existentes podem não possuir OS ou vínculo inequívoco. O diagnóstico deve separar: ocorrência com OS vinculável, ocorrência sem OS e OS possivelmente contratual sem evidência suficiente. Somente vínculos determinísticos podem ser preenchidos automaticamente; os demais entram em relatório de exceções. Histórico encerrado não deve disparar notificações retroativas.

**Critérios de aceite:**

- cada ocorrência contratual futura ativa pode ser rastreada de contrato para evento e OS, e no sentido inverso;
- iniciar, concluir, reagendar ou cancelar a execução mantém OS e agenda coerentes sem ciclo de sincronização;
- notificações operacionais incluem OS contratuais nas mesmas condições temporais aplicáveis às OS avulsas;
- nenhuma mensalidade é criada pela simples sincronização operacional da agenda.

### 10.2 Cancelamento de contrato afeta somente obrigações futuras pendentes

**Decisão técnica:** cancelar contrato é uma operação prospectiva. Ela não apaga nem cancela fatos realizados, competências vencidas, cobranças pagas, visitas concluídas, OS concluídas ou movimentos de estoque já consumados.

**Regra recomendada:** ao cancelar um contrato, em uma única operação idempotente, cancelar somente: visitas contratuais com início posterior ao instante efetivo do cancelamento e ainda pendentes; suas OS contratuais não iniciadas ou pendentes; e cobranças contratuais futuras ainda pendentes, com competência ou fato gerador posterior ao cancelamento. Itens em andamento, vencidos, parcialmente pagos ou pagos exigem tratamento explícito e não são cancelados automaticamente. O motivo e o instante efetivo do cancelamento são obrigatórios.

**Impacto em dados existentes:** cancelamentos antigos podem ter atingido registros históricos ou deixado obrigações futuras ativas. Deve haver diagnóstico e conciliação, sem reativação, exclusão ou estorno automático. Cobranças sem competência/origem confiável não podem ser canceladas por inferência textual.

**Critérios de aceite:**

- visita, OS e cobrança concluída, vencida, parcialmente paga ou paga permanecem preservadas;
- somente registros futuros pendentes vinculados ao contrato são cancelados;
- repetir o comando de cancelamento não produz novos efeitos;
- auditoria demonstra contrato, motivo, instante efetivo e todos os registros afetados ou deliberadamente preservados.

### 10.3 Cada visita contratual gera uma OS própria

**Decisão técnica:** a cardinalidade é uma ocorrência contratual para uma OS. Uma OS não representa várias visitas contratuais e uma visita contratual não compartilha OS com outra ocorrência.

**Regra recomendada:** ao materializar cada ocorrência da recorrência, gerar uma OS própria com identidade estável e vínculo único à ocorrência e ao contrato. Reagendamento altera a mesma ocorrência/OS; não cria nova OS. Uma nova visita de retorno só cria nova OS quando for uma nova ocorrência de negócio, explicitamente registrada.

**Impacto em dados existentes:** eventos recorrentes hoje expandidos em memória podem não ter identidade persistente, e uma OS pode estar associada de forma ambígua a mais de um horário. O backfill deve vincular apenas pares unívocos. Ocorrências futuras ainda não materializadas devem nascer no novo formato; ambiguidades legadas devem ser listadas para decisão humana.

**Critérios de aceite:**

- existe bijeção entre visita contratual materializada e OS contratual;
- retry da geração da recorrência não cria segunda OS para a mesma ocorrência;
- reagendar preserva os identificadores da ocorrência e da OS;
- relatório de diagnóstico aponta visitas sem OS, OS sem visita e vínculos múltiplos.

### 10.4 Classificação financeira obrigatória da OS contratual

**Decisão técnica:** toda OS contratual deve declarar exatamente uma política de cobrança: `COBERTA_PELO_CONTRATO` ou `COBRAR_EXTRA`. A classificação pertence à OS/ocorrência executada e deve ser auditável.

**Regra recomendada:** `COBERTA_PELO_CONTRATO` não gera conta a receber própria pela conclusão da OS, pois sua receita decorre da cobrança contratual. `COBRAR_EXTRA` exige motivo, valor e regra de vencimento e gera lançamento financeiro adicional idempotente, explicitamente vinculado à OS e ao contrato. A OS não pode ser concluída sem classificação; não se admite valor extra implícito pela presença de produtos ou serviços.

**Impacto em dados existentes:** OS possivelmente contratuais podem ter lançamentos próprios sem indicação de cobertura. Classificação automática só é permitida quando houver evidência estrutural inequívoca; na ausência dela, marcar como pendência de conciliação, sem gerar ou cancelar lançamentos. Lançamentos pagos são preservados.

**Critérios de aceite:**

- nenhuma OS contratual válida permanece sem uma das duas classificações, e ambas são mutuamente exclusivas;
- concluir OS coberta não cria lançamento extra;
- concluir OS extra cria no máximo um lançamento por OS e registra motivo, valor, contrato e origem;
- alteração da classificação após conclusão exige comando administrativo auditado e compensação financeira explícita.

### 10.5 Compra de estoque e consumo em OS não duplicam despesa

**Decisão técnica:** adota-se contabilidade de caixa operacional para este fluxo: a compra/entrada onerosa do estoque reconhece a despesa financeira; o consumo do item na OS reconhece custo operacional e baixa de estoque, mas não cria uma segunda despesa financeira.

**Regra recomendada:** movimento `COMPRA` pode possuir um único lançamento de despesa por documento/operação de compra. `RESERVA_OS`, `CONSUMO_RESERVA_OS`, `USO_OS`, devolução e estorno nunca geram despesa financeira de aquisição. O custo consumido pode compor margem/relatório gerencial e o preço cobrado do cliente, sem ser lançado novamente como saída de caixa. Ajustes manuais que representem perda exigem categoria e autorização próprias.

**Impacto em dados existentes:** pode haver despesas de compra e despesas geradas novamente pelo consumo da OS. O diagnóstico deve procurar duplicidades por produto, OS, valor, data, documento e origem, mas nenhuma exclusão/estorno deve ocorrer automaticamente. Movimentos antigos sem custo/documento continuam históricos e entram em conciliação quando necessário.

**Critérios de aceite:**

- uma compra produz no máximo uma despesa financeira de aquisição, mesmo com retry;
- consumir, devolver ou estornar item de OS não cria nova despesa de compra;
- relatórios distinguem saída de caixa, custo consumido e receita cobrada;
- teste de ponta a ponta compra → estoque → OS demonstra uma saída financeira de aquisição, não duas.

### 10.6 Estoque manual e financeiro são atômicos

**Decisão técnica:** toda movimentação manual com efeito financeiro e seu lançamento correspondente formam um único comando transacional. Não é permitido confirmar apenas um dos lados.

**Regra recomendada:** entradas e saídas manuais devem declarar se possuem efeito financeiro. Quando possuírem, movimento de estoque, atualização do saldo materializado, lançamento financeiro, vínculo de origem e auditoria são gravados na mesma transação PostgreSQL e sob a mesma chave idempotente. Falha em qualquer etapa causa rollback integral. Correções posteriores usam movimentos e lançamentos compensatórios vinculados ao original.

**Impacto em dados existentes:** movimentos manuais e lançamentos atuais podem estar órfãos ou associados apenas por texto/data. O diagnóstico deve classificá-los em pares determinísticos, órfãos e ambíguos. O legado não deve ser unido automaticamente quando houver mais de um candidato nem deve ser apagado para forçar conciliação.

**Critérios de aceite:**

- sucesso grava movimento, saldo, financeiro e auditoria; falha não grava nenhum deles;
- retry com a mesma chave retorna o resultado original sem duplicar saldo ou lançamento;
- concorrência não permite saldo negativo nem lançamentos múltiplos para o mesmo comando;
- é possível navegar do movimento ao lançamento financeiro correspondente e vice-versa.

### 10.7 Financeiro consulta e exibe contrato

**Decisão técnica:** contrato é dimensão consultável do lançamento financeiro, não informação escondida em observações. A consulta financeira deve retornar e a interface deve exibir o contrato quando existir vínculo.

**Regra recomendada:** lançamentos de mensalidade e de OS `COBRAR_EXTRA` carregam `contrato_id` explícito. Listagem, detalhe, filtros e exportações financeiras exibem ao menos identificador/número do contrato e cliente, sem substituir a origem específica do lançamento. Lançamento manual ou de OS avulsa pode não possuir contrato e deve ser mostrado como “Sem contrato”, nunca como erro ou string inferida.

**Impacto em dados existentes:** parte dos lançamentos pode depender de texto em observações ou da migration local ainda não consolidada. O backfill deve priorizar FK existente e origem determinística; texto é apenas pista para relatório de conciliação. A ausência de contrato em lançamento legado não bloqueia sua consulta.

**Critérios de aceite:**

- lançamentos contratuais novos são consultáveis e filtráveis por contrato;
- lista e detalhe exibem contrato, cliente e origem sem consulta ambígua por texto;
- lançamentos sem contrato continuam visíveis com indicação neutra;
- permissões aplicadas à consulta impedem exposição cruzada indevida de contratos.

### 10.8 Agenda distingue as três origens operacionais

**Decisão técnica:** a agenda deve usar uma origem tipada e mutuamente exclusiva para cada item operacional: `VISITA_AVULSA`, `OS_AVULSA` ou `VISITA_CONTRATO`. Tipo visual, filtros e ações derivam dessa origem, sem heurística por título ou observação.

**Regra recomendada:** `VISITA_AVULSA` é compromisso sem OS obrigatória; `OS_AVULSA` representa execução ligada a uma OS não contratual; `VISITA_CONTRATO` representa a ocorrência contratual e possui contrato e OS próprios. A interface deve apresentar rótulo/indicador acessível e filtro para as três origens. Conversão de origem após execução não é permitida; correção administrativa anterior à execução deve ser auditada e validar os vínculos obrigatórios.

**Impacto em dados existentes:** eventos atuais compartilham tabela e podem não ter discriminador confiável. Classificar automaticamente apenas quando vínculos estruturais forem suficientes: contrato + ocorrência + OS para visita contratual; OS sem contrato para OS avulsa; ausência de OS e contrato para visita avulsa. Conflitos ou lacunas entram em relatório de exceções e permanecem visíveis como origem legada não classificada até conciliação.

**Critérios de aceite:**

- todo novo item da agenda possui exatamente uma origem válida;
- a visualização e os filtros distinguem claramente as três origens, inclusive por texto/acessibilidade e não somente por cor;
- ações disponíveis respeitam a origem e delegam mudanças à OS quando ela for a fonte operacional;
- nenhum item legado ambíguo é silenciosamente classificado pela descrição.

### 10.9 Invariantes transversais aprováveis em conjunto

As oito decisões formam um único conjunto coerente:

1. contrato materializa visitas contratuais futuras;
2. cada visita contratual possui uma OS operacional própria;
3. a OS informa se está coberta ou gera cobrança extra;
4. agenda e notificações acompanham a execução sem assumir faturamento;
5. financeiro mantém vínculo explícito com contrato e origem;
6. estoque reconhece despesa na compra, não novamente no consumo;
7. comandos manuais que afetam estoque e financeiro são atômicos;
8. cancelamento contratual é prospectivo e preserva fatos históricos.

**Critério de saída da Fase 1:** atendido documentalmente pela aprovação das decisões desta seção em conjunto com o fechamento complementar da Fase 1.1 (seção 11). Implementação, testes executáveis, migrations, backfill e correção de dados continuam condicionados ao início explícito da fase correspondente.

## 11. Fase 1.1 — fechamento das decisões pendentes

**Estado:** aprovado para orientar as fases seguintes em 12/07/2026.  
**Limite desta entrega:** decisão e documentação. Nenhum código, teste, schema ou migration foi alterado.

As regras desta seção prevalecem sobre recomendações e perguntas em aberto das seções anteriores. A seção 6 permanece como registro do que motivou a Fase 1.1, não como lista ainda pendente.

### 11.1 Momento da reserva de estoque

**Regra aprovada e justificativa:** a reserva ocorre atomicamente ao iniciar a OS (`AGENDADA`/`ABERTA` → `EM_ANDAMENTO`). Criar ou agendar uma OS não bloqueia saldo; concluir converte a reserva em consumo sem nova baixa; cancelar antes do consumo devolve a reserva. Isso evita retenção por agendas futuras e garante disponibilidade antes da execução.

**Impacto no modelo de dados:** a alocação OS/produto precisa distinguir quantidade solicitada, reservada e consumida, estado da alocação e chave idempotente. O saldo disponível considera reservas; a razão de movimentos continua imutável.

**Impacto no legado:** reservas feitas na criação ou apenas na conclusão devem ser identificadas por diagnóstico. Não haverá correção destrutiva automática; pares inequívocos podem ser classificados no backfill e ambiguidades vão para conciliação.

**Critérios de aceite:** iniciar duas OS concorrentes sobre o último saldo permite apenas uma reserva; retry não duplica baixa; concluir não reduz o saldo pela segunda vez; cancelar devolve exatamente uma vez; OS sem saldo permanece no estado anterior com erro de negócio.

**Dependência para a Fase 2:** definir a entidade/colunas de alocação, escala da quantidade, chaves únicas e operação PostgreSQL atômica de reserva.

### 11.2 Quantidades fracionadas

**Regra aprovada e justificativa:** quantidades fracionadas são permitidas em estoque, OS, compras, reservas e consumo, com até quatro casas decimais. Valores devem ser positivos nos comandos; o sinal é determinado pelo tipo do movimento. Não se usa ponto flutuante. A regra atende produtos medidos por massa, volume ou área e elimina a divergência atual entre `BigDecimal` e `int`.

**Impacto no modelo de dados:** todas as quantidades usam a mesma representação decimal (`numeric(19,4)` no PostgreSQL e `BigDecimal` no Java), inclusive saldo, alocação e movimento. Comparações e somas obedecem escala 4 e arredondamento `HALF_UP` apenas na fronteira de entrada; persistência não arredonda silenciosamente.

**Impacto no legado:** inteiros existentes são convertíveis sem perda. O diagnóstico deve localizar tipos, casts e cálculos que truncam frações; registros com escala superior a quatro exigem relatório e decisão explícita antes do backfill.

**Critérios de aceite:** `0,0001` é aceito; zero, negativo no comando e mais de quatro casas são rejeitados antes da escrita; reserva, consumo, devolução e reconciliação preservam exatamente a quantidade; nenhum caminho converte quantidade para inteiro ou `double`.

**Dependência para a Fase 2:** inventário de todas as colunas e contratos de quantidade, definição dos checks e plano de conversão aditiva sem perda.

### 11.3 Cancelamento de OS concluída

**Regra aprovada e justificativa:** uma OS `CONCLUIDA` não pode transicionar para `CANCELADA`. Erro após conclusão é tratado por comando administrativo de anulação, com motivo, autorização e compensações explícitas; o fato original permanece auditável. A anulação estorna o consumo por movimento compensatório e aplica a política financeira da seção 11.9, sem apagar ou reescrever histórico.

**Impacto no modelo de dados:** histórico deve registrar anulação, comando original, motivo, autor e chaves das compensações. A OS precisa expor a condição de anulada sem reutilizar o status `CANCELADA`; pode ser projeção de uma anulação vinculada, preservando `CONCLUIDA` como estado em que o serviço chegou a existir.

**Impacto no legado:** cancelamentos de OS concluídas e estornos já existentes devem ser conciliados, não reinterpretados automaticamente. Fluxos legados que chamam cancelamento após conclusão serão bloqueados no cutover e direcionados ao comando administrativo.

**Critérios de aceite:** cancelamento comum de concluída é rejeitado; anulação exige permissão e motivo; retry gera no máximo uma compensação por efeito; estoque e financeiro conciliam com o original; histórico mostra conclusão e anulação sem exclusão.

**Dependência para a Fase 2:** estrutura de histórico/anulação, vínculo de compensação e chave idempotente; a autorização funcional será implementada nas fases 3 e 5.

### 11.4 Produtor único de mensalidade e contrato de integração

**Regra aprovada e justificativa:** uma função/RPC PostgreSQL é a única autoridade que cria mensalidades. A Edge Function é o agendador oficial e chama essa RPC; o desktop pode solicitar execução manual ou retry, mas chama o mesmo contrato e não grava mensalidade diretamente. Assim existe um único produtor lógico, ainda que haja mais de um disparador.

**Impacto no modelo de dados:** mensalidade possui origem `CONTRATO`, `contrato_id`, vigência, competência local e unicidade por contrato/vigência/competência. O contrato da RPC recebe `data_referencia` local e `chave_execucao`; retorna competências criadas, já existentes e rejeitadas, sem expor SQL interno. A transação e a idempotência pertencem ao banco.

**Impacto no legado:** os produtores Java e TypeScript paralelos permanecem apenas até o cutover por flag. Mensalidades existentes serão diagnosticadas e conciliadas pela chave de negócio; lançamentos pagos nunca serão mesclados ou removidos automaticamente.

**Critérios de aceite:** Edge e desktop concorrentes produzem uma única mensalidade; a mesma entrada retorna o mesmo resultado; falha parcial faz rollback; contrato inativo ou fora da vigência não fatura; logs identificam disparador e chave; nenhum cliente contém SQL alternativo de faturamento.

**Dependência para a Fase 2:** modelar origem, vigência, competência, unicidade e versão inicial do contrato da RPC. A troca dos produtores ocorre nas fases 5 e 6.

### 11.5 Status calculados versus persistidos

**Regra aprovada e justificativa:** fatos de ciclo de vida são persistidos; estados que decorrem apenas do relógio são calculados. Persistem `DRAFT`, `ACTIVE`, `CANCELLED`, estados operacionais da OS/agenda, `PENDING`, `PARTIAL`, `PAID`, `CANCELLED` e eventos de pagamento/anulação. `EXPIRED`, `MISSED`, `ATRASADA` e `OVERDUE` são projeções calculadas pela data oficial e não recebem jobs de atualização. O agregado financeiro é derivado das parcelas e eventos de liquidação.

**Impacto no modelo de dados:** consultas/views precisam expor o status efetivo e preservar separadamente o status factual. Índices devem apoiar filtros por vencimento/fim sem depender de coluna temporal atualizada. Histórico registra comandos reais, não a mera passagem do tempo.

**Impacto no legado:** valores temporais persistidos permanecem legíveis durante compatibilidade, mas deixam de ser fonte de verdade. O diagnóstico compara status gravado e projeção para medir divergência antes de remover qualquer dependência.

**Critérios de aceite:** a virada do relógio altera a projeção sem escrita; pagamento/cancelamento elimina atraso conforme precedência; parcelas determinam corretamente `PARTIAL`/`PAID`; consultas desktop e RPC retornam o mesmo status efetivo para a mesma data.

**Dependência para a Fase 2:** especificar views/consultas, precedência de estados e índices; testes de caracterização devem congelar um relógio em `America/Sao_Paulo`.

### 11.6 Timezone oficial

**Regra aprovada e justificativa:** `America/Sao_Paulo` é o timezone oficial para competência, vencimento, agenda e virada dos status calculados. Instantes de auditoria e integração são armazenados em UTC (`timestamptz`) e convertidos na borda; datas civis sem horário permanecem `date`. Não se usa offset fixo `-03:00`, para respeitar regras históricas da zona.

**Impacto no modelo de dados:** timestamps de fatos usam instante com zona; competência usa primeiro dia do mês como `date`; vencimentos usam `date`; eventos agendados preservam instante e, quando necessário à recorrência civil, a zona IANA.

**Impacto no legado:** timestamps sem zona exigem classificação por origem antes de conversão. Nenhum backfill deve presumir UTC ou horário local sem evidência; ambiguidades são relatadas.

**Critérios de aceite:** desktop, banco e Edge obtêm a mesma data de negócio; execução perto de 00:00 e da virada do mês não antecipa atraso nem duplica competência; serialização externa usa ISO 8601; testes incluem dados históricos e fronteiras UTC/local.

**Dependência para a Fase 2:** catálogo de colunas temporais, convenção de tipos e função única que resolve `data_referencia` na RPC.

### 11.7 Renovação de contrato

**Regra aprovada e justificativa:** a identidade do contrato é estável e cada renovação cria uma nova vigência versionada, nunca sobrescreve datas anteriores. Renovação automática ou manual usa o mesmo comando idempotente; automática só ocorre quando previamente autorizada no contrato. Vigências não podem se sobrepor e uma renovação cancelada não apaga as anteriores.

**Impacto no modelo de dados:** contrato passa a possuir vigências com sequência, início, fim, regra/autor da renovação e valores aplicáveis. Agenda, OS e mensalidade referenciam a vigência que lhes deu origem; unicidade de competência inclui essa vigência quando necessário, sem permitir cobrança dupla na fronteira.

**Impacto no legado:** datas atuais formam a vigência inicial. Renovações que sobrescreveram o mesmo registro podem não ser reconstruíveis e devem ser sinalizadas; não se inventa histórico. Eventos e cobranças existentes são vinculados apenas quando a data permitir inferência inequívoca.

**Critérios de aceite:** renovar preserva a vigência anterior; retry não cria nova versão; não há lacuna ou sobreposição não autorizada; agenda e mensalidade usam condições da vigência correta; cancelamento prospectivo não altera fatos de vigências encerradas.

**Dependência para a Fase 2:** entidade de vigência, constraints de não sobreposição e estratégia de backfill; materialização de agenda e faturamento consumirão esse vínculo nas fases 3 e 5.

### 11.8 RLS e acesso da Edge Function

**Regra aprovada e justificativa:** RLS deve ser habilitado em todas as tabelas de negócio expostas pelo Supabase, com negação por padrão e políticas mínimas por identidade/perfil. A Edge Function não acessa tabelas de negócio diretamente: usa somente RPCs versionadas `SECURITY DEFINER`, com `search_path` fixo, validação interna e execução concedida exclusivamente ao papel técnico do job. `anon` não executa comandos de negócio.

**Impacto no modelo de dados:** políticas, funções de autorização e identidade do ator/job tornam-se parte do contrato de banco. RPCs gravam ator técnico, chave de execução e auditoria. Donos das funções não são papéis de login da aplicação e permissões públicas são revogadas explicitamente.

**Impacto no legado:** o desktop/Flyway hoje pode usar papel com `BYPASSRLS`; isso permanece temporariamente apenas para compatibilidade e migração, nunca como desenho final de acesso da aplicação. O inventário deve identificar consultas que dependem desse bypass antes do cutover.

**Critérios de aceite:** `anon` não lê nem grava negócio; usuário autenticado acessa apenas o permitido; papel do job executa a RPC mas não possui acesso direto às tabelas; adulteração de ator/competência é rejeitada; testes cobrem `anon`, `authenticated`, papel técnico e proprietário.

**Dependência para a Fase 2:** matriz tabela/operação/perfil, estratégia de identidade e esqueleto versionado das RPCs. Políticas e cutover operacional são concluídos na Fase 6.

### 11.9 Estorno, crédito e reembolso de lançamento pago

**Regra aprovada e justificativa:** lançamento `PAID` é imutável e nunca vira `CANCELLED`. Toda correção começa por um lançamento compensatório de estorno vinculado ao original. Se o valor sair para o cliente, registra-se reembolso pelo meio original quando possível; se permanecer com a empresa, registra-se crédito do cliente para uso futuro. Crédito e reembolso são destinos mutuamente exclusivos para a mesma parcela do valor, embora um caso possa ser dividido explicitamente entre ambos. Taxas não reembolsáveis exigem política e autorização registradas.

**Impacto no modelo de dados:** são necessários vínculo com lançamento original, tipo de compensação, valor, motivo, autor, meio, estado de processamento e chave idempotente. Saldo de crédito é razão de movimentos, não campo livre; reembolso possui eventos de solicitação, confirmação e falha. Valores parciais não podem superar o total pago menos compensações confirmadas.

**Impacto no legado:** pagamentos e cancelamentos históricos são preservados. Casos em que um pago foi marcado cancelado, editado ou apagado entram em conciliação; não se cria reembolso ou crédito retroativo sem evidência e aprovação.

**Critérios de aceite:** não há transição direta de `PAID` para `CANCELLED`; compensações nunca excedem o liquidado; retry não duplica estorno, crédito ou reembolso; falha de reembolso não cria crédito automaticamente; relatórios conciliam caixa, contas a receber e crédito; anulação de OS/contrato usa a mesma política.

**Dependência para a Fase 2:** modelo aditivo de compensação e razão de crédito, constraints de valor e idempotência. Fluxos e integrações de pagamento são implementados na Fase 5.

### 11.10 Critério de saída da Fase 1.1

A Fase 1.1 está documentalmente concluída quando as nove decisões acima constam como aprovadas, não se contradizem com o roadmap e cada item das fases 2 a 6 referencia sua implementação e validação. A aprovação não autoriza alterações técnicas antes do início explícito da fase correspondente.

## 12. Sequência revisada de implementação

1. **Fase 2 — fundação de schema aditivo e integração PostgreSQL:** inventariar tipos e dados; introduzir vigência contratual, alocação decimal (`numeric(19,4)`), origem/competência, histórico/anulação, compensações/crédito, identidade e chaves idempotentes; criar contratos iniciais de RPC/views; ensaiar diagnóstico e backfill em cópia. Nenhuma migration aplicada é reescrita.
2. **Fase 3 — OS e agenda:** tornar a OS fonte operacional; reservar ao iniciar; consumir ao concluir; proibir cancelamento de concluída e oferecer anulação administrativa compensatória; materializar ocorrências por vigência; calcular `ATRASADA`/`MISSED`; usar o timezone oficial.
3. **Fase 4 — estoque:** concluir a razão imutável e alocação fracionada, saldo disponível atômico, idempotência, concorrência e reconciliação; garantir que compra e consumo não dupliquem despesa e que anulação use movimentos compensatórios.
4. **Fase 5 — financeiro e contrato:** ativar vigências versionadas e renovação idempotente; tornar parcelas/eventos a fonte do status financeiro; implementar estorno, crédito e reembolso; publicar a RPC como único produtor de mensalidade e remover escrita direta dos produtores legados sob feature flag.
5. **Fase 6 — Supabase, segurança e cutover:** habilitar e validar RLS com negação por padrão; restringir a Edge Function à RPC versionada; agendar o faturamento oficial; validar timezone e concorrência em homologação; executar cutover/rollback e retirar o bypass da aplicação conforme a matriz aprovada.

### 12.1 Portões obrigatórios entre fases

- **Entrada da Fase 2:** decisões 11.1–11.9 aprovadas e inventário somente leitura concluído.
- **Saída da Fase 2:** migrations aditivas e backfill ensaiados, contratos de RPC/views versionados, dados ambíguos reportados e rollback de código demonstrado.
- **Saída da Fase 3:** agenda/OS sem ciclo, reserva no início e anulação auditável validadas com relógio controlado.
- **Limite transitório da Fase 3:** a fundação PostgreSQL já usa `numeric(19,4)`, mas os comandos, o domínio e os adaptadores Java de estoque/OS ainda carregam quantidade como `int`. Portanto, a Fase 3 não declara suporte fim a fim a quantidades fracionadas. A conversão coerente para `BigDecimal`, incluindo UI, movimentos, alocações, cálculos e persistência sem `intValue()`, é condição de entrada da Fase 4; até lá, o Java aceita somente quantidades inteiras positivas e não deve arredondar dados decimais legados.
- **Saída da Fase 4:** nenhuma perda de precisão, saldo negativo ou efeito duplicado sob concorrência/retry.
- **Saída da Fase 5:** mensalidade possui um produtor lógico; renovação e compensações financeiras conciliam integralmente.
- **Saída da Fase 6:** matriz RLS passa para todos os papéis, Edge não acessa tabelas diretamente e cutover/rollback é aprovado em homologação.

Cada fase deve manter leitura compatível com o legado até seu cutover. Escritas novas usam somente o modelo aprovado; correções de fatos históricos são compensatórias e auditáveis, nunca destrutivas.
