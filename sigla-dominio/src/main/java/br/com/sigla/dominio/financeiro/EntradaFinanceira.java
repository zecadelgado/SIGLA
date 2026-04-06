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
        EntryStatus status
) {
    public EntradaFinanceira {
        id = requireText(id, "id");
        entryType = Objects.requireNonNull(entryType, "entryType is required");
        amount = requireAmount(amount);
        entryDate = Objects.requireNonNull(entryDate, "entryDate is required");
        customerId = normalizeOptional(customerId);
        serviceProvidedId = normalizeOptional(serviceProvidedId);
        status = Objects.requireNonNull(status, "status is required");
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
            return null;
        }
        return value.trim();
    }
}

