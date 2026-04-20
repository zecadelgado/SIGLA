package br.com.sigla.aplicacao.servicos.porta.entrada;

import br.com.sigla.dominio.servicos.ServicoPrestado;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface CasoDeUsoServicoPrestado {

    void register(RegisterServicoPrestadoCommand command);

    List<ServicoPrestado> listAll();

    record RegisterServicoPrestadoCommand(
            String id,
            String customerId,
            String contractId,
            String scheduleId,
            String employeeId,
            LocalDate executionDate,
            String description,
            BigDecimal amountCharged,
            ServicoPrestado.PaymentStatus paymentStatus,
            ServicoPrestado.SignatureType signatureType,
            String signatureFileName,
            byte[] signaturePayload,
            List<AttachmentCommand> attachments,
            ServicoPrestado.ServiceStatus serviceStatus,
            ServicoPrestado.ServicePriority priority,
            String notes
    ) {
        public RegisterServicoPrestadoCommand(
                String id,
                String customerId,
                String contractId,
                String scheduleId,
                String employeeId,
                LocalDate executionDate,
                String description,
                BigDecimal amountCharged,
                ServicoPrestado.PaymentStatus paymentStatus,
                ServicoPrestado.SignatureType signatureType,
                String signatureFileName,
                byte[] signaturePayload,
                List<AttachmentCommand> attachments,
                String notes
        ) {
            this(
                    id,
                    customerId,
                    contractId,
                    scheduleId,
                    employeeId,
                    executionDate,
                    description,
                    amountCharged,
                    paymentStatus,
                    signatureType,
                    signatureFileName,
                    signaturePayload,
                    attachments,
                    ServicoPrestado.ServiceStatus.SCHEDULED,
                    ServicoPrestado.ServicePriority.NORMAL,
                    notes
            );
        }
    }

    record AttachmentCommand(
            String fileName,
            String contentType,
            byte[] payload
    ) {
    }
}

