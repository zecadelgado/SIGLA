package br.com.sigla.aplicacao.estoque.casodeuso;

import br.com.sigla.aplicacao.estoque.porta.entrada.CasoDeUsoEstoque;
import br.com.sigla.aplicacao.estoque.porta.saida.RepositorioEstoque;
import br.com.sigla.aplicacao.financeiro.porta.entrada.CasoDeUsoFinanceiro;
import br.com.sigla.dominio.estoque.ItemEstoque;
import br.com.sigla.dominio.financeiro.DespesaFinanceira;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class CasoDeUsoGerenciarEstoque implements CasoDeUsoEstoque {

    private static final Set<String> UNIDADES = Set.of("un", "litro", "kg", "caixa", "pacote", "frasco");

    private final RepositorioEstoque repository;
    private final CasoDeUsoFinanceiro casoDeUsoFinanceiro;

    @Autowired
    public CasoDeUsoGerenciarEstoque(RepositorioEstoque repository, CasoDeUsoFinanceiro casoDeUsoFinanceiro) {
        this.repository = repository;
        this.casoDeUsoFinanceiro = casoDeUsoFinanceiro;
    }

    public CasoDeUsoGerenciarEstoque(RepositorioEstoque repository) {
        this.repository = repository;
        this.casoDeUsoFinanceiro = null;
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
        if (command.quantity() != atual.quantity()) {
            throw new IllegalArgumentException("Saldo nao pode ser alterado pela edicao do produto. Registre uma movimentacao de entrada, saida ou ajuste justificado.");
        }
        ItemEstoque item = toItem(command, atual.movements());
        validarProduto(item, false);
        repository.save(item);
    }

    @Override
    public void inativarItem(String id) {
        alterarAtivo(id, false);
    }

    @Override
    public void reativarItem(String id) {
        alterarAtivo(id, true);
    }

    private void alterarAtivo(String id, boolean ativo) {
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
                ativo,
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
        if (item.movements().stream().anyMatch(movement -> movement.id().equals(command.movementId()))) {
            return;
        }
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
        gerarDespesaDaMovimentacao(item, command, type, unitPrice);
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

    /**
     * Reserva e devolucao apenas alteram a disponibilidade do produto; nao representam
     * custo realizado. A despesa e registrada na compra/entrada e na baixa efetiva.
     */
    private void gerarDespesaDaMovimentacao(
            ItemEstoque item,
            RecordInventoryMovementCommand command,
            ItemEstoque.MovementType type,
            BigDecimal unitPrice
    ) {
        if (casoDeUsoFinanceiro == null || !geraCustoFinanceiro(type)) {
            return;
        }
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(command.amount()));
        if (total.signum() <= 0) {
            return;
        }
        String responsavel = primeiroTexto(command.quemComprou(), command.createdBy(), "Sistema");
        casoDeUsoFinanceiro.registerExpense(new CasoDeUsoFinanceiro.RegisterDespesaFinanceiraCommand(
                "estoque-" + command.movementId(),
                DespesaFinanceira.ExpenseCategory.PRODUCTS,
                total,
                command.occurredOn(),
                responsavel,
                descricaoFinanceira(type, item.name()),
                command.occurredOn(),
                null,
                "",
                command.createdBy(),
                "",
                DespesaFinanceira.ExpenseStatus.PENDING,
                "[ESTOQUE] Movimento " + command.movementId()
                        + (command.orderReference() == null || command.orderReference().isBlank()
                        ? "" : " | OS " + command.orderReference())
                        + (command.notes() == null || command.notes().isBlank() ? "" : " | " + command.notes())
        ));
    }

    private boolean geraCustoFinanceiro(ItemEstoque.MovementType type) {
        return switch (type) {
            case COMPRA, ENTRADA, INBOUND, SAIDA, OUTBOUND, AJUSTE -> true;
            case USO_OS, CONSUMO_RESERVA_OS, RESERVA_OS, DEVOLUCAO_RESERVA_OS, ESTORNO_CONSUMO_OS -> false;
        };
    }

    private String descricaoFinanceira(ItemEstoque.MovementType type, String nomeProduto) {
        return switch (type) {
            case COMPRA, ENTRADA, INBOUND -> "Aquisição de estoque: " + nomeProduto;
            case CONSUMO_RESERVA_OS, USO_OS -> "Custo de material aplicado: " + nomeProduto;
            default -> "Baixa de estoque: " + nomeProduto;
        };
    }

    private String primeiroTexto(String... valores) {
        for (String valor : valores) {
            if (valor != null && !valor.isBlank()) {
                return valor.trim();
            }
        }
        return "Sistema";
    }
}

