package br.com.sigla.aplicacao.financeiro.porta.entrada;

import br.com.sigla.dominio.financeiro.EntradaFinanceira;
import br.com.sigla.dominio.financeiro.DespesaFinanceira;
import br.com.sigla.dominio.financeiro.PlanoParcelamento;
import br.com.sigla.dominio.financeiro.CategoriaFinanceira;
import br.com.sigla.dominio.financeiro.FormaPagamentoFinanceira;
import br.com.sigla.dominio.financeiro.LancamentoFinanceiro;
import br.com.sigla.dominio.servicos.OrdemServico;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface CasoDeUsoFinanceiro {

    void registerEntry(RegisterEntradaFinanceiraCommand command);

    void registerExpense(RegisterDespesaFinanceiraCommand command);

    void registerPlanoParcelamento(RegisterPlanoParcelamentoCommand command);

    void registerTransaction(RegisterTransacaoFinanceiraCommand command);

    void markPaid(String transactionId, LocalDate paymentDate);

    void cancel(String transactionId);

    void cancel(String transactionId, String motivo);

    void estornarPagamento(String transactionId, String motivo);

    void baixarParcela(String lancamentoId, String parcelaId, LocalDate paymentDate);

    LancamentoFinanceiro saveLancamento(SalvarLancamentoFinanceiroCommand command);

    LancamentoFinanceiro updateLancamento(SalvarLancamentoFinanceiroCommand command);

    LancamentoFinanceiro gerarContaReceberOrdemServico(OrdemServico ordemServico);

    List<EntradaFinanceira> listEntries();

    List<DespesaFinanceira> listExpenses();

    List<PlanoParcelamento> listPlanoParcelamentos();

    List<TransacaoFinanceiraView> listTransactions();

    List<TransacaoFinanceiraView> listTransactions(FiltroFinanceiro filtro);

    List<LancamentoFinanceiro> listLancamentos(FiltroFinanceiro filtro);

    List<LancamentoFinanceiro.ParcelaFinanceira> listParcelas(String lancamentoId);

    List<CategoriaFinanceira> listCategoriasAtivas();

    List<FormaPagamentoFinanceira> listFormasPagamentoAtivas();

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

    record SalvarLancamentoFinanceiroCommand(
            String id,
            TransactionType type,
            String categoriaId,
            String formaPagamentoId,
            String descricao,
            String customerId,
            String orderReference,
            BigDecimal amount,
            LocalDate issueDate,
            LocalDate dueDate,
            LocalDate paymentDate,
            boolean installment,
            int installmentCount,
            String createdBy,
            String notes,
            TransactionStatus status
    ) {
    }

    record FiltroFinanceiro(
            LocalDate inicio,
            LocalDate fim,
            TransactionType type,
            TransactionStatus status,
            String customerId,
            String formaPagamentoId,
            String categoriaId,
            String texto,
            boolean apenasVencidos
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
            String notes,
            boolean installment,
            int installmentCount,
            BigDecimal paidAmount,
            boolean overdue,
            String categoryId,
            String paymentMethodId,
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
        CANCELLED,
        PARTIAL,
        OVERDUE
    }
}

