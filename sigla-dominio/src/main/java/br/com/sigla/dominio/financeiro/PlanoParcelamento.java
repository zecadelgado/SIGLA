package br.com.sigla.dominio.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record PlanoParcelamento(
        String id,
        String customerId,
        BigDecimal totalAmount,
        int totalInstallments,
        int paidInstallments,
        InstallmentStatus status,
        LocalDate nextDueDate
) {
    public PlanoParcelamento {
        id = requireText(id, "id");
        customerId = requireText(customerId, "customerId");
        totalAmount = requireAmount(totalAmount);
        if (totalInstallments <= 0) {
            throw new IllegalArgumentException("totalInstallments must be greater than zero");
        }
        if (paidInstallments < 0 || paidInstallments > totalInstallments) {
            throw new IllegalArgumentException("paidInstallments must be between zero and totalInstallments");
        }
        status = Objects.requireNonNull(status, "status is required");
        nextDueDate = Objects.requireNonNull(nextDueDate, "nextDueDate is required");
    }

    public int pendingInstallments() {
        return totalInstallments - paidInstallments;
    }

    public boolean isOverdue(LocalDate referenceDate) {
        Objects.requireNonNull(referenceDate, "referenceDate is required");
        return status != InstallmentStatus.PAID && nextDueDate.isBefore(referenceDate);
    }

    public enum InstallmentStatus {
        ACTIVE,
        PAID,
        OVERDUE,
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
        Objects.requireNonNull(value, "totalAmount is required");
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("totalAmount must be greater than zero");
        }
        return value;
    }
}

