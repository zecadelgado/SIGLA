package br.com.sigla.interfacegrafica.navegacao;

public enum VisaoAplicacao {
    LOGIN("/fxml/telas/TelaDeLogin.fxml", false, "Login", ModoExibicao.PRINCIPAL, false),
    ACCOUNT_REGISTRATION("/fxml/telas/TelaCadastroUsuario.fxml", false, "Cadastro", ModoExibicao.PRINCIPAL, false),
    SHELL("/fxml/telas/TelaPrincipal.fxml", false, "Principal", ModoExibicao.PRINCIPAL),
    DASHBOARD("/fxml/telas/TelaDashboard.fxml", true, "Dashboard", ModoExibicao.PRINCIPAL),
    REGISTRY("/fxml/telas/TelaCadastro.fxml", true, "Cadastros", ModoExibicao.PRINCIPAL),
    NEW_REGISTRY("/fxml/telas/TelaNovoCadastro.fxml", true, "Novo Cadastro", ModoExibicao.SOBREPOSTA),
    CUSTOMERS("/fxml/telas/TelaClientes.fxml", true, "Clientes", ModoExibicao.PRINCIPAL),
    NEW_INDICATION("/fxml/telas/TelaNovaIndica\u00e7\u00e3o.fxml", true, "Nova Indicação", ModoExibicao.SOBREPOSTA),
    FINANCE("/fxml/telas/TelaFinanceiro.fxml", true, "Financeiro", ModoExibicao.PRINCIPAL),
    NEW_TRANSACTION("/fxml/telas/TelaNovaTransa\u00e7\u00e3o.fxml", true, "Nova Transação", ModoExibicao.SOBREPOSTA),
    INVENTORY("/fxml/telas/TelaEstoque.fxml", true, "Estoque", ModoExibicao.PRINCIPAL),
    NEW_PRODUCT("/fxml/telas/TelaNovoProduto.fxml", true, "Novo Produto", ModoExibicao.SOBREPOSTA),
    NEW_MOVEMENT("/fxml/telas/TelaNovaMovimenta\u00e7\u00e3o.fxml", true, "Nova Movimentação", ModoExibicao.SOBREPOSTA),
    SERVICES("/fxml/telas/TelaServi\u00e7os.fxml", true, "Serviços", ModoExibicao.PRINCIPAL),
    NEW_SERVICE("/fxml/telas/TelaNovoServi\u00e7o.fxml", true, "Novo Serviço", ModoExibicao.SOBREPOSTA),
    AGENDA("/fxml/telas/TelaAgenda.fxml", true, "Agenda", ModoExibicao.PRINCIPAL),
    SERVICE_ORDER("/fxml/telas/TelaOrdemdeServi\u00e7o.fxml", true, "Ordem de Serviço", ModoExibicao.PRINCIPAL),
    NEW_SERVICE_ORDER("/fxml/telas/TelaNovaOrdemdeServi\u00e7o.fxml", true, "Nova Ordem de Serviço", ModoExibicao.SOBREPOSTA),
    CONTRACTS_CERTIFICATES("/fxml/telas/TelaContratosCertificados.fxml", true, "Contratos e Certificados", ModoExibicao.PRINCIPAL),
    ;

    private final String fxmlPath;
    private final boolean shellContent;
    private final String tituloJanela;
    private final ModoExibicao modoExibicao;
    private final boolean requiresAuthentication;

    VisaoAplicacao(String fxmlPath, boolean shellContent, String tituloJanela, ModoExibicao modoExibicao) {
        this(fxmlPath, shellContent, tituloJanela, modoExibicao, true);
    }

    VisaoAplicacao(String fxmlPath, boolean shellContent, String tituloJanela, ModoExibicao modoExibicao, boolean requiresAuthentication) {
        this.fxmlPath = fxmlPath;
        this.shellContent = shellContent;
        this.tituloJanela = tituloJanela;
        this.modoExibicao = modoExibicao;
        this.requiresAuthentication = requiresAuthentication;
    }

    public String fxmlPath() {
        return fxmlPath;
    }

    public boolean isShellContent() {
        return shellContent;
    }

    public String tituloJanela() {
        return tituloJanela;
    }

    public boolean isSobreposta() {
        return modoExibicao == ModoExibicao.SOBREPOSTA;
    }

    public boolean requiresAuthentication() {
        return requiresAuthentication;
    }

    private enum ModoExibicao {
        PRINCIPAL,
        SOBREPOSTA
    }
}
