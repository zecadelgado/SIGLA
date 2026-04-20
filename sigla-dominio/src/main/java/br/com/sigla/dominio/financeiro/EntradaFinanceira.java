package br.com.sigla.dominio.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record EntradaFinanceira(
        String id,
        EntryType entryType,
        BigDecimal amount,
        LocalDate entryDate,
        String customerId,
        String serviceProvidedId,
        String description,
        String category,
        LocalDate dueDate,
        LocalDate paymentDate,
        String paymentMethod,
        String createdBy,
        String orderReference,
        EntryStatus status
) {
    public EntradaFinanceira {
        id = requireText(id, "id");
        entryType = Objects.requireNonNull(entryType, "entryType is required");
        amount = requireAmount(amount);
        entryDate = Objects.requireNonNull(entryDate, "entryDate is required");
        customerId = normalizeOptional(customerId);
        serviceProvidedId = normalizeOptional(serviceProvidedId);
        description = normalizeOptional(description);
        category = normalizeOptional(category);
        dueDate = dueDate == null ? entryDate : dueDate;
        if (paymentDate != null && paymentDate.isBefore(entryDate)) {
            throw new IllegalArgumentException("paymentDate must not be before entryDate");
        }
        paymentMethod = normalizeOptional(paymentMethod);
        createdBy = normalizeOptional(createdBy);
        orderReference = normalizeOptional(orderReference);
        status = Objects.requireNonNull(status, "status is required");
    }

    public EntradaFinanceira(
            String id,
            EntryType entryType,
            BigDecimal amount,
            LocalDate entryDate,
            String customerId,
            String serviceProvidedId,
            EntryStatus status
    ) {
        this(
                id,
                entryType,
                amount,
                entryDate,
                customerId,
                serviceProvidedId,
                "",
                "",
                entryDate,
                null,
                "",
                "",
                null,
                status
        );
    }

    public enum EntryType {
        PIX,
        CASH,
        BOLETO,
        CARD
    }

    public enum EntryStatus {
        PENDING,
        RECEIVED,
        CANCELLED
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static BigDecimal requireAmount(BigDecimal value) {
        Objects.requireNonNull(value, "amount is required");
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        return value;
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }
}

