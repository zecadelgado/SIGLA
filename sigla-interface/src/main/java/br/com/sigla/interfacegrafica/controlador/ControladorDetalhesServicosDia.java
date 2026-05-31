package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.interfacegrafica.apresentacao.ApresentadorData;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorMoeda;
import br.com.sigla.interfacegrafica.consulta.ContextoDetalheOrdemServico;
import br.com.sigla.interfacegrafica.consulta.ServicoConsultaOrdemServico;
import br.com.sigla.interfacegrafica.util.UtilJanela;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Component
public class ControladorDetalhesServicosDia {

    private final ContextoDetalheOrdemServico contextoDetalheOrdemServico;
    private final ServicoConsultaOrdemServico servicoConsultaOrdemServico;
    private final ApresentadorMoeda apresentadorMoeda;
    private final ApresentadorData apresentadorData;

    @FXML
    private Label tituloLabel;
    @FXML
    private Label resumoLabel;
    @FXML
    private TableView<ServicoConsultaOrdemServico.OrdemServicoView> ordensTable;
    @FXML
    private TableColumn<ServicoConsultaOrdemServico.OrdemServicoView, String> numeroColumn;
    @FXML
    private TableColumn<ServicoConsultaOrdemServico.OrdemServicoView, String> clienteColumn;
    @FXML
    private TableColumn<ServicoConsultaOrdemServico.OrdemServicoView, String> tituloColumn;
    @FXML
    private TableColumn<ServicoConsultaOrdemServico.OrdemServicoView, String> statusColumn;
    @FXML
    private TableColumn<ServicoConsultaOrdemServico.OrdemServicoView, String> valorColumn;
    @FXML
    private TextArea detalhesArea;
    @FXML
    private Button fecharButton;

    public ControladorDetalhesServicosDia(
            ContextoDetalheOrdemServico contextoDetalheOrdemServico,
            ServicoConsultaOrdemServico servicoConsultaOrdemServico,
            ApresentadorMoeda apresentadorMoeda,
            ApresentadorData apresentadorData
    ) {
        this.contextoDetalheOrdemServico = contextoDetalheOrdemServico;
        this.servicoConsultaOrdemServico = servicoConsultaOrdemServico;
        this.apresentadorMoeda = apresentadorMoeda;
        this.apresentadorData = apresentadorData;
    }

    @FXML
    public void initialize() {
        configurarTabela();
        carregarOrdens();
    }

    @FXML
    private void onFechar() {
        UtilJanela.fecharJanela(fecharButton);
    }

    private void configurarTabela() {
        numeroColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().numero()));
        clienteColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().customerName()));
        tituloColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().title()));
        statusColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().status()));
        valorColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(apresentadorMoeda.format(data.getValue().amount())));
        ordensTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> renderizarDetalhe(newValue));
    }

    private void carregarOrdens() {
        LocalDate data = contextoDetalheOrdemServico.dataSelecionada();
        List<ServicoConsultaOrdemServico.OrdemServicoView> ordens = data == null
                ? servicoConsultaOrdemServico.listAll()
                : servicoConsultaOrdemServico.listByDate(data);
        String ordemServicoId = contextoDetalheOrdemServico.ordemServicoId();
        ordens = ordens.stream()
                .filter(ordem -> !"CANCELADA".equals(ordem.status()) || ordem.id().equals(ordemServicoId))
                .sorted(Comparator.comparing(ServicoConsultaOrdemServico.OrdemServicoView::numero))
                .toList();

        if (tituloLabel != null) {
            tituloLabel.setText(data == null ? "Detalhes dos Servicos" : "Servicos de " + apresentadorData.format(data));
        }
        if (resumoLabel != null) {
            BigDecimal total = ordens.stream()
                    .map(ServicoConsultaOrdemServico.OrdemServicoView::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            resumoLabel.setText(ordens.size() + " ordem(ns) - Total " + apresentadorMoeda.format(total));
        }
        ordensTable.getItems().setAll(ordens);

        if (ordemServicoId != null && !ordemServicoId.isBlank()) {
            ordens.stream()
                    .filter(ordem -> ordem.id().equals(ordemServicoId))
                    .findFirst()
                    .ifPresent(ordem -> ordensTable.getSelectionModel().select(ordem));
        }
        if (ordensTable.getSelectionModel().getSelectedItem() == null && !ordens.isEmpty()) {
            ordensTable.getSelectionModel().selectFirst();
        }
        renderizarDetalhe(ordensTable.getSelectionModel().getSelectedItem());
    }

    private void renderizarDetalhe(ServicoConsultaOrdemServico.OrdemServicoView ordem) {
        if (detalhesArea == null) {
            return;
        }
        if (ordem == null) {
            detalhesArea.setText("Nenhuma ordem de servico selecionada.");
            return;
        }
        BigDecimal valorServico = ordem.amount().subtract(ordem.productTotal());
        String detalhes = "OS: " + ordem.numero()
                + "\nCliente: " + ordem.customerName()
                + "\nTitulo: " + ordem.title()
                + "\nDescricao: " + texto(ordem.description())
                + "\nTipo: " + texto(ordem.serviceType())
                + "\nResponsavel: " + texto(ordem.responsible())
                + "\nStatus: " + ordem.status()
                + "\nData agendada: " + apresentadorData.format(ordem.emissionDate())
                + "\nInicio: " + apresentadorData.format(ordem.startAt())
                + "\nFim: " + apresentadorData.format(ordem.endAt())
                + "\nValor servico: " + apresentadorMoeda.format(valorServico)
                + "\nProdutos: " + ordem.productCount() + " (" + apresentadorMoeda.format(ordem.productTotal()) + ")"
                + "\nValor total: " + apresentadorMoeda.format(ordem.amount())
                + "\nPago: " + simNao(ordem.paid())
                + "\nAssinatura: " + simNao(ordem.signed())
                + "\nAnexos: " + ordem.attachmentCount()
                + "\nObservacoes: " + texto(ordem.notes());
        detalhesArea.setText(detalhes);
    }

    private String texto(String value) {
        return value == null || value.isBlank() || "-".equals(value) ? "-" : value;
    }

    private String simNao(boolean value) {
        return value ? "Sim" : "Nao";
    }
}
