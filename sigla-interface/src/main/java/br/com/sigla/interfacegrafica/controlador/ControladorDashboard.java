package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.agenda.porta.entrada.CasoDeUsoAgenda;
import br.com.sigla.aplicacao.certificados.porta.entrada.CasoDeUsoCertificado;
import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.contratos.porta.entrada.CasoDeUsoContrato;
import br.com.sigla.aplicacao.estoque.porta.entrada.CasoDeUsoEstoque;
import br.com.sigla.aplicacao.financeiro.porta.entrada.CasoDeUsoFinanceiro;
import br.com.sigla.aplicacao.notificacoes.porta.entrada.CasoDeUsoNotificacao;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorMoeda;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ControladorDashboard {

    private final CasoDeUsoCliente casoDeUsoCliente;
    private final CasoDeUsoContrato casoDeUsoContrato;
    private final CasoDeUsoAgenda casoDeUsoAgenda;
    private final CasoDeUsoFinanceiro casoDeUsoFinanceiro;
    private final CasoDeUsoEstoque casoDeUsoEstoque;
    private final CasoDeUsoCertificado casoDeUsoCertificado;
    private final CasoDeUsoNotificacao casoDeUsoNotificacao;
    private final ApresentadorMoeda apresentadorMoeda;

    @FXML
    private Label totalClientesLabel;
    @FXML
    private Label contratosProximosLabel;
    @FXML
    private Label visitasProximasLabel;
    @FXML
    private Label visitasAtrasadasLabel;
    @FXML
    private Label saldoAtualLabel;
    @FXML
    private Label estoqueBaixoLabel;
    @FXML
    private Label certificadosLabel;
    @FXML
    private Label notificacoesLabel;

    public ControladorDashboard(
            CasoDeUsoCliente casoDeUsoCliente,
            CasoDeUsoContrato casoDeUsoContrato,
            CasoDeUsoAgenda casoDeUsoAgenda,
            CasoDeUsoFinanceiro casoDeUsoFinanceiro,
            CasoDeUsoEstoque casoDeUsoEstoque,
            CasoDeUsoCertificado casoDeUsoCertificado,
            CasoDeUsoNotificacao casoDeUsoNotificacao,
            ApresentadorMoeda apresentadorMoeda
    ) {
        this.casoDeUsoCliente = casoDeUsoCliente;
        this.casoDeUsoContrato = casoDeUsoContrato;
        this.casoDeUsoAgenda = casoDeUsoAgenda;
        this.casoDeUsoFinanceiro = casoDeUsoFinanceiro;
        this.casoDeUsoEstoque = casoDeUsoEstoque;
        this.casoDeUsoCertificado = casoDeUsoCertificado;
        this.casoDeUsoNotificacao = casoDeUsoNotificacao;
        this.apresentadorMoeda = apresentadorMoeda;
    }

    @FXML
    public void initialize() {
        LocalDate today = LocalDate.now();
        casoDeUsoNotificacao.refresh(today);
        totalClientesLabel.setText(String.valueOf(casoDeUsoCliente.listAll().size()));
        contratosProximosLabel.setText(String.valueOf(casoDeUsoContrato.expiringContratos(today).size()));
        visitasProximasLabel.setText(String.valueOf(casoDeUsoAgenda.upcomingVisits(today, 7).size()));
        visitasAtrasadasLabel.setText(String.valueOf(casoDeUsoAgenda.overdueVisits(today).size()));
        saldoAtualLabel.setText(apresentadorMoeda.format(casoDeUsoFinanceiro.currentBalance()));
        estoqueBaixoLabel.setText(String.valueOf(casoDeUsoEstoque.listAll().stream().filter(item -> item.isLowStock()).count()));
        certificadosLabel.setText(String.valueOf(casoDeUsoCertificado.expiringCertificados(today).size()));
        notificacoesLabel.setText(String.valueOf(casoDeUsoNotificacao.listAll().size()));
    }
}
