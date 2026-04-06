package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.certificados.porta.entrada.CasoDeUsoCertificado;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorTexto;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.springframework.stereotype.Component;

@Component
public class ControladorCertificados {

    private final CasoDeUsoCertificado certificateUseCase;
    private final ApresentadorTexto textBlockPresenter;

    @FXML
    private Label title;

    @FXML
    private TextArea summary;

    public ControladorCertificados(CasoDeUsoCertificado certificateUseCase, ApresentadorTexto textBlockPresenter) {
        this.certificateUseCase = certificateUseCase;
        this.textBlockPresenter = textBlockPresenter;
    }

    @FXML
    public void initialize() {
        title.setText("Certificados");
        summary.setText(textBlockPresenter.render(
                certificateUseCase.listAll().stream()
                        .map(certificate -> certificate.id()
                                + " | servico " + certificate.serviceProvidedId()
                                + " | emissao " + certificate.issuedOn()
                                + " | validade " + certificate.validUntil()
                                + " | " + certificate.status())
                        .toList(),
                "Nenhum certificado emitido."
        ));
    }
}

