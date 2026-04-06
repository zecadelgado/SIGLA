# Arquitetura Detetizadora

## Contexto

O SIGLA nasceu com documentacao e alguns modelos orientados a uma operacao de otica. A base foi refatorada para atender uma detetizadora com foco em contratos recorrentes, agenda operacional, servicos prestados, cobranca, certificados, estoque rastreavel e fidelizacao.

## Motivacao Da Mudanca

- remover atributos e fluxos herdados do dominio antigo
- explicitar o fluxo principal do novo negocio
- padronizar repositorios e persistencia dos modulos centrais
- preparar a base para anexos, assinaturas e alertas operacionais

## Modulos

- `clientes`: cadastro do cliente, local, CNPJ, telefone, responsaveis e observacoes
- `funcionarios`: funcionarios, papel operacional, contato e status
- `contratos`: contratos, frequencia de servico, regras de renovacao e alerta de vencimento
- `agenda`: visitas mensais, quinzenais e avulsas
- `servicos`: servicos prestados com funcionario executor, valor, pagamento, assinatura e anexos
- `financeiro`: entradas, saidas e parcelamentos
- `estoque`: estoque e movimentacoes rastreaveis
- `certificados`: emissao, validade e alerta de renovacao
- `potenciaisclientes`: potenciais clientes e historico de interacao
- `notificacoes`: alertas operacionais derivados do dominio

## Fluxo Principal

`Cliente -> Contrato -> Agenda de visitas -> Servico prestado -> Assinatura -> Cobranca -> Certificado/Renovacao -> Notificacoes`

## Decisoes Arquiteturais

- manter a separacao em `sigla-dominio`, `sigla-aplicacao`, `sigla-infraestrutura`, `sigla-interface` e `sigla-relatorios`
- modelar o negocio por bounded contexts dentro de cada modulo Maven
- usar entidades de dominio sem dependencia de framework
- expor casos de uso explicitos por modulo
- persistir os contextos essenciais via JPA/Flyway, com fallback em memoria quando o desktop rodar sem datasource
- armazenar assinatura e anexos por adapter de arquivo (`PortaArmazenamentoAnexo`)
- gerar notificacoes de sistema a partir de contratos, certificados, parcelamentos e visitas

## Persistencia

Tabelas centrais adicionadas/ajustadas:

- `clientes` e `customer_contacts`
- `funcionarios`
- `contratos`
- `visit_schedules`
- `provided_servicos` e `service_attachments`
- `certificados`
- `financial_entries`, `financial_expenses`, `installment_plans`
- `estoque_items` e `estoque_movements`
- `potenciaisclientes` e `lead_interactions`
- `notificacoes`

Migration criada:

- `sigla-infraestrutura/src/main/resources/db/migration/V2__create_detetizadora_core_schema.sql`

## Interface

O menu principal foi reorganizado para priorizar:

- Clientes
- Contratos
- Agenda
- Servicos Prestados
- Financeiro
- Estoque
- Certificados
- Potenciais Clientes
- Notificacoes
- Funcionarios

## Riscos

- o desktop ainda usa fallback em memoria por padrao, entao a persistencia relacional depende de profile/configuracao com datasource ativo
- algumas telas ainda sao de visao resumida, sem CRUD completo
- o modulo `sigla-relatorios` segue generico e pode ser refinado depois para relatorios especificos da detetizadora

## Proximos Passos

- completar CRUDs e formularios por modulo no desktop
- adicionar validacoes cruzadas entre cliente, contrato, agenda e servico
- integrar notificacoes com canal externo (email, WhatsApp ou push)
- ampliar testes de integracao com banco para os novos adapters JPA

