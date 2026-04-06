package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorTexto;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.springframework.stereotype.Component;

@Component
public class ControladorClientes {

    private final CasoDeUsoCliente customerUseCase;
    private final ApresentadorTexto textBlockPresenter;

    @FXML
    private Label title;

    @FXML
    private TextArea summary;

    public ControladorClientes(CasoDeUsoCliente customerUseCase, ApresentadorTexto textBlockPresenter) {
        this.customerUseCase = customerUseCase;
        this.textBlockPresenter = textBlockPresenter;
    }

    @FXML
    public void initialize() {
        title.setText("Clientes");
        summary.setText(textBlockPresenter.render(
                customerUseCase.listAll().stream()
                        .map(customer -> customer.name()
                                + " | " + customer.location()
                                + " | CNPJ " + customer.cnpj()
                                + " | Fone " + customer.phone()
                                + " | Responsaveis " + customer.contacts().size())
                        .toList(),
                "Nenhum cliente cadastrado."
        ));
    }
}

