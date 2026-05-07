package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.agenda.porta.entrada.CasoDeUsoAgenda;
import br.com.sigla.aplicacao.certificados.porta.entrada.CasoDeUsoCertificado;
import br.com.sigla.aplicacao.contratos.porta.entrada.CasoDeUsoContrato;
import br.com.sigla.dominio.agenda.VisitaAgendada;
import br.com.sigla.dominio.certificados.Certificado;
import br.com.sigla.dominio.contratos.Contrato;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorMoeda;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import br.com.sigla.interfacegrafica.navegacao.VisaoAplicacao;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ControladorServicos extends ControladorComMenuPrincipal {

    private static final Locale LOCALE_PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter HORA_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final String ESTILO_CELULA_BASE = "-fx-background-color: white; -fx-border-color: #1f1f1f; "
            + "-fx-background-radius: 10; -fx-border-radius: 10;";
    private static final String ESTILO_CELULA_HOJE = "-fx-background-color: #eaf5ff; -fx-border-color: #00417e; "
            + "-fx-border-width: 2; -fx-background-radius: 10; -fx-border-radius: 10;";
    private static final String ESTILO_CELULA_FORA_DO_MES = "-fx-background-color: #f3f3f3; -fx-border-color: #b5b5b5; "
            + "-fx-background-radius: 10; -fx-border-radius: 10;";

    private final CasoDeUsoAgenda casoDeUsoAgenda;
    private final CasoDeUsoContrato casoDeUsoContrato;
    private final CasoDeUsoCertificado casoDeUsoCertificado;
    private final GerenciadorNavegacao gerenciadorNavegacao;
    private final ApresentadorMoeda apresentadorMoeda;
    private YearMonth mesExibido = YearMonth.now();

    @FXML
    private Label totalServicosLabel;
    @FXML
    private Label recebidosLabel;
    @FXML
    private Label pendentesLabel;
    @FXML
    private Label mesAtualLabel;
    @FXML
    private Button mesAnteriorButton;
    @FXML
    private Button proximoMesButton;
    @FXML
    private GridPane calendarioGrid;

    public ControladorServicos(
            CasoDeUsoAgenda casoDeUsoAgenda,
            CasoDeUsoContrato casoDeUsoContrato,
            CasoDeUsoCertificado casoDeUsoCertificado,
            GerenciadorNavegacao gerenciadorNavegacao,
            ApresentadorMoeda apresentadorMoeda
    ) {
        super(gerenciadorNavegacao);
        this.casoDeUsoAgenda = casoDeUsoAgenda;
        this.casoDeUsoContrato = casoDeUsoContrato;
        this.casoDeUsoCertificado = casoDeUsoCertificado;
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

    @FXML
    private void onMesAnterior() {
        mesExibido = mesExibido.minusMonths(1);
        renderizarCalendario();
    }

    @FXML
    private void onProximoMes() {
        mesExibido = mesExibido.plusMonths(1);
        renderizarCalendario();
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
        renderizarCalendario();
    }

    private void renderizarCalendario() {
        calendarioGrid.getChildren().clear();
        configurarGradeCalendario();
        mesAtualLabel.setText(tituloMes(mesExibido));
        adicionarCabecalhoDiasSemana();

        LocalDate inicio = mesExibido.atDay(1);
        LocalDate fim = mesExibido.atEndOfMonth();
        Map<LocalDate, List<ItemCalendario>> itensPorDia = carregarItensCalendario(inicio, fim);
        int primeiroIndice = inicio.getDayOfWeek().getValue() % 7;

        for (int indice = 0; indice < 42; indice++) {
            LocalDate data = inicio.plusDays(indice - primeiroIndice);
            VBox celula = criarCelulaDia(data, data.getMonth() == mesExibido.getMonth(), itensPorDia.getOrDefault(data, List.of()));
            calendarioGrid.add(celula, indice % 7, (indice / 7) + 1);
        }
    }

    private void configurarGradeCalendario() {
        calendarioGrid.getColumnConstraints().setAll(criarColunas());
        calendarioGrid.getRowConstraints().setAll(criarLinhas());
        calendarioGrid.setHgap(3);
        calendarioGrid.setVgap(3);
    }

    private List<ColumnConstraints> criarColunas() {
        List<ColumnConstraints> colunas = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            ColumnConstraints coluna = new ColumnConstraints();
            coluna.setPercentWidth(100.0 / 7.0);
            coluna.setHgrow(Priority.ALWAYS);
            colunas.add(coluna);
        }
        return colunas;
    }

    private List<RowConstraints> criarLinhas() {
        List<RowConstraints> linhas = new ArrayList<>();
        RowConstraints cabecalho = new RowConstraints();
        cabecalho.setMinHeight(34);
        cabecalho.setPrefHeight(34);
        linhas.add(cabecalho);

        for (int i = 0; i < 6; i++) {
            RowConstraints semana = new RowConstraints();
            semana.setVgrow(Priority.ALWAYS);
            semana.setMinHeight(58);
            semana.setPrefHeight(68);
            linhas.add(semana);
        }
        return linhas;
    }

    private void adicionarCabecalhoDiasSemana() {
        String[] dias = {"Domingo", "Segunda-feira", "Terça-feira", "Quarta-feira", "Quinta-feira", "Sexta-feira", "Sábado"};
        for (int coluna = 0; coluna < dias.length; coluna++) {
            Label label = new Label(dias[coluna]);
            label.setMaxWidth(Double.MAX_VALUE);
            label.setAlignment(Pos.CENTER);
            label.setFont(Font.font("System", FontWeight.BOLD, 18));
            calendarioGrid.add(label, coluna, 0);
        }
    }

    private VBox criarCelulaDia(LocalDate data, boolean pertenceAoMes, List<ItemCalendario> itens) {
        VBox celula = new VBox(3);
        celula.setPadding(new Insets(6, 8, 6, 8));
        celula.setMinHeight(58);
        celula.setMaxWidth(Double.MAX_VALUE);
        celula.setMaxHeight(Double.MAX_VALUE);
        celula.setStyle(estiloCelula(data, pertenceAoMes));

        Label numeroDia = new Label(String.valueOf(data.getDayOfMonth()));
        numeroDia.setFont(Font.font("System", FontWeight.BOLD, 13));
        numeroDia.setTextFill(pertenceAoMes ? javafx.scene.paint.Color.web("#1f1f1f") : javafx.scene.paint.Color.web("#8a8a8a"));
        celula.getChildren().add(numeroDia);

        if (data.equals(LocalDate.now())) {
            Label hoje = criarEtiqueta("Hoje", "#00417e", "#d7ecff");
            celula.getChildren().add(hoje);
        }

        int limite = data.equals(LocalDate.now()) ? 2 : 3;
        itens.stream().limit(limite).map(this::criarEtiquetaEvento).forEach(celula.getChildren()::add);
        if (itens.size() > limite) {
            celula.getChildren().add(criarEtiqueta("+" + (itens.size() - limite) + " itens", "#4b5563", "#e5e7eb"));
        }
        return celula;
    }

    private String estiloCelula(LocalDate data, boolean pertenceAoMes) {
        if (data.equals(LocalDate.now())) {
            return ESTILO_CELULA_HOJE;
        }
        return pertenceAoMes ? ESTILO_CELULA_BASE : ESTILO_CELULA_FORA_DO_MES;
    }

    private Label criarEtiquetaEvento(ItemCalendario item) {
        return criarEtiqueta(item.texto(), item.corTexto(), item.corFundo());
    }

    private Label criarEtiqueta(String texto, String corTexto, String corFundo) {
        Label etiqueta = new Label(texto);
        etiqueta.setMaxWidth(Double.MAX_VALUE);
        etiqueta.setFont(Font.font("System", 11));
        etiqueta.setTextFill(javafx.scene.paint.Color.web(corTexto));
        etiqueta.setStyle("-fx-background-color: " + corFundo + "; -fx-background-radius: 4; -fx-padding: 2 5 2 5;");
        return etiqueta;
    }

    private Map<LocalDate, List<ItemCalendario>> carregarItensCalendario(LocalDate inicio, LocalDate fim) {
        List<ItemCalendario> itens = new ArrayList<>();
        casoDeUsoAgenda.listBetween(inicio, fim).stream()
                .filter(visita -> visita.status() != VisitaAgendada.VisitStatus.CANCELLED)
                .forEach(visita -> itens.add(ItemCalendario.servico(visita)));

        casoDeUsoContrato.listAll().stream()
                .filter(this::contratoVisivelNoCalendario)
                .filter(contrato -> isNoPeriodo(contrato.endDate(), inicio, fim))
                .forEach(contrato -> itens.add(ItemCalendario.contrato(contrato)));

        casoDeUsoCertificado.listAll().stream()
                .filter(this::certificadoVisivelNoCalendario)
                .filter(certificado -> isNoPeriodo(certificado.validUntil(), inicio, fim))
                .forEach(certificado -> itens.add(ItemCalendario.certificado(certificado)));

        return itens.stream()
                .sorted(Comparator.comparing(ItemCalendario::ordem).thenComparing(ItemCalendario::texto))
                .collect(Collectors.groupingBy(ItemCalendario::data));
    }

    private boolean contratoVisivelNoCalendario(Contrato contrato) {
        return contrato.status() != Contrato.ContratoStatus.CANCELLED;
    }

    private boolean certificadoVisivelNoCalendario(Certificado certificado) {
        return certificado.status() != Certificado.CertificadoStatus.REPLACED;
    }

    private boolean isNoPeriodo(LocalDate data, LocalDate inicio, LocalDate fim) {
        return !data.isBefore(inicio) && !data.isAfter(fim);
    }

    private String tituloMes(YearMonth mes) {
        String nome = mes.getMonth().getDisplayName(TextStyle.FULL, LOCALE_PT_BR);
        return nome.substring(0, 1).toUpperCase(LOCALE_PT_BR) + nome.substring(1) + " " + mes.getYear();
    }

    private record ItemCalendario(
            LocalDate data,
            String texto,
            String corTexto,
            String corFundo,
            int ordem
    ) {
        private static ItemCalendario servico(VisitaAgendada visita) {
            String horario = visita.startAt() == null || visita.allDay() ? "" : HORA_FORMATTER.format(visita.startAt()) + " ";
            String descricao = primeiroTexto(visita.title(), visita.serviceType(), visita.id());
            return new ItemCalendario(visita.scheduledDate(), horario + "Serviço: " + descricao, "#075985", "#e0f2fe", 1);
        }

        private static ItemCalendario contrato(Contrato contrato) {
            String descricao = primeiroTexto(contrato.description(), contrato.id());
            return new ItemCalendario(contrato.endDate(), "Contrato: " + descricao, "#92400e", "#fef3c7", 2);
        }

        private static ItemCalendario certificado(Certificado certificado) {
            String descricao = primeiroTexto(certificado.description(), certificado.id());
            return new ItemCalendario(certificado.validUntil(), "Certificado: " + descricao, "#166534", "#dcfce7", 3);
        }

        private static String primeiroTexto(String... valores) {
            for (String valor : valores) {
                if (valor != null && !valor.isBlank()) {
                    return valor.trim();
                }
            }
            return "";
        }
    }
}
