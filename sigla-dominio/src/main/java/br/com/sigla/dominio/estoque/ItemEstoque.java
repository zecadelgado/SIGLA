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
    private final String sku;
    private final BigDecimal costPrice;
    private final BigDecimal salePrice;
    private final int minimumQuantity;
    private final String unit;
    private final boolean ativo;
    private final List<InventoryMovement> movements;
    private int quantity;

    public ItemEstoque(
            String id,
            String name,
            String description,
            String sku,
            BigDecimal costPrice,
            BigDecimal salePrice,
            int quantity,
            int minimumQuantity,
            String unit,
            boolean ativo,
            List<InventoryMovement> movements
    ) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.description = normalizeOptional(description);
        this.sku = normalizeOptional(sku);
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
        this.ativo = ativo;
        this.movements = new ArrayList<>(Objects.requireNonNullElse(movements, List.of()));
    }

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
        this(id, name, description, "", costPrice, salePrice, quantity, minimumQuantity, unit, true, movements);
    }

    public ItemEstoque(String id, String name, int quantity, String unit, List<InventoryMovement> movements) {
        this(id, name, "", "", BigDecimal.ZERO, BigDecimal.ZERO, quantity, 0, unit, true, movements);
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

    public String sku() {
        return sku;
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

    public boolean ativo() {
        return ativo;
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
        if (movement.type().increasesStock()) {
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
        String funcionarioId,
        String quemPegou,
        String quemComprou,
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
            funcionarioId = normalizeOptional(funcionarioId);
            quemPegou = normalizeOptional(quemPegou);
            quemComprou = normalizeOptional(quemComprou);
            notes = normalizeOptional(notes);
            totalPrice = unitPrice.multiply(BigDecimal.valueOf(amount));
            if (type == MovementType.AJUSTE && notes.isBlank()) {
                throw new ExcecaoDominio("Adjustment movement requires notes");
            }
        }

        public InventoryMovement(
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
            this(id, type, amount, occurredOn, unitPrice, totalPrice, createdBy, customerId, orderReference, destinationDescription, "", "", "", notes);
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
                    "",
                    handledBy,
                    purchasedBy,
                    notes
            );
        }
    }

    public enum MovementType {
        INBOUND,
        OUTBOUND,
        ENTRADA,
        SAIDA,
        AJUSTE,
        COMPRA,
        USO_OS;

        public boolean increasesStock() {
            return this == INBOUND || this == ENTRADA || this == COMPRA;
        }

        public boolean decreasesStock() {
            return this == OUTBOUND || this == SAIDA || this == USO_OS || this == AJUSTE;
        }

        public static MovementType from(String value) {
            if (value == null || value.isBlank()) {
                return SAIDA;
            }
            String normalized = value.trim().toUpperCase().replace('-', '_');
            return switch (normalized) {
                case "INBOUND", "ENTRADA" -> ENTRADA;
                case "OUTBOUND", "SAIDA", "SAÍDA" -> SAIDA;
                case "AJUSTE" -> AJUSTE;
                case "COMPRA" -> COMPRA;
                case "USO_OS", "USO_EM_OS" -> USO_OS;
                default -> valueOf(normalized);
            };
        }
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

