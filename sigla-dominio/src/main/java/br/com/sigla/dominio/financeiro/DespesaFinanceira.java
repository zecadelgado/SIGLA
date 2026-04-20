package br.com.sigla.dominio.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record DespesaFinanceira(
        String id,
        ExpenseCategory category,
        BigDecimal amount,
        LocalDate expenseDate,
        String responsible,
        String description,
        LocalDate dueDate,
        LocalDate paymentDate,
        String paymentMethod,
        String createdBy,
        String orderReference,
        ExpenseStatus status,
        String notes
) {
    public DespesaFinanceira {
        id = requireText(id, "id");
        category = Objects.requireNonNull(category, "category is required");
        amount = requireAmount(amount);
        expenseDate = Objects.requireNonNull(expenseDate, "expenseDate is required");
        responsible = requireText(responsible, "responsible");
        description = requireText(description, "description");
        dueDate = dueDate == null ? expenseDate : dueDate;
        if (paymentDate != null && paymentDate.isBefore(expenseDate)) {
            throw new IllegalArgumentException("paymentDate must not be before expenseDate");
        }
        paymentMethod = normalizeOptional(paymentMethod);
        createdBy = normalizeOptional(createdBy);
        orderReference = normalizeOptional(orderReference);
        status = Objects.requireNonNull(status, "status is required");
        notes = normalizeOptional(notes);
    }

    public DespesaFinanceira(
            String id,
            ExpenseCategory category,
            BigDecimal amount,
            LocalDate expenseDate,
            String responsible,
            String notes
    ) {
        this(
                id,
                category,
                amount,
                expenseDate,
                responsible,
                category.name(),
                expenseDate,
                null,
                "",
                responsible,
                null,
                ExpenseStatus.PAID,
                notes
        );
    }

    public enum ExpenseCategory {
        FOOD,
        FUEL,
        PRODUCTS,
        EXTRAS
    }

    public enum ExpenseStatus {
        PENDING,
        PAID,
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

