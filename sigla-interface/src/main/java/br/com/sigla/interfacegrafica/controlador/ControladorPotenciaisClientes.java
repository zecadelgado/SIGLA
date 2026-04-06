package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.potenciaisclientes.porta.entrada.CasoDeUsoPotencialCliente;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorTexto;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.springframework.stereotype.Component;

@Component
public class ControladorPotenciaisClientes {

    private final CasoDeUsoPotencialCliente leadUseCase;
    private final ApresentadorTexto textBlockPresenter;

    @FXML
    private Label title;

    @FXML
    private TextArea summary;

    public ControladorPotenciaisClientes(CasoDeUsoPotencialCliente leadUseCase, ApresentadorTexto textBlockPresenter) {
        this.leadUseCase = leadUseCase;
        this.textBlockPresenter = textBlockPresenter;
    }

    @FXML
    public void initialize() {
        title.setText("Potenciais Clientes");
        summary.setText(textBlockPresenter.render(
                leadUseCase.listAll().stream()
                        .map(lead -> lead.name()
                                + " | " + lead.origin()
                                + " | " + lead.status()
                                + " | historico " + lead.interactionHistory().size())
                        .toList(),
                "Nenhum potencial cliente registrado."
        ));
    }
}

