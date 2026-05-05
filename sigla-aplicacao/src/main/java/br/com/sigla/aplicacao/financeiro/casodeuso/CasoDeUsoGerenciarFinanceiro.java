package br.com.sigla.aplicacao.financeiro.casodeuso;

import br.com.sigla.aplicacao.financeiro.porta.entrada.CasoDeUsoFinanceiro;
import br.com.sigla.aplicacao.financeiro.porta.saida.RepositorioEntradaFinanceira;
import br.com.sigla.aplicacao.financeiro.porta.saida.RepositorioDespesaFinanceira;
import br.com.sigla.aplicacao.financeiro.porta.saida.RepositorioPlanoParcelamento;
import br.com.sigla.dominio.financeiro.EntradaFinanceira;
import br.com.sigla.dominio.financeiro.DespesaFinanceira;
import br.com.sigla.dominio.financeiro.PlanoParcelamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class CasoDeUsoGerenciarFinanceiro implements CasoDeUsoFinanceiro {

    private final RepositorioEntradaFinanceira entryRepository;
    private final RepositorioDespesaFinanceira expenseRepository;
    private final RepositorioPlanoParcelamento installmentPlanRepository;

    public CasoDeUsoGerenciarFinanceiro(
            RepositorioEntradaFinanceira entryRepository,
            RepositorioDespesaFinanceira expenseRepository,
            RepositorioPlanoParcelamento installmentPlanRepository
    ) {
        this.entryRepository = entryRepository;
        this.expenseRepository = expenseRepository;
        this.installmentPlanRepository = installmentPlanRepository;
    }

    @Override
    public void registerEntry(RegisterEntradaFinanceiraCommand command) {
        entryRepository.save(new EntradaFinanceira(
                command.id(),
                command.entryType(),
                command.amount(),
                command.entryDate(),
                command.customerId(),
                command.serviceProvidedId(),
                command.description(),
                command.category(),
                command.dueDate(),
                command.paymentDate(),
                command.paymentMethod(),
                command.createdBy(),
                command.orderReference(),
                command.status()
        ));
    }

    @Override
    public void registerExpense(RegisterDespesaFinanceiraCommand command) {
        expenseRepository.save(new DespesaFinanceira(
                command.id(),
                command.category(),
                command.amount(),
                command.expenseDate(),
                command.responsible(),
                command.description(),
                command.dueDate(),
                command.paymentDate(),
                command.paymentMethod(),
                command.createdBy(),
                command.orderReference(),
                command.status(),
                command.notes()
        ));
    }

    @Override
    public void registerPlanoParcelamento(RegisterPlanoParcelamentoCommand command) {
        installmentPlanRepository.save(new PlanoParcelamento(
                command.id(),
                command.customerId(),
                command.totalAmount(),
                command.totalInstallments(),
                command.paidInstallments(),
                command.status(),
                command.nextDueDate()
        ));
    }

    @Override
    public void registerTransaction(RegisterTransacaoFinanceiraCommand command) {
        if (command.type() == TransactionType.ENTRY) {
            registerEntry(new RegisterEntradaFinanceiraCommand(
                    command.id(),
                    parseEntryType(command.paymentMethod()),
                    command.amount(),
                    command.issueDate(),
                    command.customerId(),
                    command.serviceProvidedId(),
                    command.description(),
                    command.category(),
                    command.dueDate(),
                    command.paymentDate(),
                    command.paymentMethod(),
                    command.createdBy(),
                    command.orderReference(),
                    mapEntryStatus(command.status())
            ));
            if (command.installment() && command.installmentCount() > 1 && command.customerId() != null && !command.customerId().isBlank()) {
                registerPlanoParcelamento(new RegisterPlanoParcelamentoCommand(
                        command.id() + "-plan",
                        command.customerId(),
                        command.amount(),
                        command.installmentCount(),
                        command.status() == TransactionStatus.PAID ? command.installmentCount() : 0,
                        command.status() == TransactionStatus.PAID
                                ? PlanoParcelamento.InstallmentStatus.PAID
                                : PlanoParcelamento.InstallmentStatus.ACTIVE,
                        command.dueDate() == null ? command.issueDate() : command.dueDate()
                ));
            }
            return;
        }

        registerExpense(new RegisterDespesaFinanceiraCommand(
                command.id(),
                parseExpenseCategory(command.category()),
                command.amount(),
                command.issueDate(),
                normalizeResponsible(command.createdBy()),
                command.description(),
                command.dueDate(),
                command.paymentDate(),
                command.paymentMethod(),
                command.createdBy(),
                command.orderReference(),
                mapExpenseStatus(command.status()),
                command.notes()
        ));
    }

    @Override
    public void markPaid(String transactionId, LocalDate paymentDate) {
        LocalDate resolvedPaymentDate = paymentDate == null ? LocalDate.now() : paymentDate;
        EntradaFinanceira entry = entryRepository.findById(transactionId).orElse(null);
        if (entry != null) {
            entryRepository.save(new EntradaFinanceira(
                    entry.id(),
                    entry.entryType(),
                    entry.amount(),
                    entry.entryDate(),
                    entry.customerId(),
                    entry.serviceProvidedId(),
                    entry.description(),
                    entry.category(),
                    entry.dueDate(),
                    resolvedPaymentDate,
                    entry.paymentMethod(),
                    entry.createdBy(),
                    entry.orderReference(),
                    EntradaFinanceira.EntryStatus.RECEIVED
            ));
            return;
        }

        DespesaFinanceira expense = expenseRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transacao nao encontrada."));
        expenseRepository.save(new DespesaFinanceira(
                expense.id(),
                expense.category(),
                expense.amount(),
                expense.expenseDate(),
                expense.responsible(),
                expense.description(),
                expense.dueDate(),
                resolvedPaymentDate,
                expense.paymentMethod(),
                expense.createdBy(),
                expense.orderReference(),
                DespesaFinanceira.ExpenseStatus.PAID,
                expense.notes()
        ));
    }

    @Override
    public void cancel(String transactionId) {
        EntradaFinanceira entry = entryRepository.findById(transactionId).orElse(null);
        if (entry != null) {
            entryRepository.save(new EntradaFinanceira(
                    entry.id(),
                    entry.entryType(),
                    entry.amount(),
                    entry.entryDate(),
                    entry.customerId(),
                    entry.serviceProvidedId(),
                    entry.description(),
                    entry.category(),
                    entry.dueDate(),
                    entry.paymentDate(),
                    entry.paymentMethod(),
                    entry.createdBy(),
                    entry.orderReference(),
                    EntradaFinanceira.EntryStatus.CANCELLED
            ));
            return;
        }

        DespesaFinanceira expense = expenseRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transacao nao encontrada."));
        expenseRepository.save(new DespesaFinanceira(
                expense.id(),
                expense.category(),
                expense.amount(),
                expense.expenseDate(),
                expense.responsible(),
                expense.description(),
                expense.dueDate(),
                expense.paymentDate(),
                expense.paymentMethod(),
                expense.createdBy(),
                expense.orderReference(),
                DespesaFinanceira.ExpenseStatus.CANCELLED,
                expense.notes()
        ));
    }

    @Override
    public List<EntradaFinanceira> listEntries() {
        return entryRepository.findAll();
    }

    @Override
    public List<DespesaFinanceira> listExpenses() {
        return expenseRepository.findAll();
    }

    @Override
    public List<PlanoParcelamento> listPlanoParcelamentos() {
        return installmentPlanRepository.findAll();
    }

    @Override
    public List<TransacaoFinanceiraView> listTransactions() {
        List<TransacaoFinanceiraView> entries = entryRepository.findAll().stream()
                .map(entry -> new TransacaoFinanceiraView(
                        entry.id(),
                        TransactionType.ENTRY,
                        entry.category(),
                        entry.description(),
                        entry.customerId(),
                        entry.orderReference(),
                        entry.amount(),
                        entry.entryDate(),
                        entry.dueDate(),
                        entry.paymentDate(),
                        entry.paymentMethod(),
                        entry.createdBy(),
                        mapTransactionStatus(entry.status())
                ))
                .toList();
        List<TransacaoFinanceiraView> expenses = expenseRepository.findAll().stream()
                .map(expense -> new TransacaoFinanceiraView(
                        expense.id(),
                        TransactionType.EXPENSE,
                        expense.category().name(),
                        expense.description(),
                        "",
                        expense.orderReference(),
                        expense.amount(),
                        expense.expenseDate(),
                        expense.dueDate(),
                        expense.paymentDate(),
                        expense.paymentMethod(),
                        expense.createdBy(),
                        mapTransactionStatus(expense.status())
                ))
                .toList();
        return java.util.stream.Stream.concat(entries.stream(), expenses.stream())
                .sorted(Comparator.comparing(TransacaoFinanceiraView::issueDate).reversed())
                .toList();
    }

    @Override
    public BigDecimal currentBalance() {
        BigDecimal entries = entryRepository.findAll().stream()
                .filter(entry -> entry.status() == EntradaFinanceira.EntryStatus.RECEIVED)
                .map(EntradaFinanceira::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expenses = expenseRepository.findAll().stream()
                .filter(expense -> expense.status() == DespesaFinanceira.ExpenseStatus.PAID)
                .map(DespesaFinanceira::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return entries.subtract(expenses);
    }

    @Override
    public List<PlanoParcelamento> overdueInstallments(LocalDate referenceDate) {
        return installmentPlanRepository.findAll().stream()
                .filter(plan -> plan.isOverdue(referenceDate))
                .toList();
    }

    private EntradaFinanceira.EntryType parseEntryType(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            return EntradaFinanceira.EntryType.PIX;
        }
        return switch (paymentMethod.trim().toUpperCase()) {
            case "DINHEIRO", "CASH" -> EntradaFinanceira.EntryType.CASH;
            case "BOLETO" -> EntradaFinanceira.EntryType.BOLETO;
            case "CARTAO", "CARD" -> EntradaFinanceira.EntryType.CARD;
            default -> EntradaFinanceira.EntryType.PIX;
        };
    }

    private DespesaFinanceira.ExpenseCategory parseExpenseCategory(String category) {
        if (category == null || category.isBlank()) {
            return DespesaFinanceira.ExpenseCategory.EXTRAS;
        }
        return switch (category.trim().toUpperCase()) {
            case "ALIMENTACAO", "FOOD" -> DespesaFinanceira.ExpenseCategory.FOOD;
            case "COMBUSTIVEL", "FUEL" -> DespesaFinanceira.ExpenseCategory.FUEL;
            case "PRODUTOS", "PRODUCTS" -> DespesaFinanceira.ExpenseCategory.PRODUCTS;
            default -> DespesaFinanceira.ExpenseCategory.EXTRAS;
        };
    }

    private EntradaFinanceira.EntryStatus mapEntryStatus(TransactionStatus status) {
        return switch (status) {
            case PAID -> EntradaFinanceira.EntryStatus.RECEIVED;
            case CANCELLED -> EntradaFinanceira.EntryStatus.CANCELLED;
            default -> EntradaFinanceira.EntryStatus.PENDING;
        };
    }

    private DespesaFinanceira.ExpenseStatus mapExpenseStatus(TransactionStatus status) {
        return switch (status) {
            case PAID -> DespesaFinanceira.ExpenseStatus.PAID;
            case CANCELLED -> DespesaFinanceira.ExpenseStatus.CANCELLED;
            default -> DespesaFinanceira.ExpenseStatus.PENDING;
        };
    }

    private TransactionStatus mapTransactionStatus(EntradaFinanceira.EntryStatus status) {
        return switch (status) {
            case RECEIVED -> TransactionStatus.PAID;
            case CANCELLED -> TransactionStatus.CANCELLED;
            default -> TransactionStatus.PENDING;
        };
    }

    private TransactionStatus mapTransactionStatus(DespesaFinanceira.ExpenseStatus status) {
        return switch (status) {
            case PAID -> TransactionStatus.PAID;
            case CANCELLED -> TransactionStatus.CANCELLED;
            default -> TransactionStatus.PENDING;
        };
    }

    private String normalizeResponsible(String value) {
        return value == null || value.isBlank() ? "Sistema" : value.trim();
    }
}

