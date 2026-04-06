package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.servicos.porta.entrada.CasoDeUsoServicoPrestado;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorMoeda;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorTexto;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.springframework.stereotype.Component;

@Component
public class ControladorServicos {

    private final CasoDeUsoServicoPrestado serviceProvidedUseCase;
    private final ApresentadorMoeda currencyPresenter;
    private final ApresentadorTexto textBlockPresenter;

    @FXML
    private Label title;

    @FXML
    private TextArea summary;

    public ControladorServicos(
            CasoDeUsoServicoPrestado serviceProvidedUseCase,
            ApresentadorMoeda currencyPresenter,
            ApresentadorTexto textBlockPresenter
    ) {
        this.serviceProvidedUseCase = serviceProvidedUseCase;
        this.currencyPresenter = currencyPresenter;
        this.textBlockPresenter = textBlockPresenter;
    }

    @FXML
    public void initialize() {
        title.setText("Servicos Prestados");
        summary.setText(textBlockPresenter.render(
                serviceProvidedUseCase.listAll().stream()
                        .map(service -> service.id()
                                + " | Cliente " + service.customerId()
                                + " | Funcionario " + service.employeeId()
                                + " | " + service.executionDate()
                                + " | " + currencyPresenter.format(service.amountCharged())
                                + " | pagamento " + service.paymentStatus()
                                + " | assinatura " + service.signatureType())
                        .toList(),
                "Nenhum servico prestado registrado."
        ));
    }
}

