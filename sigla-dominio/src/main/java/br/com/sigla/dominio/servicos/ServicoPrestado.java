package br.com.sigla.dominio.servicos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record ServicoPrestado(
        String id,
        String customerId,
        String contractId,
        String scheduleId,
        String employeeId,
        LocalDate executionDate,
        String description,
        BigDecimal amountCharged,
        PaymentStatus paymentStatus,
        SignatureType signatureType,
        String signaturePath,
        List<Attachment> attachments,
        String notes
) {
    public ServicoPrestado {
        id = requireText(id, "id");
        customerId = requireText(customerId, "customerId");
        contractId = normalizeOptional(contractId);
        scheduleId = normalizeOptional(scheduleId);
        employeeId = requireText(employeeId, "employeeId");
        executionDate = Objects.requireNonNull(executionDate, "executionDate is required");
        description = requireText(description, "description");
        amountCharged = requireAmount(amountCharged);
        paymentStatus = Objects.requireNonNull(paymentStatus, "paymentStatus is required");
        signatureType = Objects.requireNonNull(signatureType, "signatureType is required");
        signaturePath = normalizeOptional(signaturePath);
        attachments = List.copyOf(Objects.requireNonNullElse(attachments, List.of()));
        notes = normalizeOptional(notes);
    }

    public boolean isPaid() {
        return paymentStatus == PaymentStatus.PAID;
    }

    public record Attachment(
            String name,
            String storagePath,
            String contentType
    ) {
        public Attachment {
            name = requireText(name, "name");
            storagePath = requireText(storagePath, "storagePath");
            contentType = requireText(contentType, "contentType");
        }
    }

    public enum PaymentStatus {
        PENDING,
        PARTIALLY_PAID,
        PAID,
        OVERDUE
    }

    public enum SignatureType {
        NONE,
        MANUAL,
        DIGITAL
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static BigDecimal requireAmount(BigDecimal value) {
        Objects.requireNonNull(value, "amountCharged is required");
        if (value.signum() < 0) {
            throw new IllegalArgumentException("amountCharged must not be negative");
        }
        return value;
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

