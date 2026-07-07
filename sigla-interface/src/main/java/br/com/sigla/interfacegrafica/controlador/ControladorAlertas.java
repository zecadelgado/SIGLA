package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.agenda.porta.entrada.CasoDeUsoAgenda;
import br.com.sigla.aplicacao.certificados.porta.entrada.CasoDeUsoCertificado;
import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.contratos.porta.entrada.CasoDeUsoContrato;
import br.com.sigla.aplicacao.financeiro.porta.entrada.CasoDeUsoFinanceiro;
import br.com.sigla.aplicacao.notificacoes.porta.entrada.CasoDeUsoEnvioNotificacao;
import br.com.sigla.dominio.agenda.VisitaAgendada;
import br.com.sigla.dominio.certificados.Certificado;
import br.com.sigla.dominio.clientes.Cliente;
import br.com.sigla.dominio.contratos.Contrato;
import br.com.sigla.dominio.financeiro.LancamentoFinanceiro;
import br.com.sigla.dominio.notificacoes.Notificacao;
import br.com.sigla.interfacegrafica.async.ExecutorTarefasUi;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import br.com.sigla.interfacegrafica.util.DialogoUi;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Central de alertas in-app: reune, num so lugar, o que precisa de atencao — contratos e
 * certificados proximos do vencimento, visitas proximas e nao realizadas, parcelas em atraso
 * e falhas de envio de notificacao. So leitura + atalhos; nao dispara nada por si.
 */
@Component
public class ControladorAlertas {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int JANELA_VISITAS_DIAS = 7;

    private final CasoDeUsoContrato casoDeUsoContrato;
    private final CasoDeUsoCertificado casoDeUsoCertificado;
    private final CasoDeUsoAgenda casoDeUsoAgenda;
    private final CasoDeUsoFinanceiro casoDeUsoFinanceiro;
    private final CasoDeUsoCliente casoDeUsoCliente;
    private final CasoDeUsoEnvioNotificacao casoDeUsoEnvio;
    private final GerenciadorNavegacao navegacao;
    private final ExecutorTarefasUi executorTarefasUi;

    @FXML
    private Label title;
    @FXML
    private Label lblContratos;
    @FXML
    private Label lblCertificados;
    @FXML
    private Label lblVisitasProximas;
    @FXML
    private Label lblVisitasPerdidas;
    @FXML
    private Label lblParcelas;
    @FXML
    private Label lblFalhas;
    @FXML
    private TableView<AlertaRow> alertasTable;

    public ControladorAlertas(
            CasoDeUsoContrato casoDeUsoContrato,
            CasoDeUsoCertificado casoDeUsoCertificado,
            CasoDeUsoAgenda casoDeUsoAgenda,
            CasoDeUsoFinanceiro casoDeUsoFinanceiro,
            CasoDeUsoCliente casoDeUsoCliente,
            CasoDeUsoEnvioNotificacao casoDeUsoEnvio,
            GerenciadorNavegacao navegacao,
            ExecutorTarefasUi executorTarefasUi
    ) {
        this.casoDeUsoContrato = casoDeUsoContrato;
        this.casoDeUsoCertificado = casoDeUsoCertificado;
        this.casoDeUsoAgenda = casoDeUsoAgenda;
        this.casoDeUsoFinanceiro = casoDeUsoFinanceiro;
        this.casoDeUsoCliente = casoDeUsoCliente;
        this.casoDeUsoEnvio = casoDeUsoEnvio;
        this.navegacao = navegacao;
        this.executorTarefasUi = executorTarefasUi;
    }

    @FXML
    public void initialize() {
        if (title != null) {
            title.setText("Alertas");
        }
        configurarColunas();
        if (alertasTable != null) {
            alertasTable.setRowFactory(tabela -> {
                var linha = new javafx.scene.control.TableRow<AlertaRow>();
                linha.setOnMouseClicked(evento -> {
                    if (evento.getClickCount() == 2 && !linha.isEmpty() && linha.getItem().destino() != null) {
                        navegacao.navigateTo(linha.getItem().destino());
                    }
                });
                return linha;
            });
        }
        atualizar();
    }

    @FXML
    private void onAtualizar() {
        atualizar();
    }

    @FXML
    private void onReprocessarFalhas() {
        try {
            int reprocessadas = casoDeUsoEnvio.reprocessarFalhas();
            DialogoUi.informacao("Notificações reprocessadas: " + reprocessadas);
        } catch (Exception excecao) {
            DialogoUi.informacao(br.com.sigla.interfacegrafica.util.MensagensErro.descrever(excecao));
        }
        atualizar();
    }

    private void atualizar() {
        executorTarefasUi.executar(this::carregar, this::exibir);
    }

    private Dados carregar() {
        LocalDate hoje = LocalDate.now();
        Map<String, String> nomes = casoDeUsoCliente.listAll().stream()
                .collect(Collectors.toMap(Cliente::id, Cliente::name, (a, b) -> a));
        List<AlertaRow> linhas = new ArrayList<>();
        int contratos = 0;
        int certificados = 0;
        int visitasProximas = 0;
        int visitasPerdidas = 0;
        int parcelas = 0;
        int falhas = 0;

        for (Contrato contrato : casoDeUsoContrato.expiringContratos(hoje)) {
            linhas.add(new AlertaRow("Contrato", nome(nomes, contrato.customerId()),
                    descricaoOu(contrato.description(), "Contrato"), DATA.format(contrato.endDate()),
                    "Vence em " + entre(hoje, contrato.endDate()), VisaoAplicacao.CONTRACTS_CERTIFICATES));
            contratos++;
        }
        for (Certificado certificado : casoDeUsoCertificado.expiringCertificados(hoje)) {
            linhas.add(new AlertaRow("Certificado", nome(nomes, certificado.customerId()),
                    descricaoOu(certificado.description(), "Certificado"), DATA.format(certificado.validUntil()),
                    "Vence em " + entre(hoje, certificado.validUntil()), VisaoAplicacao.CONTRACTS_CERTIFICATES));
            certificados++;
        }
        for (VisitaAgendada visita : casoDeUsoAgenda.upcomingVisits(hoje, JANELA_VISITAS_DIAS)) {
            if (!visita.isOperational()) {
                continue;
            }
            linhas.add(new AlertaRow("Visita próxima", nome(nomes, visita.customerId()),
                    descricaoOu(visita.serviceType(), visita.title()), DATA.format(visita.scheduledDate()),
                    "Em " + entre(hoje, visita.scheduledDate()), VisaoAplicacao.AGENDA));
            visitasProximas++;
        }
        for (VisitaAgendada visita : casoDeUsoAgenda.overdueVisits(hoje)) {
            if (!visita.isOperational()) {
                continue;
            }
            linhas.add(new AlertaRow("Visita não realizada", nome(nomes, visita.customerId()),
                    descricaoOu(visita.serviceType(), visita.title()), DATA.format(visita.scheduledDate()),
                    "Atrasada há " + entre(visita.scheduledDate(), hoje), VisaoAplicacao.AGENDA));
            visitasPerdidas++;
        }
        for (LancamentoFinanceiro lancamento : casoDeUsoFinanceiro.listLancamentos(null)) {
            if (lancamento.tipo() != LancamentoFinanceiro.Tipo.ENTRY
                    || lancamento.status() == LancamentoFinanceiro.Status.CANCELLED
                    || lancamento.status() == LancamentoFinanceiro.Status.PAID) {
                continue;
            }
            boolean vencido = lancamento.vencido(hoje)
                    || lancamento.parcelas().stream().anyMatch(parcela -> parcela.vencida(hoje));
            if (vencido) {
                linhas.add(new AlertaRow("Parcela em atraso", nome(nomes, lancamento.clienteId()),
                        descricaoOu(lancamento.descricao(), "Conta a receber"), DATA.format(lancamento.dataVencimento()),
                        "Em atraso", VisaoAplicacao.FINANCE));
                parcelas++;
            }
        }
        for (Notificacao notificacao : casoDeUsoEnvio.listar()) {
            if (notificacao.status() == Notificacao.NotificacaoStatus.FAILED) {
                String destinatario = notificacao.destinatario() == null ? "-" : notificacao.destinatario().nome();
                linhas.add(new AlertaRow("Falha de envio", destinatario, notificacao.type().rotulo(),
                        DATA.format(notificacao.momentoDisparo().toLocalDate()),
                        resumir(notificacao.lastError()), VisaoAplicacao.NOTIFICATIONS));
                falhas++;
            }
        }
        return new Dados(linhas, contratos, certificados, visitasProximas, visitasPerdidas, parcelas, falhas);
    }

    private void exibir(Dados dados) {
        if (alertasTable != null) {
            alertasTable.getItems().setAll(dados.linhas());
        }
        setText(lblContratos, dados.contratos());
        setText(lblCertificados, dados.certificados());
        setText(lblVisitasProximas, dados.visitasProximas());
        setText(lblVisitasPerdidas, dados.visitasPerdidas());
        setText(lblParcelas, dados.parcelas());
        setText(lblFalhas, dados.falhas());
    }

    private void configurarColunas() {
        if (alertasTable == null || !alertasTable.getColumns().isEmpty()) {
            return;
        }
        coluna("Tipo", 200, AlertaRow::categoria);
        coluna("Cliente", 320, AlertaRow::cliente);
        coluna("Descrição", 360, AlertaRow::descricao);
        coluna("Quando", 140, AlertaRow::quando);
        coluna("Situação", 300, AlertaRow::situacao);
    }

    private void coluna(String titulo, double largura, Function<AlertaRow, String> getter) {
        TableColumn<AlertaRow, String> coluna = new TableColumn<>(titulo);
        coluna.setPrefWidth(largura);
        coluna.setCellValueFactory(dados -> new ReadOnlyStringWrapper(getter.apply(dados.getValue())));
        alertasTable.getColumns().add(coluna);
    }

    private static String nome(Map<String, String> nomes, String clienteId) {
        String nome = nomes.get(clienteId);
        return nome == null || nome.isBlank() ? "-" : nome;
    }

    private static String descricaoOu(String valor, String alternativa) {
        return valor == null || valor.isBlank() ? alternativa : valor;
    }

    private static String entre(LocalDate inicio, LocalDate fim) {
        long dias = java.time.temporal.ChronoUnit.DAYS.between(inicio, fim);
        if (dias <= 0) {
            return "hoje";
        }
        return dias + (dias == 1 ? " dia" : " dias");
    }

    private static String resumir(String texto) {
        if (texto == null || texto.isBlank()) {
            return "Falha no envio";
        }
        return texto.length() <= 60 ? texto : texto.substring(0, 57) + "...";
    }

    private void setText(Label label, int valor) {
        if (label != null) {
            label.setText(String.valueOf(valor));
        }
    }

    public record AlertaRow(String categoria, String cliente, String descricao, String quando,
                            String situacao, VisaoAplicacao destino) {
    }

    private record Dados(List<AlertaRow> linhas, int contratos, int certificados, int visitasProximas,
                         int visitasPerdidas, int parcelas, int falhas) {
    }
}
