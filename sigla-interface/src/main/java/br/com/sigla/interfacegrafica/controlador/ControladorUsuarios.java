package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.usuarios.porta.entrada.CasoDeUsoUsuario;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorTexto;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.springframework.stereotype.Component;

@Component
public class ControladorUsuarios {

    private final CasoDeUsoUsuario casoDeUsoUsuario;
    private final ApresentadorTexto apresentadorTexto;

    @FXML
    private Label title;

    @FXML
    private TextArea summary;

    public ControladorUsuarios(CasoDeUsoUsuario casoDeUsoUsuario, ApresentadorTexto apresentadorTexto) {
        this.casoDeUsoUsuario = casoDeUsoUsuario;
        this.apresentadorTexto = apresentadorTexto;
    }

    @FXML
    public void initialize() {
        title.setText("Usuarios");
        summary.setText(apresentadorTexto.render(
                casoDeUsoUsuario.listAll().stream()
                        .map(usuario -> usuario.usuario()
                                + " | " + usuario.nome()
                                + " | " + usuario.tipo()
                                + " | " + (usuario.ativo() ? "ativo" : "inativo"))
                        .toList(),
                "Nenhum usuario cadastrado."
        ));
    }
}
