package br.com.sigla.aplicacao.estoque.casodeuso;

import br.com.sigla.aplicacao.estoque.porta.entrada.CasoDeUsoEstoque;
import br.com.sigla.aplicacao.estoque.porta.saida.RepositorioEstoque;
import br.com.sigla.dominio.estoque.ItemEstoque;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class CasoDeUsoGerenciarEstoque implements CasoDeUsoEstoque {

    private static final Set<String> UNIDADES = Set.of("un", "litro", "kg", "caixa", "pacote", "frasco");

    private final RepositorioEstoque repository;

    public CasoDeUsoGerenciarEstoque(RepositorioEstoque repository) {
        this.repository = repository;
    }

    @Override
    public void registerItem(RegisterItemEstoqueCommand command) {
        ItemEstoque item = toItem(command, List.of());
        validarProduto(item, true);
        repository.save(item);
    }

    @Override
    public void updateItem(RegisterItemEstoqueCommand command) {
        ItemEstoque atual = repository.findById(command.id())
                .orElseThrow(() -> new IllegalArgumentException("Produto nao encontrado."));
        ItemEstoque item = toItem(command, atual.movements());
        validarProduto(item, false);
        repository.save(item);
    }

    @Override
    public void inativarItem(String id) {
        ItemEstoque atual = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produto nao encontrado."));
        repository.save(new ItemEstoque(
                atual.id(),
                atual.name(),
                atual.description(),
                atual.sku(),
                atual.costPrice(),
                atual.salePrice(),
                atual.quantity(),
                atual.minimumQuantity(),
                atual.unit(),
                false,
                atual.movements()
        ));
    }

    private ItemEstoque toItem(RegisterItemEstoqueCommand command, List<ItemEstoque.InventoryMovement> movimentos) {
        return new ItemEstoque(
                command.id(),
                command.name(),
                command.description(),
                command.sku(),
                normalizeMoney(command.costPrice()),
                normalizeMoney(command.salePrice()),
                command.quantity(),
                command.minimumQuantity(),
                command.unit(),
                command.ativo(),
                movimentos
        );
    }

    @Override
    public void recordMovement(RecordInventoryMovementCommand command) {
        ItemEstoque item = repository.findById(command.itemId())
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found: " + command.itemId()));
        if (!item.ativo()) {
            throw new IllegalArgumentException("Produto inativo nao pode receber nova movimentacao.");
        }
        ItemEstoque.MovementType type = command.type() == null
                ? ItemEstoque.MovementType.SAIDA
                : ItemEstoque.MovementType.from(command.type().name());
        if (type.decreasesStock() && item.quantity() < command.amount()) {
            throw new IllegalArgumentException("Saldo insuficiente para movimentacao de estoque.");
        }
        BigDecimal unitPrice = normalizeMoney(command.unitPrice());
        item.recordMovement(new ItemEstoque.InventoryMovement(
                command.movementId(),
                type,
                command.amount(),
                command.occurredOn(),
                unitPrice,
                unitPrice.multiply(BigDecimal.valueOf(command.amount())),
                command.createdBy(),
                command.customerId(),
                command.orderReference(),
                command.destinationDescription(),
                command.funcionarioId(),
                command.quemPegou(),
                command.quemComprou(),
                command.notes()
        ));
        repository.save(item);
    }

    @Override
    public List<ItemEstoque> listAll() {
        return repository.findAll();
    }

    @Override
    public List<ItemEstoque> listLowStock() {
        return repository.findAll().stream().filter(ItemEstoque::isLowStock).toList();
    }

    @Override
    public List<InventoryMovementView> listMovements() {
        return repository.findAll().stream()
                .flatMap(item -> item.movements().stream().map(movement -> new InventoryMovementView(
                        item.id(),
                        item.name(),
                        movement.id(),
                        movement.type(),
                        movement.amount(),
                        movement.occurredOn(),
                        movement.unitPrice(),
                        movement.totalPrice(),
                        movement.createdBy(),
                        movement.customerId(),
                        movement.orderReference(),
                        movement.destinationDescription(),
                        movement.funcionarioId(),
                        movement.quemPegou(),
                        movement.quemComprou(),
                        movement.notes()
                )))
                .toList();
    }

    private void validarProduto(ItemEstoque item, boolean novo) {
        if (!UNIDADES.contains(item.unit().toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Unidade invalida.");
        }
        if (!item.sku().isBlank() && !item.sku().matches("[A-Za-z0-9._-]{2,40}")) {
            throw new IllegalArgumentException("SKU invalido.");
        }
        if (repository.existsActiveSku(item.sku(), item.id())) {
            throw new IllegalArgumentException("SKU ja cadastrado em outro produto ativo.");
        }
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

