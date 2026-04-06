package br.com.sigla.aplicacao.certificados.casodeuso;

import br.com.sigla.aplicacao.certificados.porta.entrada.CasoDeUsoCertificado;
import br.com.sigla.aplicacao.certificados.porta.saida.RepositorioCertificado;
import br.com.sigla.dominio.certificados.Certificado;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class CasoDeUsoGerenciarCertificado implements CasoDeUsoCertificado {

    private final RepositorioCertificado repository;

    public CasoDeUsoGerenciarCertificado(RepositorioCertificado repository) {
        this.repository = repository;
    }

    @Override
    public void issue(IssueCertificadoCommand command) {
        LocalDate validUntil = command.validUntil() == null ? command.issuedOn().plusMonths(6) : command.validUntil();
        repository.save(new Certificado(
                command.id(),
                command.serviceProvidedId(),
                command.issuedOn(),
                validUntil,
                command.status(),
                command.renewalAlertDays()
        ));
    }

    @Override
    public List<Certificado> listAll() {
        return repository.findAll();
    }

    @Override
    public List<Certificado> expiringCertificados(LocalDate referenceDate) {
        return repository.findAll().stream()
                .filter(certificate -> certificate.isExpiringWithin(referenceDate))
                .toList();
    }
}

