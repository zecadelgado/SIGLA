package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.contratos.porta.entrada.CasoDeUsoContrato;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorTexto;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.springframework.stereotype.Component;

@Component
public class ControladorContratos {

    private final CasoDeUsoContrato contractUseCase;
    private final ApresentadorTexto textBlockPresenter;

    @FXML
    private Label title;

    @FXML
    private TextArea summary;

    public ControladorContratos(CasoDeUsoContrato contractUseCase, ApresentadorTexto textBlockPresenter) {
        this.contractUseCase = contractUseCase;
        this.textBlockPresenter = textBlockPresenter;
    }

    @FXML
    public void initialize() {
        title.setText("Contratos");
        summary.setText(textBlockPresenter.render(
                contractUseCase.listAll().stream()
                        .map(contract -> contract.id()
                                + " | Cliente " + contract.customerId()
                                + " | " + contract.serviceFrequency()
                                + " | fim " + contract.endDate()
                                + " | alerta " + contract.alertDaysBeforeEnd() + " dias"
                                + " | " + contract.status())
                        .toList(),
                "Nenhum contrato cadastrado."
        ));
    }
}

