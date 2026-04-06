package br.com.sigla.aplicacao.contratos.porta.entrada;

import br.com.sigla.dominio.contratos.Contrato;

import java.time.LocalDate;
import java.util.List;

public interface CasoDeUsoContrato {

    void create(CreateContratoCommand command);

    List<Contrato> listAll();

    List<Contrato> expiringContratos(LocalDate referenceDate);

    record CreateContratoCommand(
            String id,
            String customerId,
            LocalDate startDate,
            LocalDate endDate,
            Contrato.ContratoType type,
            Contrato.ServiceFrequency serviceFrequency,
            Contrato.ContratoStatus status,
            Contrato.RenewalRule renewalRule,
            int alertDaysBeforeEnd
    ) {
    }
}

