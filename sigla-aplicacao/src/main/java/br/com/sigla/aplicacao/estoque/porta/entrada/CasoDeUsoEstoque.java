package br.com.sigla.aplicacao.estoque.porta.entrada;

import br.com.sigla.dominio.estoque.ItemEstoque;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface CasoDeUsoEstoque {

    void registerItem(RegisterItemEstoqueCommand command);

    void recordMovement(RecordInventoryMovementCommand command);

    List<ItemEstoque> listAll();

    List<InventoryMovementView> listMovements();

    record RegisterItemEstoqueCommand(
            String id,
            String name,
            String description,
            BigDecimal costPrice,
            BigDecimal salePrice,
            int quantity,
            int minimumQuantity,
            String unit
    ) {
        public RegisterItemEstoqueCommand(String id, String name, int quantity, String unit) {
            this(id, name, "", BigDecimal.ZERO, BigDecimal.ZERO, quantity, 0, unit);
        }
    }

    record RecordInventoryMovementCommand(
            String itemId,
            String movementId,
            ItemEstoque.MovementType type,
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
        public RecordInventoryMovementCommand(
                String itemId,
                String movementId,
                ItemEstoque.MovementType type,
                int amount,
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
                    notes
            );
        }
    }

    record InventoryMovementView(
            String itemId,
            String itemName,
            String movementId,
            ItemEstoque.MovementType type,
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
    }
}

