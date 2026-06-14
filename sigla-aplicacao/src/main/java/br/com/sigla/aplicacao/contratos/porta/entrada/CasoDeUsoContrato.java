package br.com.sigla.aplicacao.contratos.porta.entrada;

import br.com.sigla.dominio.contratos.Contrato;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

public interface CasoDeUsoContrato {

    void create(CreateContratoCommand command);

    void update(UpdateContratoCommand command);

    void encerrar(EncerrarContratoCommand command);

    void renovar(RenovarContratoCommand command);

    /** Marca como EXPIRED os contratos ATIVO cuja data fim ja passou. Retorna os afetados. */
    List<Contrato> marcarVencidos(LocalDate referenceDate);

    List<Contrato> listAll();

    List<Contrato> expiringContratos(LocalDate referenceDate);

    record UpdateContratoCommand(
            String id,
            String customerId,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            Contrato.ContratoType type,
            Contrato.ServiceFrequency serviceFrequency,
            Contrato.RenewalRule renewalRule,
            BigDecimal monthlyValue,
            boolean alertActive,
            int alertDaysBeforeEnd,
            String notes
    ) {
    }

    record EncerrarContratoCommand(String id, String motivo) {
    }

    record RenovarContratoCommand(String id, LocalDate novaDataFim) {
    }

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

