package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.estoque.porta.saida.RepositorioEstoque;
import br.com.sigla.dominio.estoque.ItemEstoque;
import br.com.sigla.infraestrutura.persistencia.PersistenciaIds;
import br.com.sigla.infraestrutura.persistencia.entidade.EstoqueMovimentacaoEntidade;
import br.com.sigla.infraestrutura.persistencia.entidade.ItemEstoqueEntidade;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class AdaptadorRepositorioEstoque implements RepositorioEstoque {

    private final SpringDataRepositorioEstoque repository;
    private final SpringDataRepositorioEstoqueMovimentacao movimentacoes;

    public AdaptadorRepositorioEstoque(SpringDataRepositorioEstoque repository,
                                       SpringDataRepositorioEstoqueMovimentacao movimentacoes) {
        this.repository = repository;
        this.movimentacoes = movimentacoes;
    }

    /**
     * Persiste apenas dados cadastrais. O saldo materializado pertence a razao
     * de movimentos (trigger no banco); regrava-lo aqui reintroduziria a
     * leitura-modificacao-gravacao nao atomica que a Fase 4 eliminou.
     */
    @Override
    @Transactional
    @CacheEvict(value = "ref.produtos", allEntries = true)
    public void save(ItemEstoque item) {
        UUID id = PersistenciaIds.toUuid(item.id());
        int atualizados = repository.atualizarCadastro(
                id,
                item.name(),
                item.description(),
                item.sku(),
                item.unit(),
                item.costPrice(),
                item.salePrice(),
                item.minimumQuantity(),
                item.ativo());
        if (atualizados == 0) {
            ItemEstoqueEntidade entity = new ItemEstoqueEntidade();
            entity.setId(id);
            entity.setName(item.name());
            entity.setDescription(item.description());
            entity.setSku(item.sku());
            entity.setCostPrice(item.costPrice());
            entity.setSalePrice(item.salePrice());
            entity.setQuantity(item.quantity());
            entity.setMinimumQuantity(item.minimumQuantity());
            entity.setUnit(item.unit());
            entity.setAtivo(item.ativo());
            repository.save(entity);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "ref.produtos", allEntries = true)
    public void registrarMovimento(ItemEstoque item, ItemEstoque.InventoryMovement movimento) {
        EstoqueMovimentacaoEntidade entidade = new EstoqueMovimentacaoEntidade();
        entidade.setId(PersistenciaIds.toUuid(movimento.id()));
        entidade.setProdutoId(PersistenciaIds.toUuid(item.id()));
        entidade.setTipo(movimento.type().name());
        entidade.setQuantidade(movimento.amount());
        entidade.setQuantidadeDecimal(movimento.amount());
        entidade.setDataMovimentacao(movimento.occurredOn().atStartOfDay());
        entidade.setValorUnitario(movimento.unitPrice());
        entidade.setValorTotal(movimento.totalPrice());
        entidade.setUsuarioId(PersistenciaIds.toUuidIfValid(movimento.createdBy()));
        entidade.setClienteId(PersistenciaIds.toUuid(movimento.customerId()));
        entidade.setFuncionarioId(PersistenciaIds.toUuid(movimento.funcionarioId()));
        entidade.setOrdemServicoId(PersistenciaIds.toUuid(movimento.orderReference()));
        entidade.setDestinoDescricao(movimento.destinationDescription());
        entidade.setQuemPegou(movimento.quemPegou());
        entidade.setQuemComprou(movimento.quemComprou());
        entidade.setObservacoes(movimento.notes());
        entidade.setChaveIdempotencia(movimento.id());
        movimentacoes.save(entidade);
        movimentacoes.flush();
    }

    @Override
    @Cacheable("ref.produtos")
    @Transactional(readOnly = true)
    public List<ItemEstoque> findAll() {
        Map<UUID, List<EstoqueMovimentacaoEntidade>> porProduto = movimentacoes.findAll().stream()
                .collect(Collectors.groupingBy(EstoqueMovimentacaoEntidade::getProdutoId));
        return repository.findAll().stream()
                .map(entity -> toDomain(entity, porProduto.getOrDefault(entity.getId(), List.of())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ItemEstoque> findById(String id) {
        UUID uuid = PersistenciaIds.toUuid(id);
        return repository.findById(uuid)
                .map(entity -> toDomain(entity, movimentacoes.findByProdutoIdOrderByDataMovimentacaoAsc(uuid)));
    }

    private ItemEstoque toDomain(ItemEstoqueEntidade entity, List<EstoqueMovimentacaoEntidade> movimentos) {
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
                movimentos.stream()
                        .map(movement -> new ItemEstoque.InventoryMovement(
                                PersistenciaIds.toString(movement.getId()),
                                parseMovementType(movement.getTipo()),
                                movement.getQuantidadeDecimal() != null
                                        ? movement.getQuantidadeDecimal() : movement.getQuantidade(),
                                movement.getDataMovimentacao().toLocalDate(),
                                movement.getValorUnitario(),
                                movement.getValorTotal(),
                                PersistenciaIds.toString(movement.getUsuarioId()),
                                PersistenciaIds.toString(movement.getClienteId()),
                                PersistenciaIds.toString(movement.getOrdemServicoId()),
                                movement.getDestinoDescricao(),
                                PersistenciaIds.toString(movement.getFuncionarioId()),
                                movement.getQuemPegou(),
                                movement.getQuemComprou(),
                                movement.getObservacoes()
                        ))
                        .toList()
        );
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
        return movimentacoes.existsByOrdemServicoId(orderUuid);
    }
}

@Repository
@Profile("memoria")
class InMemoryAdaptadorRepositorioEstoque implements RepositorioEstoque {

    private final Map<String, ItemEstoque> storage = new ConcurrentHashMap<>();

    @Override
    public void save(ItemEstoque item) {
        ItemEstoque atual = storage.get(item.id());
        if (atual == null) {
            storage.put(item.id(), item);
            return;
        }
        // Espelha o contrato do adaptador JPA: edicao cadastral preserva o
        // saldo e a razao de movimentos existentes.
        storage.put(item.id(), new ItemEstoque(
                item.id(), item.name(), item.description(), item.sku(),
                item.costPrice(), item.salePrice(),
                atual.quantity(), item.minimumQuantity(), item.unit(), item.ativo(),
                atual.movements()));
    }

    @Override
    public void registrarMovimento(ItemEstoque item, ItemEstoque.InventoryMovement movimento) {
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

    @Modifying
    @Query(value = """
            update produtos set
                nome = :nome,
                descricao = :descricao,
                sku = :sku,
                unidade = :unidade,
                valor_custo = :valorCusto,
                valor_venda = :valorVenda,
                quantidade_minima = :quantidadeMinima,
                quantidade_minima_decimal = :quantidadeMinima,
                ativo = :ativo,
                updated_at = now()
            where id = :id
            """, nativeQuery = true)
    int atualizarCadastro(@Param("id") UUID id,
                          @Param("nome") String nome,
                          @Param("descricao") String descricao,
                          @Param("sku") String sku,
                          @Param("unidade") String unidade,
                          @Param("valorCusto") BigDecimal valorCusto,
                          @Param("valorVenda") BigDecimal valorVenda,
                          @Param("quantidadeMinima") BigDecimal quantidadeMinima,
                          @Param("ativo") boolean ativo);

    @Query(value = """
            select exists(
                select 1 from produtos p
                where p.ativo = true
                  and (cast(:exceptId as uuid) is null or p.id <> cast(:exceptId as uuid))
                  and lower(trim(coalesce(p.sku, ''))) = :sku
            )
            """, nativeQuery = true)
    boolean existsActiveSku(@Param("sku") String sku, @Param("exceptId") UUID exceptId);
}

interface SpringDataRepositorioEstoqueMovimentacao extends JpaRepository<EstoqueMovimentacaoEntidade, UUID> {

    List<EstoqueMovimentacaoEntidade> findByProdutoIdOrderByDataMovimentacaoAsc(UUID produtoId);

    boolean existsByOrdemServicoId(UUID ordemServicoId);
}
