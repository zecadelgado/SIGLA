package br.com.sigla.aplicacao.certificados.porta.entrada;

import br.com.sigla.dominio.certificados.Certificado;

import java.time.LocalDate;
import java.util.List;

public interface CasoDeUsoCertificado {

    void issue(IssueCertificadoCommand command);

    void update(UpdateCertificadoCommand command);

    /** Marca o certificado atual como SUBSTITUIDO e emite um novo. Retorna o id do novo. */
    String renovar(RenovarCertificadoCommand command);

    /** Marca como EXPIRED os certificados ATIVO cuja validade ja passou. Retorna os afetados. */
    List<Certificado> marcarVencidos(LocalDate referenceDate);

    List<Certificado> listAll();

    List<Certificado> expiringCertificados(LocalDate referenceDate);

    record UpdateCertificadoCommand(
            String id,
            String customerId,
            String description,
            LocalDate issuedOn,
            LocalDate validUntil,
            int intervalMonths,
            boolean alertActive,
            int renewalAlertDays,
            String notes
    ) {
    }

    record RenovarCertificadoCommand(
            String id,
            LocalDate issuedOn,
            int intervalMonths
    ) {
    }

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

