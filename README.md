# SIGLA

Sistema Integrado de Gestao Logistica e Administrativa para operacao de detetizacao.

## Arquitetura

- `sigla-dominio`: entidades e regras puras dos modulos de clientes, contratos, agenda, servicos, financeiro, estoque, certificados, potenciaisclientes, notificacoes e funcionarios.
- `sigla-aplicacao`: casos de uso explicitos e contratos de repositorio/adapters.
- `sigla-infraestrutura`: persistencia, armazenamento de anexos e configuracao tecnica.
- `sigla-relatorios`: relatorios e impressoes auxiliares.
- `sigla-interface`: aplicacao JavaFX com navegacao voltada ao fluxo operacional da detetizadora.

## Guia Da Estrutura

- documentacao geral do projeto: `docs/contexto-geral-do-projeto.md`
- guia rapido do repositorio: `docs/guia-estrutura-sigla.md`
- documento arquitetural da detetizadora: `docs/arquitetura-detetizadora.md`

## Requisitos tecnicos

- Java 25
- Maven Wrapper (`mvnw` / `mvnw.cmd`)
- Acesso ao banco PostgreSQL configurado em `sigla-interface/src/main/resources/application-supabase.yml`

## Build rapido

O perfil padrao usa o banco Supabase. A URL e o usuario padrao ja ficam
configurados; a senha do banco deve ser configurada no Windows antes de rodar.

No Windows PowerShell:

```powershell
.\scripts\dev\configurar-ambiente-supabase.ps1
```

Cole a senha quando o terminal pedir. O script salva estas variaveis no usuario
do Windows:

```text
SIGLA_DATASOURCE_URL
SIGLA_DATASOURCE_USERNAME
SIGLA_DATASOURCE_PASSWORD
```

Nao e necessario Docker para rodar a aplicacao com Supabase.

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

Tambem e possivel usar os scripts de desenvolvimento:

```bash
./scripts/dev/run-desktop.sh
```

No Windows PowerShell:

```powershell
.\scripts\dev\run-desktop.ps1
```

