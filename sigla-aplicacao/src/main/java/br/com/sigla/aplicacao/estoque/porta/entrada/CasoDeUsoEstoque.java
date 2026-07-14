package br.com.sigla.aplicacao.estoque.porta.entrada;

import br.com.sigla.dominio.estoque.ItemEstoque;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface CasoDeUsoEstoque {

    void registerItem(RegisterItemEstoqueCommand command);

    void updateItem(RegisterItemEstoqueCommand command);

    void inativarItem(String id);

    void reativarItem(String id);

    void recordMovement(RecordInventoryMovementCommand command);

    List<ItemEstoque> listAll();

    List<InventoryMovementView> listMovements();

    List<ItemEstoque> listLowStock();

    record RegisterItemEstoqueCommand(
            String id,
            String name,
            String description,
            String sku,
            BigDecimal costPrice,
            BigDecimal salePrice,
            BigDecimal quantity,
            BigDecimal minimumQuantity,
            String unit,
            boolean ativo
    ) {
        public RegisterItemEstoqueCommand(
                String id,
                String name,
                String description,
                BigDecimal costPrice,
                BigDecimal salePrice,
                BigDecimal quantity,
                BigDecimal minimumQuantity,
                String unit
        ) {
            this(id, name, description, "", costPrice, salePrice, quantity, minimumQuantity, unit, true);
        }

        public RegisterItemEstoqueCommand(String id, String name, BigDecimal quantity, String unit) {
            this(id, name, "", "", BigDecimal.ZERO, BigDecimal.ZERO, quantity, BigDecimal.ZERO, unit, true);
        }
    }

    record RecordInventoryMovementCommand(
            String itemId,
            String movementId,
            ItemEstoque.MovementType type,
            BigDecimal amount,
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
        public RecordInventoryMovementCommand(
                String itemId,
                String movementId,
                ItemEstoque.MovementType type,
                BigDecimal amount,
                LocalDate occurredOn,
                BigDecimal unitPrice,
                BigDecimal totalPrice,
                String createdBy,
                String customerId,
                String orderReference,
                String destinationDescription,
                String notes
        ) {
            this(itemId, movementId, type, amount, occurredOn, unitPrice, totalPrice, createdBy, customerId, orderReference, destinationDescription, "", "", "", notes);
        }

        public RecordInventoryMovementCommand(
                String itemId,
                String movementId,
                ItemEstoque.MovementType type,
                BigDecimal amount,
                LocalDate occurredOn,
                String handledBy,
                String purchasedBy,
                String storedBy,
                String notes
        ) {
            this(
                    itemId,
                    movementId,
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

    record InventoryMovementView(
            String itemId,
            String itemName,
            String movementId,
            ItemEstoque.MovementType type,
            BigDecimal amount,
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
    }
}

