package br.com.sigla.interfacegrafica.navegacao;

public enum VisaoAplicacao {
    LOGIN("/fxml/telas/TelaDeLogin.fxml", false),
    SHELL("/fxml/telas/TelaPrincipal.fxml", false),
    DASHBOARD("/fxml/telas/TelaPrincipal.fxml", true),
    REGISTRY("/fxml/telas/TelaCadastro.fxml", true),
    NEW_REGISTRY("/fxml/telas/TelaNovoCadastro.fxml", true),
    CUSTOMERS("/fxml/telas/TelaClientes.fxml", true),
    NEW_INDICATION("/fxml/telas/TelaNovaIndica\u00e7\u00e3o.fxml", true),
    FINANCE("/fxml/telas/TelaFinanceiro.fxml", true),
    NEW_TRANSACTION("/fxml/telas/TelaNovaTransa\u00e7\u00e3o.fxml", true),
    INVENTORY("/fxml/telas/TelaEstoque.fxml", true),
    NEW_PRODUCT("/fxml/telas/TelaNovoProduto.fxml", true),
    NEW_MOVEMENT("/fxml/telas/TelaNovaMovimenta\u00e7\u00e3o.fxml", true),
    SERVICES("/fxml/telas/TelaServi\u00e7os.fxml", true),
    NEW_SERVICE("/fxml/telas/TelaNovoServi\u00e7o.fxml", true),
    SERVICE_ORDER("/fxml/telas/TelaOrdemdeServi\u00e7o.fxml", true),
    NEW_SERVICE_ORDER("/fxml/telas/TelaNovaOrdemdeServi\u00e7o.fxml", true);

    private final String fxmlPath;
    private final boolean shellContent;

    VisaoAplicacao(String fxmlPath, boolean shellContent) {
        this.fxmlPath = fxmlPath;
        this.shellContent = shellContent;
    }

    public String fxmlPath() {
        return fxmlPath;
    }

    public boolean isShellContent() {
        return shellContent;
    }
}
