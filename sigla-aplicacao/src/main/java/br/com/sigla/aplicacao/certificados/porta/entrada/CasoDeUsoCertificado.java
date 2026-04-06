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
            String serviceProvidedId,
            LocalDate issuedOn,
            LocalDate validUntil,
            Certificado.CertificadoStatus status,
            int renewalAlertDays
    ) {
    }
}

