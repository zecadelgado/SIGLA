package br.com.sigla.application.inventory.usecase;

import br.com.sigla.application.inventory.dto.InventoryMovementCommand;
import br.com.sigla.application.inventory.port.in.InventoryUseCase;
import br.com.sigla.application.inventory.port.out.InventoryRepository;
import br.com.sigla.domain.inventory.InventoryItem;
import org.springframework.stereotype.Service;

@Service
public class AdjustInventoryUseCase implements InventoryUseCase {

    private final InventoryRepository repository;

    public AdjustInventoryUseCase(InventoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public void addStock(InventoryMovementCommand command) {
        InventoryItem item = repository.findBySku(command.sku())
                .orElseGet(() -> new InventoryItem(command.sku(), command.description(), 0));
        item.addStock(command.amount());
        repository.save(item);
    }

    @Override
    public void removeStock(InventoryMovementCommand command) {
        InventoryItem item = repository.findBySku(command.sku())
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found: " + command.sku()));
        item.removeStock(command.amount());
        repository.save(item);
    }
}
