package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.financeiro.porta.entrada.CasoDeUsoFinanceiro;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorMoeda;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorTexto;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class ControladorFinanceiro {

    private final CasoDeUsoFinanceiro financeiroUseCase;
    private final ApresentadorMoeda currencyPresenter;
    private final ApresentadorTexto textBlockPresenter;

    @FXML
    private Label title;

    @FXML
    private TextArea summary;

    public ControladorFinanceiro(
            CasoDeUsoFinanceiro financeiroUseCase,
            ApresentadorMoeda currencyPresenter,
            ApresentadorTexto textBlockPresenter
    ) {
        this.financeiroUseCase = financeiroUseCase;
        this.currencyPresenter = currencyPresenter;
        this.textBlockPresenter = textBlockPresenter;
    }

    @FXML
    public void initialize() {
        title.setText("Financeiro");
        List<String> lines = new ArrayList<>();
        lines.add("Saldo atual: " + currencyPresenter.format(financeiroUseCase.currentBalance()));
        lines.add("Entradas registradas: " + financeiroUseCase.listEntries().size());
        lines.add("Saidas registradas: " + financeiroUseCase.listExpenses().size());
        lines.add("Planos parcelados: " + financeiroUseCase.listPlanoParcelamentos().size());
        financeiroUseCase.overdueInstallments(LocalDate.now()).forEach(plan ->
                lines.add("Atraso | Plano " + plan.id() + " | Cliente " + plan.customerId() + " | vencimento " + plan.nextDueDate())
        );
        summary.setText(textBlockPresenter.render(lines, "Nenhum dado financeiro disponivel."));
    }
}

