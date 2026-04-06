package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.estoque.porta.saida.RepositorioEstoque;
import br.com.sigla.dominio.estoque.ItemEstoque;
import br.com.sigla.infraestrutura.persistencia.entidade.ItemEstoqueEntidade;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        return repository.findById(id).map(this::toDomain);
    }

    private ItemEstoque toDomain(ItemEstoqueEntidade entity) {
        return new ItemEstoque(
                entity.getId(),
                entity.getName(),
                entity.getQuantity(),
                entity.getUnit(),
                entity.getMovements().stream()
                        .map(movement -> new ItemEstoque.InventoryMovement(
                                movement.getId(),
                                movement.getType(),
                                movement.getAmount(),
                                movement.getOccurredOn(),
                                movement.getHandledBy(),
                                movement.getPurchasedBy(),
                                movement.getStoredBy(),
                                movement.getNotes()
                        ))
                        .toList()
        );
    }

    private ItemEstoqueEntidade toEntity(ItemEstoque item) {
        ItemEstoqueEntidade entity = new ItemEstoqueEntidade();
        entity.setId(item.id());
        entity.setName(item.name());
        entity.setQuantity(item.quantity());
        entity.setUnit(item.unit());
        List<ItemEstoqueEntidade.MovementEmbeddable> movements = new ArrayList<>();
        for (ItemEstoque.InventoryMovement movement : item.movements()) {
            ItemEstoqueEntidade.MovementEmbeddable embeddable = new ItemEstoqueEntidade.MovementEmbeddable();
            embeddable.setId(movement.id());
            embeddable.setType(movement.type());
            embeddable.setAmount(movement.amount());
            embeddable.setOccurredOn(movement.occurredOn());
            embeddable.setHandledBy(movement.handledBy());
            embeddable.setPurchasedBy(movement.purchasedBy());
            embeddable.setStoredBy(movement.storedBy());
            embeddable.setNotes(movement.notes());
            movements.add(embeddable);
        }
        entity.setMovements(movements);
        return entity;
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

interface SpringDataRepositorioEstoque extends JpaRepository<ItemEstoqueEntidade, String> {
}

