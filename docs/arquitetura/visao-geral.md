# Visao Geral

A solucao segue Clean Architecture com DDD leve:
- Domain: clientes, contratos, agenda, servicos prestados, certificados, financeiro, estoque, potenciaisclientes, funcionarios e notificacoes.
- Application: casos de uso explicitos por contexto e repositorios por contrato.
- Infrastructure: adapters JPA/in-memory, migrations Flyway e armazenamento de anexos.
- Desktop: JavaFX (FXML + CSS) com menu alinhado ao fluxo da detetizadora.
- Reporting: relatorios auxiliares desacoplados do dominio central.

