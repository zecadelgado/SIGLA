package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.estoque.porta.entrada.CasoDeUsoEstoque;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorTexto;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.springframework.stereotype.Component;

@Component
public class ControladorEstoque {

    private final CasoDeUsoEstoque estoqueUseCase;
    private final ApresentadorTexto textBlockPresenter;

    @FXML
    private Label title;

    @FXML
    private TextArea summary;

    public ControladorEstoque(CasoDeUsoEstoque estoqueUseCase, ApresentadorTexto textBlockPresenter) {
        this.estoqueUseCase = estoqueUseCase;
        this.textBlockPresenter = textBlockPresenter;
    }

    @FXML
    public void initialize() {
        title.setText("Estoque");
        summary.setText(textBlockPresenter.render(
                estoqueUseCase.listAll().stream()
                        .map(item -> item.name()
                                + " | qtd " + item.quantity() + " " + item.unit()
                                + " | movimentacoes " + item.movements().size())
                        .toList(),
                "Nenhum item em estoque."
        ));
    }
}

