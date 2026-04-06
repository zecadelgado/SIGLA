package br.com.sigla.interfacegrafica.navegacao;

public enum VisaoAplicacao {
    CUSTOMERS("/fxml/clientes/clientes-visao.fxml"),
    CONTRACTS("/fxml/contratos/contratos-visao.fxml"),
    SCHEDULING("/fxml/agenda/agenda-visao.fxml"),
    SERVICES("/fxml/servicos/servicos-visao.fxml"),
    FINANCE("/fxml/financeiro/financeiro-visao.fxml"),
    INVENTORY("/fxml/estoque/estoque-visao.fxml"),
    CERTIFICATES("/fxml/certificados/certificados-visao.fxml"),
    LEADS("/fxml/potenciais-clientes/potenciais-clientes-visao.fxml"),
    NOTIFICATIONS("/fxml/notificacoes/notificacoes-visao.fxml"),
    EMPLOYEES("/fxml/funcionarios/funcionarios-visao.fxml");

    private final String fxmlPath;

    VisaoAplicacao(String fxmlPath) {
        this.fxmlPath = fxmlPath;
    }

    public String fxmlPath() {
        return fxmlPath;
    }
}

