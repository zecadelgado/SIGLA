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
    public BigDecimal currentBalance() {
        BigDecimal entries = entryRepository.findAll().stream()
                .filter(entry -> entry.status() == EntradaFinanceira.EntryStatus.RECEIVED)
                .map(EntradaFinanceira::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expenses = expenseRepository.findAll().stream()
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
}

