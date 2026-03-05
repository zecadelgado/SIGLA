package br.com.sigla.infrastructure.persistence.repository;

import br.com.sigla.application.inventory.port.out.InventoryRepository;
import br.com.sigla.domain.inventory.InventoryItem;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnMissingBean(SpringDataInventoryRepository.class)
public class InMemoryInventoryRepositoryAdapter implements InventoryRepository {

    private final Map<String, InventoryItem> storage = new ConcurrentHashMap<>();

    @Override
    public Optional<InventoryItem> findBySku(String sku) {
        return Optional.ofNullable(storage.get(sku));
    }

    @Override
    public void save(InventoryItem item) {
        storage.put(item.sku(), item);
    }
}
