package br.com.sigla.application.inventory.usecase;

import br.com.sigla.application.inventory.dto.InventoryMovementCommand;
import br.com.sigla.application.inventory.port.out.InventoryRepository;
import br.com.sigla.domain.inventory.InventoryItem;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdjustInventoryUseCaseTest {

    @Test
    void shouldPersistStockAddition() {
        InMemoryInventoryRepository repository = new InMemoryInventoryRepository();
        AdjustInventoryUseCase useCase = new AdjustInventoryUseCase(repository);

        useCase.addStock(new InventoryMovementCommand("AR-100", "Armacao Metal", 3));

        assertEquals(3, repository.findBySku("AR-100").orElseThrow().quantity());
    }

    private static final class InMemoryInventoryRepository implements InventoryRepository {
        private final Map<String, InventoryItem> items = new HashMap<>();

        @Override
        public Optional<InventoryItem> findBySku(String sku) {
            return Optional.ofNullable(items.get(sku));
        }

        @Override
        public void save(InventoryItem item) {
            items.put(item.sku(), item);
        }
    }
}
