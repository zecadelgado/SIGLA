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

    List<EntradaFinanceira> listEntries();

    List<DespesaFinanceira> listExpenses();

    List<PlanoParcelamento> listPlanoParcelamentos();

    BigDecimal currentBalance();

    List<PlanoParcelamento> overdueInstallments(LocalDate referenceDate);

    record RegisterEntradaFinanceiraCommand(
            String id,
            EntradaFinanceira.EntryType entryType,
            BigDecimal amount,
            LocalDate entryDate,
            String customerId,
            String serviceProvidedId,
            EntradaFinanceira.EntryStatus status
    ) {
    }

    record RegisterDespesaFinanceiraCommand(
            String id,
            DespesaFinanceira.ExpenseCategory category,
            BigDecimal amount,
            LocalDate expenseDate,
            String responsible,
            String notes
    ) {
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
}

