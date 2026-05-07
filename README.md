# SIGLA

Sistema Integrado de Gestao Logistica e Administrativa para operacao de detetizacao.

## Arquitetura

- `sigla-dominio`: entidades e regras puras dos modulos de clientes, contratos, agenda, servicos, financeiro, estoque, certificados, potenciaisclientes, notificacoes e funcionarios.
- `sigla-aplicacao`: casos de uso explicitos e contratos de repositorio/adapters.
- `sigla-infraestrutura`: persistencia, armazenamento de anexos e configuracao tecnica.
- `sigla-relatorios`: relatorios e impressoes auxiliares.
- `sigla-interface`: aplicacao JavaFX com navegacao voltada ao fluxo operacional da detetizadora.

## Guia Da Estrutura

- guia rapido do repositorio: `docs/guia-estrutura-sigla.md`
- documento arquitetural da detetizadora: `docs/arquitetura-detetizadora.md`

## Requisitos tecnicos

- Java 25
- Maven Wrapper (`mvnw` / `mvnw.cmd`)

## Build rapido

```bash
./mvnw clean test
./mvnw -pl sigla-interface -am -DskipTests install
./mvnw -pl sigla-interface spring-boot:run
```

No Windows PowerShell:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd -pl sigla-interface -am -DskipTests install
.\mvnw.cmd -pl sigla-interface spring-boot:run
```

