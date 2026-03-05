# Clean Architecture

## Regras
- A UI nao conhece detalhes de banco ou integracoes fiscais.
- Application depende de portas e nao de frameworks.
- Infrastructure implementa ports do Application.
- Domain nao depende de Spring, JavaFX nem JPA.
