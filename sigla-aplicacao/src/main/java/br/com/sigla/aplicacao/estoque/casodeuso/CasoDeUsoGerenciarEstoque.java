package br.com.sigla.aplicacao.estoque.casodeuso;

import br.com.sigla.aplicacao.estoque.porta.entrada.CasoDeUsoEstoque;
import br.com.sigla.aplicacao.estoque.porta.saida.RepositorioEstoque;
import br.com.sigla.dominio.estoque.ItemEstoque;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CasoDeUsoGerenciarEstoque implements CasoDeUsoEstoque {

    private final RepositorioEstoque repository;

    public CasoDeUsoGerenciarEstoque(RepositorioEstoque repository) {
        this.repository = repository;
    }

    @Override
    public void registerItem(RegisterItemEstoqueCommand command) {
        repository.save(new ItemEstoque(
                command.id(),
                command.name(),
                command.description(),
                normalizeMoney(command.costPrice()),
                normalizeMoney(command.salePrice()),
                command.quantity(),
                command.minimumQuantity(),
                command.unit(),
                List.of()
        ));
    }

    @Override
    public void recordMovement(RecordInventoryMovementCommand command) {
        ItemEstoque item = repository.findById(command.itemId())
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found: " + command.itemId()));
        item.recordMovement(new ItemEstoque.InventoryMovement(
                command.movementId(),
                command.type(),
                command.amount(),
                command.occurredOn(),
                normalizeMoney(command.unitPrice()),
                normalizeMoney(command.totalPrice()),
                command.createdBy(),
                command.customerId(),
                command.orderReference(),
                command.destinationDescription(),
                command.notes()
        ));
        repository.save(item);
    }

    @Override
    public List<ItemEstoque> listAll() {
        return repository.findAll();
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
                        movement.notes()
                )))
                .toList();
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

