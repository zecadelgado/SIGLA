package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.usuarios.porta.entrada.CasoDeUsoUsuario;
import br.com.sigla.dominio.usuarios.Usuario;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Tela de gestao de usuarios: lista, cadastro e troca de senha. Usa apenas os
 * metodos ja disponiveis em {@link CasoDeUsoUsuario} (listAll/registrar/trocarSenha),
 * sem alterar o banco. Edicao e desativacao ficam de fora ate existirem metodos de porta.
 */
@Component
public class ControladorUsuarios {

    private final CasoDeUsoUsuario casoDeUsoUsuario;

    @FXML
    private Label title;
    @FXML
    private TableView<UsuarioRow> usuariosTable;
    @FXML
    private TableColumn<UsuarioRow, String> nomeColumn;
    @FXML
    private TableColumn<UsuarioRow, String> usuarioColumn;
    @FXML
    private TableColumn<UsuarioRow, String> emailColumn;
    @FXML
    private TableColumn<UsuarioRow, String> tipoColumn;
    @FXML
    private TableColumn<UsuarioRow, String> statusColumn;

    public ControladorUsuarios(CasoDeUsoUsuario casoDeUsoUsuario) {
        this.casoDeUsoUsuario = casoDeUsoUsuario;
    }

    @FXML
    public void initialize() {
        if (title != null) {
            title.setText("Usuários");
        }
        configurarColunas();
        refresh();
    }

    @FXML
    private void onNovoUsuario() {
        abrirDialogoNovo().ifPresent(command -> {
            executar(() -> casoDeUsoUsuario.registrar(command));
            refresh();
        });
    }

    @FXML
    private void onTrocarSenha() {
        UsuarioRow row = selecionado();
        if (row == null) {
            return;
        }
        abrirDialogoSenha(row).ifPresent(command -> executar(() -> casoDeUsoUsuario.trocarSenha(command)));
    }

    @FXML
    private void onAtualizar() {
        refresh();
    }

    private void refresh() {
        if (usuariosTable == null) {
            return;
        }
        usuariosTable.getItems().setAll(casoDeUsoUsuario.listAll().stream()
                .map(usuario -> new UsuarioRow(
                        usuario.id(),
                        usuario.nome(),
                        usuario.usuario(),
                        usuario.email().isBlank() ? "-" : usuario.email(),
                        usuario.tipo().name(),
                        usuario.ativo() ? "Ativo" : "Inativo"))
                .toList());
    }

    private void configurarColunas() {
        configurarColuna(nomeColumn, 0, UsuarioRow::nome);
        configurarColuna(usuarioColumn, 1, UsuarioRow::usuario);
        configurarColuna(emailColumn, 2, UsuarioRow::email);
        configurarColuna(tipoColumn, 3, UsuarioRow::tipo);
        configurarColuna(statusColumn, 4, UsuarioRow::status);
    }

    private void configurarColuna(TableColumn<UsuarioRow, String> coluna, int fallback, Function<UsuarioRow, String> getter) {
        TableColumn<UsuarioRow, String> alvo = coluna != null ? coluna : obterColuna(fallback);
        if (alvo != null) {
            alvo.setCellValueFactory(data -> new ReadOnlyStringWrapper(getter.apply(data.getValue())));
        }
    }

    @SuppressWarnings("unchecked")
    private TableColumn<UsuarioRow, String> obterColuna(int index) {
        if (usuariosTable == null || usuariosTable.getColumns().size() <= index) {
            return null;
        }
        return (TableColumn<UsuarioRow, String>) usuariosTable.getColumns().get(index);
    }

    private UsuarioRow selecionado() {
        UsuarioRow row = usuariosTable == null ? null : usuariosTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            mostrar("Selecione um usuário.");
        }
        return row;
    }

    private Optional<CasoDeUsoUsuario.RegistrarUsuarioCommand> abrirDialogoNovo() {
        Dialog<CasoDeUsoUsuario.RegistrarUsuarioCommand> dialog = new Dialog<>();
        dialog.setTitle("Novo usuário");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField nome = new TextField();
        TextField usuario = new TextField();
        TextField email = new TextField();
        PasswordField senha = new PasswordField();
        ComboBox<Usuario.TipoUsuario> tipo = new ComboBox<>();
        tipo.getItems().setAll(Usuario.TipoUsuario.values());
        tipo.getSelectionModel().select(Usuario.TipoUsuario.OPERADOR);
        CheckBox ativo = new CheckBox("Ativo");
        ativo.setSelected(true);
        GridPane grid = grid();
        grid.addRow(0, new Label("Nome"), nome);
        grid.addRow(1, new Label("Usuário"), usuario);
        grid.addRow(2, new Label("E-mail"), email);
        grid.addRow(3, new Label("Senha"), senha);
        grid.addRow(4, new Label("Tipo"), tipo);
        grid.add(ativo, 1, 5);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> button == ButtonType.OK ? new CasoDeUsoUsuario.RegistrarUsuarioCommand(
                UUID.randomUUID().toString(),
                nome.getText(),
                usuario.getText(),
                email.getText(),
                senha.getText(),
                tipo.getValue(),
                ativo.isSelected()) : null);
        return dialog.showAndWait();
    }

    private Optional<CasoDeUsoUsuario.TrocarSenhaCommand> abrirDialogoSenha(UsuarioRow row) {
        Dialog<CasoDeUsoUsuario.TrocarSenhaCommand> dialog = new Dialog<>();
        dialog.setTitle("Trocar senha de " + row.usuario());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        PasswordField atual = new PasswordField();
        PasswordField nova = new PasswordField();
        GridPane grid = grid();
        grid.addRow(0, new Label("Senha atual"), atual);
        grid.addRow(1, new Label("Nova senha"), nova);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> button == ButtonType.OK ? new CasoDeUsoUsuario.TrocarSenhaCommand(
                row.id(), atual.getText(), nova.getText()) : null);
        return dialog.showAndWait();
    }

    private GridPane grid() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        return grid;
    }

    private void executar(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception exception) {
            mostrar(br.com.sigla.interfacegrafica.util.MensagensErro.descrever(exception));
        }
    }

    private void mostrar(String message) {
        br.com.sigla.interfacegrafica.util.DialogoUi.informacao(message);
    }

    private record UsuarioRow(
            String id,
            String nome,
            String usuario,
            String email,
            String tipo,
            String status
    ) {
    }
}
