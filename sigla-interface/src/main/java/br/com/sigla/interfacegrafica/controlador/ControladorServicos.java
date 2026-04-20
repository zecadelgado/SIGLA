package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.servicos.porta.entrada.CasoDeUsoServicoPrestado;
import br.com.sigla.dominio.servicos.ServicoPrestado;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorMoeda;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ControladorServicos extends ControladorComMenuPrincipal {

    private final CasoDeUsoServicoPrestado casoDeUsoServicoPrestado;
    private final GerenciadorNavegacao gerenciadorNavegacao;
    private final ApresentadorMoeda apresentadorMoeda;

    @FXML
    private Label totalServicosLabel;
    @FXML
    private Label recebidosLabel;
    @FXML
    private Label pendentesLabel;

    public ControladorServicos(
            CasoDeUsoServicoPrestado casoDeUsoServicoPrestado,
            GerenciadorNavegacao gerenciadorNavegacao,
            ApresentadorMoeda apresentadorMoeda
    ) {
        super(gerenciadorNavegacao);
        this.casoDeUsoServicoPrestado = casoDeUsoServicoPrestado;
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
        var services = casoDeUsoServicoPrestado.listAll();
        totalServicosLabel.setText(String.valueOf(services.size()));

        BigDecimal recebidos = services.stream()
                .filter(service -> service.paymentStatus() == ServicoPrestado.PaymentStatus.PAID)
                .map(ServicoPrestado::amountCharged)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        recebidosLabel.setText(apresentadorMoeda.format(recebidos));

        BigDecimal pendentes = services.stream()
                .filter(service -> service.paymentStatus() != ServicoPrestado.PaymentStatus.PAID)
                .map(ServicoPrestado::amountCharged)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        pendentesLabel.setText(apresentadorMoeda.format(pendentes));
    }
}
