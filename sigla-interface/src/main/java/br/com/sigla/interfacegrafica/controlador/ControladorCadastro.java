package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.funcionarios.porta.entrada.CasoDeUsoFuncionario;
import br.com.sigla.dominio.clientes.Cliente;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Component
public class ControladorCadastro extends ControladorComMenuPrincipal {

    private final CasoDeUsoCliente casoDeUsoCliente;
    private final CasoDeUsoFuncionario casoDeUsoFuncionario;
    private final GerenciadorNavegacao gerenciadorNavegacao;

    @FXML
    private TextField searchField;
    @FXML
    private ChoiceBox<String> ativoFiltroChoice;
    @FXML
    private TableView<CadastroRow> cadastroTable;
    @FXML
    private TableColumn<CadastroRow, String> nomeColumn;
    @FXML
    private TableColumn<CadastroRow, String> cpfColumn;
    @FXML
    private TableColumn<CadastroRow, String> cnpjColumn;
    @FXML
    private TableColumn<CadastroRow, String> razaoSocialColumn;
    @FXML
    private TableColumn<CadastroRow, String> telefoneColumn;
    @FXML
    private TableColumn<CadastroRow, String> emailColumn;
    @FXML
    private TableColumn<CadastroRow, String> cepColumn;
    @FXML
    private TableColumn<CadastroRow, String> cidadeColumn;
    @FXML
    private TableColumn<CadastroRow, String> statusColumn;
    @FXML
    private TableView<ResponsavelRow> responsaveisTable;
    @FXML
    private TableColumn<ResponsavelRow, String> responsavelNomeColumn;
    @FXML
    private TableColumn<ResponsavelRow, String> responsavelCargoColumn;
    @FXML
    private TableColumn<ResponsavelRow, String> responsavelTelefoneColumn;
    @FXML
    private TableColumn<ResponsavelRow, String> responsavelEmailColumn;
    @FXML
    private TableColumn<ResponsavelRow, String> responsavelPrincipalColumn;

    private String filtroAtual = "TODOS";
    private String filtroAtivo = "ATIVOS";

    public ControladorCadastro(
            CasoDeUsoCliente casoDeUsoCliente,
            CasoDeUsoFuncionario casoDeUsoFuncionario,
            GerenciadorNavegacao gerenciadorNavegacao
    ) {
        super(gerenciadorNavegacao);
        this.casoDeUsoCliente = casoDeUsoCliente;
        this.casoDeUsoFuncionario = casoDeUsoFuncionario;
        this.gerenciadorNavegacao = gerenciadorNavegacao;
    }

    @FXML
    public void initialize() {
        configureTable();
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> refresh());
        }
        if (ativoFiltroChoice != null) {
            ativoFiltroChoice.getItems().setAll("ATIVOS", "INATIVOS", "TODOS");
            ativoFiltroChoice.getSelectionModel().select(filtroAtivo);
            ativoFiltroChoice.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                filtroAtivo = newValue == null ? "ATIVOS" : newValue;
                refresh();
            });
        }
        if (cadastroTable != null) {
            cadastroTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> atualizarResponsaveis(newValue));
        }
        refresh();
    }

    @FXML
    private void onNovoCadastro() {
        gerenciadorNavegacao.navigateTo(VisaoAplicacao.NEW_REGISTRY);
    }

    @FXML
    private void onTodos() {
        filtroAtual = "TODOS";
        refresh();
    }

    @FXML
    private void onFuncionarios() {
        filtroAtual = "FUNCIONARIO";
        refresh();
    }

    @FXML
    private void onClientes() {
        filtroAtual = "CLIENTE";
        refresh();
    }

    private void refresh() {
        String termo = normalizeSearch(searchField == null ? "" : searchField.getText());
        List<CadastroRow> registros = new ArrayList<>();
        if (!"FUNCIONARIO".equals(filtroAtual)) {
            Boolean ativo = switch (filtroAtivo) {
                case "INATIVOS" -> false;
                case "TODOS" -> null;
                default -> true;
            };
            casoDeUsoCliente.filtrar(new CasoDeUsoCliente.FiltroCliente(termo, ativo, null)).forEach(customer -> registros.add(new CadastroRow(
                    customer.id(),
                    true,
                    customer.tipo().name(),
                    customer.name(),
                    customer.cpf(),
                    customer.cnpj(),
                    customer.razaoSocial(),
                    customer.phone(),
                    customer.email().isBlank() ? "-" : customer.email(),
                    blankAsDash(customer.cep()),
                    blankAsDash(customer.cidade()),
                    customer.ativo() ? "Ativo" : "Inativo"
            )));
        }
        if (!"CLIENTE".equals(filtroAtual) && !"INATIVOS".equals(filtroAtivo)) {
            casoDeUsoFuncionario.listAll().forEach(employee -> registros.add(new CadastroRow(
                    employee.id(),
                    false,
                    "FUNCIONARIO",
                    employee.name(),
                    employee.role(),
                    "",
                    "-",
                    employee.contact(),
                    "-",
                    "-",
                    "-",
                    "Ativo"
            )));
        }

        if (cadastroTable == null) {
            return;
        }
        cadastroTable.getItems().setAll(registros.stream()
                .filter(row -> termo.isBlank()
                        || row.nome().toLowerCase(Locale.ROOT).contains(termo)
                        || row.cpf().toLowerCase(Locale.ROOT).contains(termo)
                        || row.cnpj().toLowerCase(Locale.ROOT).contains(termo)
                        || row.telefone().toLowerCase(Locale.ROOT).contains(termo)
                        || row.email().toLowerCase(Locale.ROOT).contains(termo))
                .toList());
        atualizarResponsaveis(cadastroTable.getSelectionModel().getSelectedItem());
    }

    private void configureTable() {
        configureColumn(nomeColumn, 0, row -> row.nome());
        configureColumn(cpfColumn, 1, row -> row.cpf());
        configureColumn(cnpjColumn, 2, row -> row.cnpj());
        configureColumn(razaoSocialColumn, 3, row -> row.razaoSocial());
        configureColumn(telefoneColumn, 4, row -> row.telefone());
        configureColumn(emailColumn, 5, row -> row.email());
        configureColumn(cepColumn, 6, row -> row.cep());
        configureColumn(cidadeColumn, 7, row -> row.cidade());
        configureColumn(statusColumn, 8, row -> row.status());
        configureResponsavelColumn(responsavelNomeColumn, 0, row -> row.nome());
        configureResponsavelColumn(responsavelCargoColumn, 1, row -> row.cargo());
        configureResponsavelColumn(responsavelTelefoneColumn, 2, row -> row.telefone());
        configureResponsavelColumn(responsavelEmailColumn, 3, row -> row.email());
        configureResponsavelColumn(responsavelPrincipalColumn, 4, row -> row.principal() ? "Sim" : "Nao");
    }

    private void configureColumn(TableColumn<CadastroRow, String> column, int fallbackIndex, java.util.function.Function<CadastroRow, String> getter) {
        TableColumn<CadastroRow, String> target = column != null ? column : getColumn(fallbackIndex);
        if (target != null) {
            target.setCellValueFactory(data -> new ReadOnlyStringWrapper(getter.apply(data.getValue())));
        }
    }

    @SuppressWarnings("unchecked")
    private TableColumn<CadastroRow, String> getColumn(int index) {
        if (cadastroTable == null || cadastroTable.getColumns().size() <= index) {
            return null;
        }
        return (TableColumn<CadastroRow, String>) cadastroTable.getColumns().get(index);
    }

    private void configureResponsavelColumn(TableColumn<ResponsavelRow, String> column, int fallbackIndex, java.util.function.Function<ResponsavelRow, String> getter) {
        TableColumn<ResponsavelRow, String> target = column != null ? column : getResponsavelColumn(fallbackIndex);
        if (target != null) {
            target.setCellValueFactory(data -> new ReadOnlyStringWrapper(getter.apply(data.getValue())));
        }
    }

    @SuppressWarnings("unchecked")
    private TableColumn<ResponsavelRow, String> getResponsavelColumn(int index) {
        if (responsaveisTable == null || responsaveisTable.getColumns().size() <= index) {
            return null;
        }
        return (TableColumn<ResponsavelRow, String>) responsaveisTable.getColumns().get(index);
    }

    @FXML
    private void onEditarCadastro() {
        CadastroRow row = clienteSelecionado();
        if (row == null) {
            return;
        }
        Cliente cliente = casoDeUsoCliente.listAll().stream().filter(item -> item.id().equals(row.id())).findFirst().orElse(null);
        if (cliente == null) {
            return;
        }
        Optional<CasoDeUsoCliente.RegisterClienteCommand> command = abrirDialogoCliente(cliente);
        command.ifPresent(value -> {
            executar(() -> casoDeUsoCliente.update(value));
            refresh();
        });
    }

    @FXML
    private void onInativarCadastro() {
        CadastroRow row = clienteSelecionado();
        if (row != null) {
            executar(() -> casoDeUsoCliente.inativar(row.id()));
            refresh();
        }
    }

    @FXML
    private void onReativarCadastro() {
        CadastroRow row = clienteSelecionado();
        if (row != null) {
            executar(() -> casoDeUsoCliente.reativar(row.id()));
            refresh();
        }
    }

    @FXML
    private void onExcluirCadastro() {
        CadastroRow row = clienteSelecionado();
        if (row != null) {
            executar(() -> casoDeUsoCliente.excluirFisicamente(row.id()));
            refresh();
        }
    }

    @FXML
    private void onAdicionarResponsavel() {
        alterarResponsavel(null);
    }

    @FXML
    private void onEditarResponsavel() {
        if (responsaveisTable == null) {
            return;
        }
        alterarResponsavel(responsaveisTable.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void onRemoverResponsavel() {
        CadastroRow row = clienteSelecionado();
        ResponsavelRow responsavel = responsaveisTable == null ? null : responsaveisTable.getSelectionModel().getSelectedItem();
        if (row == null || responsavel == null) {
            return;
        }
        Cliente cliente = casoDeUsoCliente.listAll().stream().filter(item -> item.id().equals(row.id())).findFirst().orElseThrow();
        List<CasoDeUsoCliente.ContactCommand> contatos = cliente.contacts().stream()
                .filter(item -> !item.id().equals(responsavel.id()))
                .map(this::toContactCommand)
                .toList();
        executar(() -> casoDeUsoCliente.update(toCommand(cliente, contatos)));
        refresh();
    }

    @FXML
    private void onMarcarResponsavelPrincipal() {
        CadastroRow row = clienteSelecionado();
        ResponsavelRow responsavel = responsaveisTable == null ? null : responsaveisTable.getSelectionModel().getSelectedItem();
        if (row == null || responsavel == null) {
            return;
        }
        Cliente cliente = casoDeUsoCliente.listAll().stream().filter(item -> item.id().equals(row.id())).findFirst().orElseThrow();
        List<CasoDeUsoCliente.ContactCommand> contatos = cliente.contacts().stream()
                .map(item -> new CasoDeUsoCliente.ContactCommand(item.id(), item.name(), item.role(), item.phone(), item.email(), item.id().equals(responsavel.id())))
                .toList();
        executar(() -> casoDeUsoCliente.update(toCommand(cliente, contatos)));
        refresh();
    }

    private void alterarResponsavel(ResponsavelRow existente) {
        CadastroRow row = clienteSelecionado();
        if (row == null) {
            return;
        }
        Cliente cliente = casoDeUsoCliente.listAll().stream().filter(item -> item.id().equals(row.id())).findFirst().orElseThrow();
        Optional<CasoDeUsoCliente.ContactCommand> novo = abrirDialogoResponsavel(existente);
        novo.ifPresent(contact -> {
            List<CasoDeUsoCliente.ContactCommand> contatos = new ArrayList<>(cliente.contacts().stream().map(this::toContactCommand).toList());
            if (existente == null) {
                contatos.add(contact);
            } else {
                contatos.replaceAll(item -> item.id().equals(existente.id()) ? contact : item);
            }
            executar(() -> casoDeUsoCliente.update(toCommand(cliente, contatos)));
            refresh();
        });
    }

    private Optional<CasoDeUsoCliente.RegisterClienteCommand> abrirDialogoCliente(Cliente cliente) {
        Dialog<CasoDeUsoCliente.RegisterClienteCommand> dialog = new Dialog<>();
        dialog.setTitle("Editar Cliente");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<Cliente.TipoCliente> tipo = new ComboBox<>();
        tipo.getItems().setAll(Cliente.TipoCliente.values());
        tipo.getSelectionModel().select(cliente.tipo());
        TextField nome = field(cliente.name());
        TextField razao = field(cliente.razaoSocial());
        TextField fantasia = field(cliente.nomeFantasia());
        TextField cpf = field(cliente.cpf());
        TextField cnpj = field(cliente.cnpj());
        TextField telefone = field(cliente.phone());
        TextField email = field(cliente.email());
        TextArea observacoes = new TextArea(cliente.notes());
        observacoes.setPrefRowCount(3);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.addRow(0, new javafx.scene.control.Label("Tipo"), tipo);
        grid.addRow(1, new javafx.scene.control.Label("Nome"), nome);
        grid.addRow(2, new javafx.scene.control.Label("Razao social"), razao);
        grid.addRow(3, new javafx.scene.control.Label("Nome fantasia"), fantasia);
        grid.addRow(4, new javafx.scene.control.Label("CPF"), cpf);
        grid.addRow(5, new javafx.scene.control.Label("CNPJ"), cnpj);
        grid.addRow(6, new javafx.scene.control.Label("Telefone"), telefone);
        grid.addRow(7, new javafx.scene.control.Label("E-mail"), email);
        grid.addRow(8, new javafx.scene.control.Label("Observacoes"), observacoes);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> button == ButtonType.OK ? new CasoDeUsoCliente.RegisterClienteCommand(
                cliente.id(), tipo.getValue(), nome.getText(), razao.getText(), fantasia.getText(), cpf.getText(), cnpj.getText(), telefone.getText(),
                email.getText(), cliente.cep(), cliente.rua(), cliente.numero(), cliente.complemento(), cliente.bairro(), cliente.cidade(), cliente.estado(),
                cliente.contacts().stream().map(this::toContactCommand).toList(), observacoes.getText(), cliente.ativo()) : null);
        return dialog.showAndWait();
    }

    private Optional<CasoDeUsoCliente.ContactCommand> abrirDialogoResponsavel(ResponsavelRow existente) {
        Dialog<CasoDeUsoCliente.ContactCommand> dialog = new Dialog<>();
        dialog.setTitle(existente == null ? "Novo Responsavel" : "Editar Responsavel");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField nome = field(existente == null ? "" : existente.nome());
        TextField cargo = field(existente == null ? "" : existente.cargo());
        TextField telefone = field(existente == null ? "" : existente.telefone());
        TextField email = field(existente == null ? "" : existente.email());
        CheckBox principal = new CheckBox("Principal");
        principal.setSelected(existente != null && existente.principal());
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.addRow(0, new javafx.scene.control.Label("Nome"), nome);
        grid.addRow(1, new javafx.scene.control.Label("Cargo"), cargo);
        grid.addRow(2, new javafx.scene.control.Label("Telefone"), telefone);
        grid.addRow(3, new javafx.scene.control.Label("E-mail"), email);
        grid.add(principal, 1, 4);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> button == ButtonType.OK ? new CasoDeUsoCliente.ContactCommand(
                existente == null ? UUID.randomUUID().toString() : existente.id(), nome.getText(), cargo.getText(), telefone.getText(), email.getText(), principal.isSelected()) : null);
        return dialog.showAndWait();
    }

    private void atualizarResponsaveis(CadastroRow row) {
        if (responsaveisTable == null) {
            return;
        }
        if (row == null || !row.cliente()) {
            responsaveisTable.getItems().clear();
            return;
        }
        casoDeUsoCliente.listAll().stream()
                .filter(cliente -> cliente.id().equals(row.id()))
                .findFirst()
                .ifPresentOrElse(cliente -> responsaveisTable.getItems().setAll(cliente.contacts().stream()
                        .map(contact -> new ResponsavelRow(contact.id(), contact.name(), contact.role(), contact.phone(), contact.email(), contact.principal()))
                        .toList()), () -> responsaveisTable.getItems().clear());
    }

    private CadastroRow clienteSelecionado() {
        CadastroRow row = cadastroTable == null ? null : cadastroTable.getSelectionModel().getSelectedItem();
        if (row == null || !row.cliente()) {
            mostrar("Selecione um cliente.");
            return null;
        }
        return row;
    }

    private CasoDeUsoCliente.RegisterClienteCommand toCommand(Cliente cliente, List<CasoDeUsoCliente.ContactCommand> contatos) {
        return new CasoDeUsoCliente.RegisterClienteCommand(
                cliente.id(), cliente.tipo(), cliente.name(), cliente.razaoSocial(), cliente.nomeFantasia(), cliente.cpf(), cliente.cnpj(), cliente.phone(),
                cliente.email(), cliente.cep(), cliente.rua(), cliente.numero(), cliente.complemento(), cliente.bairro(), cliente.cidade(), cliente.estado(),
                contatos, cliente.notes(), cliente.ativo());
    }

    private CasoDeUsoCliente.ContactCommand toContactCommand(Cliente.ContactPerson contact) {
        return new CasoDeUsoCliente.ContactCommand(contact.id(), contact.name(), contact.role(), contact.phone(), contact.email(), contact.principal());
    }

    private TextField field(String value) {
        return new TextField(value == null ? "" : value);
    }

    private void executar(Runnable runnable) {
        try {
            runnable.run();
        } catch (IllegalArgumentException exception) {
            mostrar(exception.getMessage());
        }
    }

    private void mostrar(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.showAndWait();
    }

    private String normalizeSearch(String text) {
        if (text == null) {
            return "";
        }
        String value = text.trim();
        if (value.equalsIgnoreCase("Buscar por nome ou CPF/CNPJ...")) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private String extractCidade(String location) {
        if (location == null || location.isBlank()) {
            return "-";
        }
        String[] parts = location.split(" - ");
        return parts.length >= 2 ? parts[parts.length - 2] : location;
    }

    private String extractCep(String location) {
        if (location == null || location.isBlank()) {
            return "-";
        }
        String[] parts = location.split(" - ");
        return parts.length == 0 ? "-" : parts[parts.length - 1];
    }

    private String blankAsDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private record CadastroRow(
            String id,
            boolean cliente,
            String tipo,
            String nome,
            String cpf,
            String cnpj,
            String razaoSocial,
            String telefone,
            String email,
            String cep,
            String cidade,
            String status
    ) {
    }

    private record ResponsavelRow(
            String id,
            String nome,
            String cargo,
            String telefone,
            String email,
            boolean principal
    ) {
    }
}
