package br.com.sigla.aplicacao.contratos.porta.entrada;

import br.com.sigla.dominio.contratos.Contrato;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

public interface CasoDeUsoContrato {

    void create(CreateContratoCommand command);

    List<Contrato> listAll();

    List<Contrato> expiringContratos(LocalDate referenceDate);

    record CreateContratoCommand(
            String id,
            String customerId,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            Contrato.ContratoType type,
            Contrato.ServiceFrequency serviceFrequency,
            Contrato.ContratoStatus status,
            Contrato.RenewalRule renewalRule,
            BigDecimal monthlyValue,
            boolean alertActive,
            int alertDaysBeforeEnd,
            String notes
    ) {
        public CreateContratoCommand(
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
            this(id, customerId, "", startDate, endDate, type, serviceFrequency, status, renewalRule, BigDecimal.ZERO, true, alertDaysBeforeEnd, "");
        }
    }
}

