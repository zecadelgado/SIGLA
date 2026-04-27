package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.estoque.porta.saida.RepositorioEstoque;
import br.com.sigla.dominio.estoque.ItemEstoque;
import br.com.sigla.infraestrutura.persistencia.PersistenciaIds;
import br.com.sigla.infraestrutura.persistencia.entidade.ItemEstoqueEntidade;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnBean(SpringDataRepositorioEstoque.class)
public class AdaptadorRepositorioEstoque implements RepositorioEstoque {

    private final SpringDataRepositorioEstoque repository;

    public AdaptadorRepositorioEstoque(SpringDataRepositorioEstoque repository) {
        this.repository = repository;
    }

    @Override
    public void save(ItemEstoque item) {
        repository.save(toEntity(item));
    }

    @Override
    public List<ItemEstoque> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<ItemEstoque> findById(String id) {
        return repository.findById(PersistenciaIds.toUuid(id)).map(this::toDomain);
    }

    private ItemEstoque toDomain(ItemEstoqueEntidade entity) {
        return new ItemEstoque(
                PersistenciaIds.toString(entity.getId()),
                entity.getName(),
                entity.getDescription(),
                entity.getCostPrice(),
                entity.getSalePrice(),
                entity.getQuantity(),
                entity.getMinimumQuantity(),
                entity.getUnit(),
                entity.getMovements().stream()
                        .map(movement -> new ItemEstoque.InventoryMovement(
                                PersistenciaIds.toString(movement.getId()),
                                parseMovementType(movement.getType()),
                                movement.getAmount(),
                                movement.getOccurredOn().toLocalDate(),
                                movement.getUnitPrice(),
                                movement.getTotalPrice(),
                                PersistenciaIds.toString(movement.getCreatedBy()),
                                PersistenciaIds.toString(movement.getCustomerId()),
                                PersistenciaIds.toString(movement.getOrderReference()),
                                movement.getDestinationDescription(),
                                movement.getNotes()
                        ))
                        .toList()
        );
    }

    private ItemEstoqueEntidade toEntity(ItemEstoque item) {
        ItemEstoqueEntidade entity = new ItemEstoqueEntidade();
        entity.setId(PersistenciaIds.toUuid(item.id()));
        entity.setName(item.name());
        entity.setDescription(item.description());
        entity.setCostPrice(item.costPrice());
        entity.setSalePrice(item.salePrice());
        entity.setQuantity(item.quantity());
        entity.setMinimumQuantity(item.minimumQuantity());
        entity.setUnit(item.unit());
        List<ItemEstoqueEntidade.MovementEmbeddable> movements = new ArrayList<>();
        for (ItemEstoque.InventoryMovement movement : item.movements()) {
            ItemEstoqueEntidade.MovementEmbeddable embeddable = new ItemEstoqueEntidade.MovementEmbeddable();
            embeddable.setId(PersistenciaIds.toUuid(movement.id()));
            embeddable.setType(movement.type().name());
            embeddable.setAmount(movement.amount());
            embeddable.setOccurredOn(movement.occurredOn().atStartOfDay());
            embeddable.setUnitPrice(movement.unitPrice());
            embeddable.setTotalPrice(movement.totalPrice());
            embeddable.setCreatedBy(PersistenciaIds.toUuidIfValid(movement.createdBy()));
            embeddable.setCustomerId(PersistenciaIds.toUuid(movement.customerId()));
            embeddable.setOrderReference(PersistenciaIds.toUuid(movement.orderReference()));
            embeddable.setDestinationDescription(movement.destinationDescription());
            embeddable.setNotes(movement.notes());
            movements.add(embeddable);
        }
        entity.setMovements(movements);
        return entity;
    }

    private ItemEstoque.MovementType parseMovementType(String value) {
        if (value == null || value.isBlank()) {
            return ItemEstoque.MovementType.OUTBOUND;
        }
        return ItemEstoque.MovementType.valueOf(value.trim().toUpperCase());
    }
}

@Repository
@ConditionalOnMissingBean(SpringDataRepositorioEstoque.class)
class InMemoryAdaptadorRepositorioEstoque implements RepositorioEstoque {

    private final Map<String, ItemEstoque> storage = new ConcurrentHashMap<>();

    @Override
    public void save(ItemEstoque item) {
        storage.put(item.id(), item);
    }

    @Override
    public List<ItemEstoque> findAll() {
        return storage.values().stream().toList();
    }

    @Override
    public Optional<ItemEstoque> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }
}

interface SpringDataRepositorioEstoque extends JpaRepository<ItemEstoqueEntidade, UUID> {
}

