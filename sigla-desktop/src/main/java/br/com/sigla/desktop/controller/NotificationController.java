package br.com.sigla.desktop.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.springframework.stereotype.Component;

@Component
public class NotificationController {

    @FXML
    private Label title;

    @FXML
    public void initialize() {
        title.setText("Avisos e Notificacoes");
    }
}
