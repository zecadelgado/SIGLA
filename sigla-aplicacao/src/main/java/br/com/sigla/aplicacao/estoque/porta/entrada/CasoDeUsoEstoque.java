package br.com.sigla.aplicacao.estoque.porta.entrada;

import br.com.sigla.dominio.estoque.ItemEstoque;

import java.time.LocalDate;
import java.util.List;

public interface CasoDeUsoEstoque {

    void registerItem(RegisterItemEstoqueCommand command);

    void recordMovement(RecordInventoryMovementCommand command);

    List<ItemEstoque> listAll();

    record RegisterItemEstoqueCommand(
            String id,
            String name,
            int quantity,
            String unit
    ) {
    }

    record RecordInventoryMovementCommand(
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
    }
}

