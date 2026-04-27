package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.funcionarios.porta.entrada.CasoDeUsoFuncionario;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class ControladorCadastro extends ControladorComMenuPrincipal {

    private final CasoDeUsoCliente casoDeUsoCliente;
    private final CasoDeUsoFuncionario casoDeUsoFuncionario;
    private final GerenciadorNavegacao gerenciadorNavegacao;

    @FXML
    private TextField searchField;
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

    private String filtroAtual = "TODOS";

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
            casoDeUsoCliente.listAll().forEach(customer -> registros.add(new CadastroRow(
                    customer.name(),
                    customer.cpf(),
                    customer.cnpj(),
                    customer.razaoSocial(),
                    customer.phone(),
                    customer.email().isBlank() ? "-" : customer.email(),
                    blankAsDash(customer.cep()),
                    blankAsDash(customer.cidade())
            )));
        }
        if (!"CLIENTE".equals(filtroAtual)) {
            casoDeUsoFuncionario.listAll().forEach(employee -> registros.add(new CadastroRow(
                    employee.name(),
                    employee.role(),
                    "",
                    "-",
                    employee.contact(),
                    "-",
                    "-",
                    "-"
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
                        || row.telefone().toLowerCase(Locale.ROOT).contains(termo))
                .toList());
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
            String nome,
            String cpf,
            String cnpj,
            String razaoSocial,
            String telefone,
            String email,
            String cep,
            String cidade
    ) {
    }
}
