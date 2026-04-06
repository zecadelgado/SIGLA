package br.com.sigla.aplicacao.contratos.casodeuso;

import br.com.sigla.aplicacao.contratos.porta.entrada.CasoDeUsoContrato;
import br.com.sigla.aplicacao.contratos.porta.saida.RepositorioContrato;
import br.com.sigla.dominio.contratos.Contrato;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class CasoDeUsoGerenciarContrato implements CasoDeUsoContrato {

    private final RepositorioContrato repository;

    public CasoDeUsoGerenciarContrato(RepositorioContrato repository) {
        this.repository = repository;
    }

    @Override
    public void create(CreateContratoCommand command) {
        repository.save(new Contrato(
                command.id(),
                command.customerId(),
                command.startDate(),
                command.endDate(),
                command.type(),
                command.serviceFrequency(),
                command.status(),
                command.renewalRule(),
                command.alertDaysBeforeEnd()
        ));
    }

    @Override
    public List<Contrato> listAll() {
        return repository.findAll();
    }

    @Override
    public List<Contrato> expiringContratos(LocalDate referenceDate) {
        return repository.findAll().stream()
                .filter(contract -> contract.isExpiringWithin(referenceDate))
                .toList();
    }
}

