package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.funcionarios.porta.entrada.CasoDeUsoFuncionario;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorTexto;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.springframework.stereotype.Component;

@Component
public class ControladorFuncionarios {

    private final CasoDeUsoFuncionario employeeUseCase;
    private final ApresentadorTexto textBlockPresenter;

    @FXML
    private Label title;

    @FXML
    private TextArea summary;

    public ControladorFuncionarios(CasoDeUsoFuncionario employeeUseCase, ApresentadorTexto textBlockPresenter) {
        this.employeeUseCase = employeeUseCase;
        this.textBlockPresenter = textBlockPresenter;
    }

    @FXML
    public void initialize() {
        title.setText("Funcionarios");
        summary.setText(textBlockPresenter.render(
                employeeUseCase.listAll().stream()
                        .map(employee -> employee.name()
                                + " | " + employee.role()
                                + " | " + employee.contact()
                                + " | " + employee.status())
                        .toList(),
                "Nenhum funcionario cadastrado."
        ));
    }
}

