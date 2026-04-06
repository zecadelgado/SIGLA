package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.notificacoes.porta.entrada.CasoDeUsoNotificacao;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorTexto;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ControladorNotificacoes {

    private final CasoDeUsoNotificacao notificationUseCase;
    private final ApresentadorTexto textBlockPresenter;

    @FXML
    private Label title;

    @FXML
    private TextArea summary;

    public ControladorNotificacoes(CasoDeUsoNotificacao notificationUseCase, ApresentadorTexto textBlockPresenter) {
        this.notificationUseCase = notificationUseCase;
        this.textBlockPresenter = textBlockPresenter;
    }

    @FXML
    public void initialize() {
        title.setText("Notificacoes");
        notificationUseCase.refresh(LocalDate.now());
        summary.setText(textBlockPresenter.render(
                notificationUseCase.listAll().stream()
                        .map(notification -> notification.type()
                                + " | " + notification.triggerDate()
                                + " | " + notification.title()
                                + " | " + notification.message())
                        .toList(),
                "Nenhuma notificacao aberta."
        ));
    }
}

