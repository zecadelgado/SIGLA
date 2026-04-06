# Guia Da Estrutura Do SIGLA

## Objetivo

Este arquivo existe para matar a pergunta que sempre aparece em refatoracao grande:

"onde e que fica cada coisa nesse troco?"

O SIGLA agora esta organizado em portugues e separado por responsabilidade tecnica. A ideia e simples:

- `sigla-dominio`: regra de negocio pura
- `sigla-aplicacao`: casos de uso e contratos
- `sigla-infraestrutura`: persistencia, arquivos e integracoes tecnicas
- `sigla-interface`: telas, navegacao e inicializacao do desktop
- `sigla-relatorios`: emissao e impressao de relatorios

## Visao Rapida

```text
SIGLA/
|- sigla-dominio/
|- sigla-aplicacao/
|- sigla-infraestrutura/
|- sigla-relatorios/
|- sigla-interface/
|- docs/
|- scripts/
|- deploy/
|- var/
```

## O Que E Cada Modulo

### `sigla-dominio`

Aqui mora o coracao do negocio. Nao entra Spring, JavaFX, JPA nem gambiarra de framework.

Exemplos do que fica aqui:

- entidades como `Cliente`, `Contrato`, `VisitaAgendada`, `ServicoPrestado`
- validacoes de negocio
- enums de status
- comportamento do dominio, como alerta de vencimento e atraso

Caminho base:

`sigla-dominio/src/main/java/br/com/sigla/dominio`

Subpastas principais:

- `clientes`
- `funcionarios`
- `contratos`
- `agenda`
- `servicos`
- `financeiro`
- `estoque`
- `certificados`
- `potenciaisclientes`
- `notificacoes`

### `sigla-aplicacao`

Aqui mora o "o que o sistema faz" para cada modulo. E a camada que orquestra o dominio.

Cada contexto segue o mesmo desenho:

- `porta/entrada`: contratos que a interface usa
- `porta/saida`: contratos que a infraestrutura implementa
- `casodeuso`: implementacao dos casos de uso

Caminho base:

`sigla-aplicacao/src/main/java/br/com/sigla/aplicacao`

Exemplo:

```text
sigla-aplicacao/
|- clientes/
|  |- porta/
|  |  |- entrada/
|  |  `- saida/
|  `- casodeuso/
```

Se tu quer:

- cadastrar cliente
- registrar visita
- emitir certificado
- atualizar notificacoes

e aqui que tu procura primeiro.

### `sigla-infraestrutura`

Aqui mora o que conversa com o mundo real. E o modulo que pega os contratos da aplicacao e faz o servico pesado.

Responsabilidades daqui:

- persistencia JPA
- adaptadores de repositorio
- storage de assinatura e anexos
- configuracoes tecnicas
- migrations Flyway
- relogio do sistema
- execucao transacional

Caminho base:

`sigla-infraestrutura/src/main/java/br/com/sigla/infraestrutura`

Subpastas importantes:

- `persistencia/entidade`: entidades JPA
- `persistencia/repositorio`: adaptadores e Spring Data
- `armazenamento`: arquivos e anexos
- `configuracao`: beans e propriedades
- `relogio`: provedor de data/hora
- `transacao`: execucao transacional

Migrations:

`sigla-infraestrutura/src/main/resources/db/migration`

### `sigla-interface`

Aqui mora o desktop JavaFX. E a camada que mostra o sistema pra pessoa usuaria.

Responsabilidades daqui:

- controllers JavaFX
- FXML
- navegacao entre telas
- inicializacao Spring + JavaFX
- seed de desenvolvimento
- apresentadores/formatadores de UI

Caminho base Java:

`sigla-interface/src/main/java/br/com/sigla/interfacegrafica`

Caminho base FXML:

`sigla-interface/src/main/resources/fxml`

Mapa rapido:

- `controlador`: controllers das telas
- `navegacao`: troca de telas
- `aplicativo`: casca principal da aplicacao
- `inicializacao`: boot do Spring e dados de dev
- `apresentacao`: formatacao de texto/moeda
- `formatador`: mascaras e utilitarios de interface

### `sigla-relatorios`

Modulo auxiliar para relatorios e impressao.

Fica aqui:

- relatorio de etiqueta
- relatorio de recibo
- despachante de impressao
- provedor de modelo

Caminho base:

`sigla-relatorios/src/main/java/br/com/sigla/relatorios`

## Onde Procurar Cada Tipo De Coisa

Se tu quer mexer em regra de negocio:

- vai em `sigla-dominio`

Se tu quer mexer no fluxo de cadastro, registro, emissao ou alerta:

- vai em `sigla-aplicacao`

Se tu quer mexer em banco, migration, JPA ou arquivos:

- vai em `sigla-infraestrutura`

Se tu quer mexer em tela, menu, navegacao ou FXML:

- vai em `sigla-interface`

Se tu quer mexer em impressao ou documento gerado:

- vai em `sigla-relatorios`

## Como Os Modulos Conversam

Fluxo esperado:

`interface -> aplicacao -> dominio`

Quando precisa salvar, ler, anexar arquivo ou integrar com algo tecnico:

`aplicacao -> infraestrutura`

Regra de ouro:

- o dominio nao depende de framework
- a aplicacao conhece contratos
- a infraestrutura implementa contratos
- a interface chama casos de uso

## Fluxo Principal Do Sistema

O sistema esta centrado neste encadeamento:

`Cliente -> Contrato -> Agenda -> Servico Prestado -> Assinatura -> Cobranca -> Certificado/Renovacao -> Notificacoes`

Entao, quando surgir duvida de onde encaixar uma funcionalidade nova, a resposta quase sempre vem daqui.

## Convencao De Nomes

O projeto foi puxado para portugues no que e nosso:

- modulos
- pacotes do dominio e da aplicacao
- pastas de FXML
- classes principais
- controllers
- adaptadores

Alguns nomes continuam tecnicos por obrigacao do ferramental ou por convencao forte:

- `pom.xml`
- `application.yml` e variantes
- `logback-spring.xml`
- `README.md`
- scripts `.sh` e `.ps1`

Se mexer nisso sem motivo bom, o ferramental pode empacar. A ideia aqui e ser pratico, nao fazer poesia com build quebrado.

## Pastas De Apoio

### `docs`

Documentacao funcional e arquitetural.

Arquivos importantes:

- `docs/arquitetura-detetizadora.md`
- `docs/guia-estrutura-sigla.md`

### `scripts`

Automacoes de banco, execucao local e empacotamento.

### `deploy`

Arquivos voltados a distribuicao e empacotamento.

### `var`

Area de apoio para arquivos variaveis em ambiente local.

## Regra Pratica Para Evolucao

Antes de criar pasta nova, pergunta:

1. isso e regra de negocio?
2. isso e caso de uso?
3. isso e adaptador tecnico?
4. isso e interface?
5. isso e relatorio?

Se responder essa sequencia sem inventar moda, dificilmente a arquitetura sai do trilho.
