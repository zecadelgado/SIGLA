package br.com.sigla.application.inventory.port.in;

import br.com.sigla.application.inventory.dto.InventoryMovementCommand;

public interface InventoryUseCase {

    void addStock(InventoryMovementCommand command);

    void removeStock(InventoryMovementCommand command);
}
