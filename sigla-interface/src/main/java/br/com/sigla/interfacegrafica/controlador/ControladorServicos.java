package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.agenda.porta.entrada.CasoDeUsoAgenda;
import br.com.sigla.dominio.agenda.VisitaAgendada;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorMoeda;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ControladorServicos extends ControladorComMenuPrincipal {

    private final CasoDeUsoAgenda casoDeUsoAgenda;
    private final GerenciadorNavegacao gerenciadorNavegacao;
    private final ApresentadorMoeda apresentadorMoeda;

    @FXML
    private Label totalServicosLabel;
    @FXML
    private Label recebidosLabel;
    @FXML
    private Label pendentesLabel;

    public ControladorServicos(
            CasoDeUsoAgenda casoDeUsoAgenda,
            GerenciadorNavegacao gerenciadorNavegacao,
            ApresentadorMoeda apresentadorMoeda
    ) {
        super(gerenciadorNavegacao);
        this.casoDeUsoAgenda = casoDeUsoAgenda;
        this.gerenciadorNavegacao = gerenciadorNavegacao;
        this.apresentadorMoeda = apresentadorMoeda;
    }

    @FXML
    public void initialize() {
        refresh();
    }

    @FXML
    private void onNovoServico() {
        gerenciadorNavegacao.navigateTo(VisaoAplicacao.NEW_SERVICE);
    }

    private void refresh() {
        var services = casoDeUsoAgenda.listAll();
        totalServicosLabel.setText(String.valueOf(services.size()));

        BigDecimal recebidos = BigDecimal.ZERO;
        recebidosLabel.setText(apresentadorMoeda.format(recebidos));

        BigDecimal pendentes = BigDecimal.valueOf(services.stream()
                .filter(service -> service.status() != VisitaAgendada.VisitStatus.COMPLETED)
                .count());
        pendentesLabel.setText(apresentadorMoeda.format(pendentes));
    }
}
