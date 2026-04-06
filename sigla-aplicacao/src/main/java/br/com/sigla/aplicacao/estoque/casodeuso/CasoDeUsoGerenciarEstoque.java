package br.com.sigla.aplicacao.estoque.casodeuso;

import br.com.sigla.aplicacao.estoque.porta.entrada.CasoDeUsoEstoque;
import br.com.sigla.aplicacao.estoque.porta.saida.RepositorioEstoque;
import br.com.sigla.dominio.estoque.ItemEstoque;
import org.springframework.stereotype.Service;

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
                command.quantity(),
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
                command.handledBy(),
                command.purchasedBy(),
                command.storedBy(),
                command.notes()
        ));
        repository.save(item);
    }

    @Override
    public List<ItemEstoque> listAll() {
        return repository.findAll();
    }
}

