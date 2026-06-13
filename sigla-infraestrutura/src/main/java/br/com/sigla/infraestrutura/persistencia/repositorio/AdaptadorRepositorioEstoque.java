package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.estoque.porta.saida.RepositorioEstoque;
import br.com.sigla.dominio.estoque.ItemEstoque;
import br.com.sigla.infraestrutura.persistencia.PersistenciaIds;
import br.com.sigla.infraestrutura.persistencia.entidade.ItemEstoqueEntidade;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class AdaptadorRepositorioEstoque implements RepositorioEstoque {

    private final SpringDataRepositorioEstoque repository;

    public AdaptadorRepositorioEstoque(SpringDataRepositorioEstoque repository) {
        this.repository = repository;
    }

    @Override
    @CacheEvict(value = "ref.produtos", allEntries = true)
    public void save(ItemEstoque item) {
        repository.save(toEntity(item));
    }

    @Override
    @Cacheable("ref.produtos")
    @Transactional(readOnly = true)
    public List<ItemEstoque> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ItemEstoque> findById(String id) {
        return repository.findById(PersistenciaIds.toUuid(id)).map(this::toDomain);
    }

    private ItemEstoque toDomain(ItemEstoqueEntidade entity) {
        return new ItemEstoque(
                PersistenciaIds.toString(entity.getId()),
                entity.getName(),
                entity.getDescription(),
                entity.getSku(),
                entity.getCostPrice(),
                entity.getSalePrice(),
                entity.getQuantity(),
                entity.getMinimumQuantity(),
                entity.getUnit(),
                entity.isAtivo(),
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
                                PersistenciaIds.toString(movement.getFuncionarioId()),
                                movement.getQuemPegou(),
                                movement.getQuemComprou(),
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
        entity.setSku(item.sku());
        entity.setCostPrice(item.costPrice());
        entity.setSalePrice(item.salePrice());
        entity.setQuantity(item.quantity());
        entity.setMinimumQuantity(item.minimumQuantity());
        entity.setUnit(item.unit());
        entity.setAtivo(item.ativo());
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
            embeddable.setFuncionarioId(PersistenciaIds.toUuid(movement.funcionarioId()));
            embeddable.setOrderReference(PersistenciaIds.toUuid(movement.orderReference()));
            embeddable.setDestinationDescription(movement.destinationDescription());
            embeddable.setQuemPegou(movement.quemPegou());
            embeddable.setQuemComprou(movement.quemComprou());
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
        return ItemEstoque.MovementType.from(value);
    }

    @Override
    public boolean existsActiveSku(String sku, String exceptId) {
        String normalized = sku == null ? "" : sku.trim().toLowerCase();
        if (normalized.isBlank()) {
            return false;
        }
        return repository.existsActiveSku(normalized, PersistenciaIds.toUuidIfValid(exceptId));
    }

    @Override
    public boolean existsMovementForOrder(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            return false;
        }
        UUID orderUuid = PersistenciaIds.toUuid(orderId);
        if (orderUuid == null) {
            return false;
        }
        return repository.existsMovementForOrder(orderUuid);
    }
}

@Repository
@Profile("memoria")
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

    @Override
    public boolean existsActiveSku(String sku, String exceptId) {
        String normalized = sku == null ? "" : sku.trim();
        return storage.values().stream()
                .filter(ItemEstoque::ativo)
                .filter(item -> !item.id().equals(exceptId))
                .anyMatch(item -> item.sku().equalsIgnoreCase(normalized));
    }

    @Override
    public boolean existsMovementForOrder(String orderId) {
        return storage.values().stream()
                .flatMap(item -> item.movements().stream())
                .anyMatch(movement -> movement.orderReference().equals(orderId));
    }
}

interface SpringDataRepositorioEstoque extends JpaRepository<ItemEstoqueEntidade, UUID> {

    @Query(value = """
            select exists(
                select 1 from produtos p
                where p.ativo = true
                  and (cast(:exceptId as uuid) is null or p.id <> cast(:exceptId as uuid))
                  and lower(trim(coalesce(p.sku, ''))) = :sku
            )
            """, nativeQuery = true)
    boolean existsActiveSku(@Param("sku") String sku, @Param("exceptId") UUID exceptId);

    @Query(value = """
            select exists(
                select 1 from estoque_movimentacoes m
                where m.ordem_servico_id = :orderId
            )
            """, nativeQuery = true)
    boolean existsMovementForOrder(@Param("orderId") UUID orderId);
}

