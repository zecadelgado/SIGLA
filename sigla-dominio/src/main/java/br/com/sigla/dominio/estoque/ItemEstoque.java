package br.com.sigla.dominio.estoque;

import br.com.sigla.dominio.compartilhado.ExcecaoDominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ItemEstoque {

    private final String id;
    private final String name;
    private final String description;
    private final BigDecimal costPrice;
    private final BigDecimal salePrice;
    private final int minimumQuantity;
    private final String unit;
    private final List<InventoryMovement> movements;
    private int quantity;

    public ItemEstoque(
            String id,
            String name,
            String description,
            BigDecimal costPrice,
            BigDecimal salePrice,
            int quantity,
            int minimumQuantity,
            String unit,
            List<InventoryMovement> movements
    ) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.description = normalizeOptional(description);
        this.costPrice = requireMoney(costPrice, "costPrice");
        this.salePrice = requireMoney(salePrice, "salePrice");
        if (minimumQuantity < 0) {
            throw new ExcecaoDominio("Minimum quantity cannot be negative");
        }
        this.minimumQuantity = minimumQuantity;
        this.unit = requireText(unit, "unit");
        if (quantity < 0) {
            throw new ExcecaoDominio("Initial quantity cannot be negative");
        }
        this.quantity = quantity;
        this.movements = new ArrayList<>(Objects.requireNonNullElse(movements, List.of()));
    }

    public ItemEstoque(String id, String name, int quantity, String unit, List<InventoryMovement> movements) {
        this(id, name, "", BigDecimal.ZERO, BigDecimal.ZERO, quantity, 0, unit, movements);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public BigDecimal costPrice() {
        return costPrice;
    }

    public BigDecimal salePrice() {
        return salePrice;
    }

    public int quantity() {
        return quantity;
    }

    public String unit() {
        return unit;
    }

    public int minimumQuantity() {
        return minimumQuantity;
    }

    public List<InventoryMovement> movements() {
        return List.copyOf(movements);
    }

    public boolean isLowStock() {
        return quantity <= minimumQuantity;
    }

    public void recordMovement(InventoryMovement movement) {
        Objects.requireNonNull(movement, "movement is required");
        if (movement.type() == MovementType.INBOUND) {
            quantity += movement.amount();
        } else {
            if (quantity - movement.amount() < 0) {
                throw new ExcecaoDominio("Insufficient stock for item " + id);
            }
            quantity -= movement.amount();
        }
        movements.add(movement);
    }

    public record InventoryMovement(
            String id,
            MovementType type,
            int amount,
            LocalDate occurredOn,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            String createdBy,
            String customerId,
            String orderReference,
            String destinationDescription,
            String notes
    ) {
        public InventoryMovement {
            id = requireText(id, "id");
            type = Objects.requireNonNull(type, "type is required");
            if (amount <= 0) {
                throw new ExcecaoDominio("Movement amount must be greater than zero");
            }
            occurredOn = Objects.requireNonNull(occurredOn, "occurredOn is required");
            unitPrice = requireMoney(unitPrice, "unitPrice");
            totalPrice = requireMoney(totalPrice, "totalPrice");
            createdBy = normalizeOptional(createdBy);
            customerId = normalizeOptional(customerId);
            orderReference = normalizeOptional(orderReference);
            destinationDescription = normalizeOptional(destinationDescription);
            notes = normalizeOptional(notes);
        }

        public InventoryMovement(
                String id,
                MovementType type,
                int amount,
                LocalDate occurredOn,
                String handledBy,
                String purchasedBy,
                String storedBy,
                String notes
        ) {
            this(
                    id,
                    type,
                    amount,
                    occurredOn,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    handledBy,
                    purchasedBy,
                    null,
                    storedBy,
                    notes
            );
        }
    }

    public enum MovementType {
        INBOUND,
        OUTBOUND
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }

    private static BigDecimal requireMoney(BigDecimal value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " is required");
        if (value.signum() < 0) {
            throw new ExcecaoDominio(fieldName + " cannot be negative");
        }
        return value;
    }
}

