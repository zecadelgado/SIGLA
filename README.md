# SIGLA

Sistema Integrado de Gerenciamento Logistico e Administrativo para oticas.

## Arquitetura

- `sigla-domain`: entidades, value objects e regras de negocio.
- `sigla-application`: casos de uso e ports (entrada/saida).
- `sigla-infrastructure`: persistencia, integracoes fiscais, notificacoes e configuracao tecnica.
- `sigla-reporting`: geracao/impressao de DANFE, recibos e etiquetas.
- `sigla-desktop`: aplicacao JavaFX (FXML + CSS) com bootstrap Spring Boot.

## Requisitos tecnicos

- Java 25
- Maven Wrapper (`mvnw` / `mvnw.cmd`)

## Build rapido

```bash
./mvnw clean test
./mvnw -pl sigla-desktop spring-boot:run
```

No Windows PowerShell:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd -pl sigla-desktop spring-boot:run
```
