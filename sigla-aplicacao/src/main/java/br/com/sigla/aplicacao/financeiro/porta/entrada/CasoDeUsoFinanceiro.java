package br.com.sigla.aplicacao.financeiro.porta.entrada;

import br.com.sigla.dominio.financeiro.EntradaFinanceira;
import br.com.sigla.dominio.financeiro.DespesaFinanceira;
import br.com.sigla.dominio.financeiro.PlanoParcelamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface CasoDeUsoFinanceiro {

    void registerEntry(RegisterEntradaFinanceiraCommand command);

    void registerExpense(RegisterDespesaFinanceiraCommand command);

    void registerPlanoParcelamento(RegisterPlanoParcelamentoCommand command);

    void registerTransaction(RegisterTransacaoFinanceiraCommand command);

    List<EntradaFinanceira> listEntries();

    List<DespesaFinanceira> listExpenses();

    List<PlanoParcelamento> listPlanoParcelamentos();

    List<TransacaoFinanceiraView> listTransactions();

    BigDecimal currentBalance();

    List<PlanoParcelamento> overdueInstallments(LocalDate referenceDate);

    record RegisterEntradaFinanceiraCommand(
            String id,
            EntradaFinanceira.EntryType entryType,
            BigDecimal amount,
            LocalDate entryDate,
            String customerId,
            String serviceProvidedId,
            String description,
            String category,
            LocalDate dueDate,
            LocalDate paymentDate,
            String paymentMethod,
            String createdBy,
            String orderReference,
            EntradaFinanceira.EntryStatus status
    ) {
        public RegisterEntradaFinanceiraCommand(
                String id,
                EntradaFinanceira.EntryType entryType,
                BigDecimal amount,
                LocalDate entryDate,
                String customerId,
                String serviceProvidedId,
                EntradaFinanceira.EntryStatus status
        ) {
            this(
                    id,
                    entryType,
                    amount,
                    entryDate,
                    customerId,
                    serviceProvidedId,
                    "",
                    "",
                    entryDate,
                    null,
                    "",
                    "",
                    null,
                    status
            );
        }
    }

    record RegisterDespesaFinanceiraCommand(
            String id,
            DespesaFinanceira.ExpenseCategory category,
            BigDecimal amount,
            LocalDate expenseDate,
            String responsible,
            String description,
            LocalDate dueDate,
            LocalDate paymentDate,
            String paymentMethod,
            String createdBy,
            String orderReference,
            DespesaFinanceira.ExpenseStatus status,
            String notes
    ) {
        public RegisterDespesaFinanceiraCommand(
                String id,
                DespesaFinanceira.ExpenseCategory category,
                BigDecimal amount,
                LocalDate expenseDate,
                String responsible,
                String notes
        ) {
            this(
                    id,
                    category,
                    amount,
                    expenseDate,
                    responsible,
                    category.name(),
                    expenseDate,
                    null,
                    "",
                    responsible,
                    null,
                    DespesaFinanceira.ExpenseStatus.PAID,
                    notes
            );
        }
    }

    record RegisterPlanoParcelamentoCommand(
            String id,
            String customerId,
            BigDecimal totalAmount,
            int totalInstallments,
            int paidInstallments,
            PlanoParcelamento.InstallmentStatus status,
            LocalDate nextDueDate
    ) {
    }

    record RegisterTransacaoFinanceiraCommand(
            String id,
            TransactionType type,
            String category,
            String description,
            String customerId,
            String serviceProvidedId,
            String orderReference,
            BigDecimal amount,
            LocalDate issueDate,
            LocalDate dueDate,
            LocalDate paymentDate,
            String paymentMethod,
            boolean installment,
            int installmentCount,
            String createdBy,
            String notes,
            TransactionStatus status
    ) {
    }

    record TransacaoFinanceiraView(
            String id,
            TransactionType type,
            String category,
            String description,
            String customerId,
            String orderReference,
            BigDecimal amount,
            LocalDate issueDate,
            LocalDate dueDate,
            LocalDate paymentDate,
            String paymentMethod,
            String createdBy,
            TransactionStatus status
    ) {
    }

    enum TransactionType {
        ENTRY,
        EXPENSE
    }

    enum TransactionStatus {
        PENDING,
        PAID,
        CANCELLED
    }
}

