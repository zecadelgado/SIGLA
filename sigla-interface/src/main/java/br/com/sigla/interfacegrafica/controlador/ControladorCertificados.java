package br.com.sigla.interfacegrafica.controlador;

import br.com.sigla.aplicacao.certificados.porta.entrada.CasoDeUsoCertificado;
import br.com.sigla.interfacegrafica.apresentacao.ApresentadorData;
import br.com.sigla.interfacegrafica.consulta.ServicoConsultaReferencias;
import br.com.sigla.interfacegrafica.navegacao.GerenciadorNavegacao;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.springframework.stereotype.Component;

@Component
public class ControladorCertificados extends ControladorComMenuPrincipal {

    private final CasoDeUsoCertificado certificateUseCase;
    private final ServicoConsultaReferencias servicoConsultaReferencias;
    private final ApresentadorData apresentadorData;

    @FXML
    private TableView<CertificadoRow> certificadosTable;
    @FXML
    private TableColumn<CertificadoRow, String> clienteColumn;
    @FXML
    private TableColumn<CertificadoRow, String> emissaoColumn;
    @FXML
    private TableColumn<CertificadoRow, String> validadeColumn;
    @FXML
    private TableColumn<CertificadoRow, String> statusColumn;
    @FXML
    private TableColumn<CertificadoRow, String> alertaColumn;

    public ControladorCertificados(
            CasoDeUsoCertificado certificateUseCase,
            ServicoConsultaReferencias servicoConsultaReferencias,
            ApresentadorData apresentadorData,
            GerenciadorNavegacao gerenciadorNavegacao
    ) {
        super(gerenciadorNavegacao);
        this.certificateUseCase = certificateUseCase;
        this.servicoConsultaReferencias = servicoConsultaReferencias;
        this.apresentadorData = apresentadorData;
    }

    @FXML
    public void initialize() {
        configureTable();
        refresh();
    }

    private void refresh() {
        if (certificadosTable == null) {
            return;
        }
        certificadosTable.getItems().setAll(certificateUseCase.listAll().stream()
                .map(certificate -> new CertificadoRow(
                        clienteNome(certificate.serviceProvidedId()),
                        apresentadorData.format(certificate.issuedOn()),
                        apresentadorData.format(certificate.validUntil()),
                        certificate.status().name(),
                        String.valueOf(certificate.renewalAlertDays())
                ))
                .toList());
    }

    private void configureTable() {
        configureColumn(clienteColumn, 0, CertificadoRow::cliente);
        configureColumn(emissaoColumn, 1, CertificadoRow::emissao);
        configureColumn(validadeColumn, 2, CertificadoRow::validade);
        configureColumn(statusColumn, 3, CertificadoRow::status);
        configureColumn(alertaColumn, 4, CertificadoRow::alerta);
    }

    private void configureColumn(TableColumn<CertificadoRow, String> column, int fallbackIndex, java.util.function.Function<CertificadoRow, String> getter) {
        TableColumn<CertificadoRow, String> target = column != null ? column : getColumn(fallbackIndex);
        if (target != null) {
            target.setCellValueFactory(data -> new ReadOnlyStringWrapper(getter.apply(data.getValue())));
        }
    }

    @SuppressWarnings("unchecked")
    private TableColumn<CertificadoRow, String> getColumn(int index) {
        if (certificadosTable == null || certificadosTable.getColumns().size() <= index) {
            return null;
        }
        return (TableColumn<CertificadoRow, String>) certificadosTable.getColumns().get(index);
    }

    private String clienteNome(String clienteId) {
        return servicoConsultaReferencias.clientes().stream()
                .filter(option -> option.id().equals(clienteId))
                .map(option -> option.label())
                .findFirst()
                .orElse(clienteId == null || clienteId.isBlank() ? "-" : clienteId);
    }

    private record CertificadoRow(String cliente, String emissao, String validade, String status, String alerta) {
    }
}

