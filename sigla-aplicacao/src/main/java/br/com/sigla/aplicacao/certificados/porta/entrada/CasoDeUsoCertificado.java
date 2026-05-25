package br.com.sigla.aplicacao.certificados.porta.entrada;

import br.com.sigla.dominio.certificados.Certificado;

import java.time.LocalDate;
import java.util.List;

public interface CasoDeUsoCertificado {

    void issue(IssueCertificadoCommand command);

    List<Certificado> listAll();

    List<Certificado> expiringCertificados(LocalDate referenceDate);

    record IssueCertificadoCommand(
            String id,
            String customerId,
            String serviceProvidedId,
            String orderId,
            String description,
            LocalDate issuedOn,
            LocalDate validUntil,
            int intervalMonths,
            boolean alertActive,
            Certificado.CertificadoStatus status,
            int renewalAlertDays,
            String notes
    ) {
        public IssueCertificadoCommand(
                String id,
                String serviceProvidedId,
                LocalDate issuedOn,
                LocalDate validUntil,
                Certificado.CertificadoStatus status,
                int renewalAlertDays
        ) {
            this(id, serviceProvidedId, "", "", "Certificado de higiene", issuedOn, validUntil, 6, true, status, renewalAlertDays, "");
        }
    }
}

