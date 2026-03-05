package br.com.sigla.infrastructure.persistence.repository;

import br.com.sigla.application.inventory.port.out.InventoryRepository;
import br.com.sigla.domain.inventory.InventoryItem;
import br.com.sigla.infrastructure.persistence.mapper.InventoryEntityMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@ConditionalOnBean(SpringDataInventoryRepository.class)
public class InventoryRepositoryAdapter implements InventoryRepository {

    private final SpringDataInventoryRepository repository;
    private final InventoryEntityMapper mapper;

    public InventoryRepositoryAdapter(SpringDataInventoryRepository repository, InventoryEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<InventoryItem> findBySku(String sku) {
        return repository.findById(sku).map(mapper::toDomain);
    }

    @Override
    public void save(InventoryItem item) {
        repository.save(mapper.toEntity(item));
    }
}
