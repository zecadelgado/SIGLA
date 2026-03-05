package br.com.sigla.application.inventory.port.out;

import br.com.sigla.domain.inventory.InventoryItem;

import java.util.Optional;

public interface InventoryRepository {

    Optional<InventoryItem> findBySku(String sku);

    void save(InventoryItem item);
}
