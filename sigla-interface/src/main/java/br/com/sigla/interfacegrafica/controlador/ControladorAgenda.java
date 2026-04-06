package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.agenda.porta.entrada.CasoDeUsoAgenda;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorTexto;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.springframework.stereotype.Component;

@Component
public class ControladorAgenda {

    private final CasoDeUsoAgenda agendaUseCase;
    private final ApresentadorTexto textBlockPresenter;

    @FXML
    private Label title;

    @FXML
    private TextArea summary;

    public ControladorAgenda(CasoDeUsoAgenda agendaUseCase, ApresentadorTexto textBlockPresenter) {
        this.agendaUseCase = agendaUseCase;
        this.textBlockPresenter = textBlockPresenter;
    }

    @FXML
    public void initialize() {
        title.setText("Agenda");
        summary.setText(textBlockPresenter.render(
                agendaUseCase.listAll().stream()
                        .map(schedule -> schedule.id()
                                + " | Cliente " + schedule.customerId()
                                + " | " + schedule.type()
                                + " | data " + schedule.scheduledDate()
                                + " | " + schedule.status())
                        .toList(),
                "Nenhuma visita agendada."
        ));
    }
}

