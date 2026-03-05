package br.com.sigla.desktop.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.springframework.stereotype.Component;

@Component
public class InventoryController {

    @FXML
    private Label title;

    @FXML
    public void initialize() {
        title.setText("Estoque");
    }
}
