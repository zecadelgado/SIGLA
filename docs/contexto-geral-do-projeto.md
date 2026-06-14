# Contexto Geral Do Projeto SIGLA

## Resumo

O SIGLA e um ERP desktop para operacao de detetizacao. A aplicacao organiza o fluxo operacional de clientes, contratos recorrentes, agenda de visitas, ordens de servico, servicos prestados, financeiro, estoque, certificados, potenciais clientes, funcionarios, usuarios e notificacoes.

O projeto segue uma arquitetura em camadas, com separacao forte entre regra de negocio, casos de uso, infraestrutura tecnica, interface grafica e relatorios.

## Objetivo Do Produto

O sistema centraliza a rotina administrativa e operacional de uma detetizadora:

- cadastro de clientes, responsaveis e indicacoes
- controle de contratos, recorrencia e renovacao
- agenda de visitas mensais, quinzenais e avulsas
- abertura e acompanhamento de ordens de servico
- registro de servicos executados, assinaturas e anexos
- controle financeiro de entradas, despesas, lancamentos e parcelamentos
- controle de estoque e movimentacoes
- emissao e acompanhamento de certificados
- acompanhamento de potenciais clientes
- notificacoes operacionais sobre vencimentos, visitas e pendencias

Fluxo principal do negocio:

```text
Cliente -> Contrato -> Agenda -> Ordem/Servico -> Cobranca -> Certificado/Renovacao -> Notificacoes
```

## Stack Tecnica

- Java 25
- Maven multi-modulo com Maven Wrapper
- Spring Boot 4.0.3
- Spring Context, Scheduling e Data JPA
- JavaFX 25 com FXML e CSS
- PostgreSQL
- Flyway para migracoes de banco
- Spring Security Crypto para senhas
- JUnit 5 e Mockito
- jpackage para empacotamento desktop

## Estrutura Do Repositorio

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
|- pom.xml
|- README.md
|- compose.yaml
```

## Modulos Maven

### `sigla-dominio`

Contem as entidades e regras puras do negocio. Nao deve depender de Spring, JavaFX, JPA ou detalhes de banco.

Contextos presentes:

- `agenda`
- `auditoria`
- `certificados`
- `clientes`
- `contratos`
- `estoque`
- `financeiro`
- `funcionarios`
- `notificacoes`
- `potenciaisclientes`
- `servicos`
- `usuarios`

Use este modulo quando a mudanca envolver invariantes, validacoes e comportamento do dominio.

### `sigla-aplicacao`

Contem os casos de uso e as portas da arquitetura.

Padrao por contexto:

```text
contexto/
|- porta/
|  |- entrada/
|  `- saida/
`- casodeuso/
```

- `porta/entrada`: contratos chamados pela interface.
- `porta/saida`: contratos implementados pela infraestrutura.
- `casodeuso`: orquestracao da regra de negocio.

Use este modulo quando a mudanca envolver fluxo de operacao, comandos, consultas ou coordenacao entre repositorios.

### `sigla-infraestrutura`

Contem os adaptadores tecnicos:

- entidades JPA
- repositorios Spring Data e adaptadores de repositorio
- migracoes Flyway
- armazenamento de anexos em sistema de arquivos
- criptografia de senha
- executor transacional
- relogio do sistema
- integracao de webhook para notificacoes

Principais caminhos:

```text
sigla-infraestrutura/src/main/java/br/com/sigla/infraestrutura
sigla-infraestrutura/src/main/resources/db/migration
```

Use este modulo para mudancas em persistencia, banco, arquivos, integracoes externas e configuracoes tecnicas.

### `sigla-relatorios`

Modulo auxiliar para relatorios e impressao, incluindo recibos, etiquetas, modelos e despacho de impressao.

Use este modulo quando a mudanca envolver arquivo impresso, exportacao ou relatorio operacional.

### `sigla-interface`

Contem a aplicacao desktop JavaFX:

- controllers JavaFX
- arquivos FXML
- CSS
- navegacao
- sessao local
- inicializacao Spring + JavaFX
- formatadores e apresentadores de UI
- servicos de consulta usados pela tela

Ponto de entrada:

```text
br.com.sigla.interfacegrafica.inicializacao.LancadorSigla
```

Fluxo de boot:

```text
LancadorSigla -> AplicacaoDesktopSigla -> AplicacaoSpringSigla -> FluxoAplicacao -> Login
```

O Spring faz scan em `br.com.sigla`, configura entidades JPA em `br.com.sigla.infraestrutura.persistencia.entidade`, repositorios em `br.com.sigla.infraestrutura.persistencia.repositorio` e habilita agendamento.

## Telas Principais

Os FXMLs principais ficam em:

```text
sigla-interface/src/main/resources/fxml/telas
```

Telas observadas:

- login
- principal/dashboard
- clientes/cadastro
- contratos e certificados
- agenda
- ordens de servico
- servicos
- financeiro
- estoque
- novos cadastros, produtos, servicos, transacoes, movimentacoes e indicacoes

Controllers ficam em:

```text
sigla-interface/src/main/java/br/com/sigla/interfacegrafica/controlador
```

## Persistencia E Banco

O banco alvo e PostgreSQL. As migracoes ficam em:

```text
sigla-infraestrutura/src/main/resources/db/migration
```

O schema atual e versionado por Flyway em arquivos `V1` a `V8`, cobrindo cadastro, usuarios, responsaveis, indicacoes, produtos, contratos, ordens de servico, agenda, certificados, estoque, financeiro, anexos, notificacoes, auditoria e referencias financeiras.

Configuracao base da infraestrutura:

```text
sigla-infraestrutura/src/main/resources/application-infra.yml
```

Configuracoes da interface:

```text
sigla-interface/src/main/resources/application.yml
sigla-interface/src/main/resources/application-dev.yml
sigla-interface/src/main/resources/application-prod.yml
sigla-interface/src/main/resources/application-supabase.yml
```

O profile padrao em `application.yml` e `supabase`. Para ambientes fora de desenvolvimento, prefira variaveis de ambiente para credenciais e conexao de banco:

```text
SIGLA_DATASOURCE_URL
SIGLA_DATASOURCE_USERNAME
SIGLA_DATASOURCE_PASSWORD
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
```

Observacao de seguranca: nao replique chaves, senhas ou tokens em documentacao. Se algum segredo estiver versionado, trate como risco operacional e planeje rotacao/migracao para variaveis de ambiente.

## Arquivos De Runtime

A pasta `var/` guarda dados locais gerados em execucao:

- `var/logs`: logs
- `var/attachments`: assinaturas, comprovantes e anexos
- `var/relatorios`: relatorios e arquivos exportados

Os arquivos YAML atuais declaram o caminho de anexos em `sigla.storage.attachment-root`.

## Como Rodar

### Windows PowerShell

Rodar testes:

```powershell
.\mvnw.cmd clean test
```

Instalar dependencias dos modulos e rodar a interface:

```powershell
.\mvnw.cmd -pl sigla-interface -am -DskipTests install
.\mvnw.cmd -pl sigla-interface spring-boot:run
```

Atalho de desenvolvimento:

```powershell
.\scripts\dev\run-desktop.ps1
```

### Linux/macOS

Rodar testes:

```bash
./mvnw clean test
```

Instalar dependencias dos modulos e rodar a interface:

```bash
./mvnw -pl sigla-interface -am -DskipTests install
./mvnw -pl sigla-interface spring-boot:run
```

Atalho de desenvolvimento:

```bash
./scripts/dev/run-desktop.sh
```

## Banco Local Com Docker

O arquivo `compose.yaml` define um PostgreSQL local:

```powershell
docker compose up -d
```

Parametros locais do compose:

- banco: `sigla`
- usuario: `sigla`
- porta: `5432`

Ao usar banco local, ajuste `SPRING_PROFILES_ACTIVE`, `SIGLA_DATASOURCE_URL`, `SIGLA_DATASOURCE_USERNAME` e `SIGLA_DATASOURCE_PASSWORD` conforme necessario.

## Migracoes

Script Windows:

```powershell
.\scripts\db\migrate.ps1
```

Script Linux/macOS:

```bash
./scripts/db/migrate.sh
```

O Spring Boot tambem executa Flyway na inicializacao quando o datasource esta configurado.

## Testes

Suite geral:

```powershell
.\mvnw.cmd test
```

Testar um modulo especifico:

```powershell
.\mvnw.cmd -pl sigla-aplicacao test
.\mvnw.cmd -pl sigla-interface test
.\mvnw.cmd -pl sigla-infraestrutura test
```

Tipos de teste encontrados:

- dominio: regras puras, como estoque
- aplicacao: casos de uso por contexto
- infraestrutura: adaptadores tecnicos
- interface: navegacao, FXML, formatadores, controladores e servicos de consulta

## Empacotamento Desktop

Scripts:

```text
scripts/package/jpackage-win.ps1
scripts/package/jpackage-linux.sh
```

Saidas esperadas:

```text
deploy/jpackage/app-image
deploy/jpackage/installers
```

Exemplo Windows:

```powershell
.\scripts\package\jpackage-win.ps1
```

O empacotamento usa `jpackage` e espera o jar gerado de `sigla-interface`.

## Como Evoluir Uma Funcionalidade

Use este roteiro para manter a arquitetura alinhada:

1. Entenda se a mudanca e regra de negocio, fluxo, persistencia, tela ou relatorio.
2. Para regra pura, altere `sigla-dominio`.
3. Para fluxo de operacao, altere `sigla-aplicacao`.
4. Para banco, arquivo ou integracao, altere `sigla-infraestrutura`.
5. Para tela, FXML, CSS ou navegacao, altere `sigla-interface`.
6. Para impressao/exportacao, altere `sigla-relatorios`.
7. Adicione ou ajuste testes no modulo mais proximo da mudanca.

Exemplo de fluxo para uma nova regra de negocio:

```text
Dominio define comportamento
Aplicacao cria/ajusta caso de uso
Infraestrutura implementa repositorio/adaptador, se necessario
Interface chama porta de entrada
Testes cobrem dominio/aplicacao/interface conforme risco
```

## Convencoes Importantes

- Codigo de negocio usa portugues nos pacotes, classes e metodos principais.
- O dominio deve permanecer livre de framework.
- A interface chama casos de uso, nao repositorios diretamente.
- A aplicacao depende de portas, nao de detalhes de JPA ou arquivo.
- A infraestrutura implementa contratos da aplicacao.
- Migracoes de banco devem ser feitas via Flyway, nao por `ddl-auto`.
- Segredos devem ficar fora de documentacao e, idealmente, fora do repositorio.

## Documentos Relacionados

- `README.md`: resumo rapido e comandos principais.
- `docs/README.md`: indice da documentacao.
- `docs/guia-estrutura-sigla.md`: guia pratico de onde fica cada coisa.
- `docs/arquitetura/visao-geral.md`: resumo curto de arquitetura.
- `docs/arquitetura/arquitetura-limpa.md`: regras de Clean Architecture.
- `docs/modulos/essenciais.md`: lista dos modulos funcionais essenciais.
- `docs/modulos/opcionais.md`: lista de modulos opcionais.
- `docs/operacao/ambientes-dev-prod.md`: notas de ambientes.
- `docs/operacao/empacotamento-jpackage.md`: notas de empacotamento.

## Pontos De Atencao

- O profile padrao aponta para `supabase`; confirme o ambiente antes de rodar comandos que alterem dados.
- A classe `PropriedadesArmazenamentoSigla` espera o prefixo `sigla.armazenamento`, enquanto os YAMLs atuais usam `sigla.storage`; revise esse alinhamento antes de depender de override de caminho de anexos.
- Algumas documentacoes antigas podem citar nomes historicos de tabelas ou fluxos. Para persistencia, use sempre as migracoes Flyway atuais como fonte de verdade.
- A arvore `target/` contem artefatos gerados e nao deve ser usada como fonte para alteracoes manuais.
- O projeto tem scripts Windows e Unix; ao documentar comandos, prefira informar os dois quando o fluxo for compartilhado pela equipe.
