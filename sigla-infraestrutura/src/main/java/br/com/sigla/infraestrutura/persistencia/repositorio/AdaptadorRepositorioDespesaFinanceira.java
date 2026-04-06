package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.financeiro.porta.saida.RepositorioDespesaFinanceira;
import br.com.sigla.dominio.financeiro.DespesaFinanceira;
import br.com.sigla.infraestrutura.persistencia.entidade.DespesaFinanceiraEntidade;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnBean(SpringDataRepositorioDespesaFinanceira.class)
public class AdaptadorRepositorioDespesaFinanceira implements RepositorioDespesaFinanceira {

    private final SpringDataRepositorioDespesaFinanceira repository;

    public AdaptadorRepositorioDespesaFinanceira(SpringDataRepositorioDespesaFinanceira repository) {
        this.repository = repository;
    }

    @Override
    public void save(DespesaFinanceira expense) {
        DespesaFinanceiraEntidade entity = new DespesaFinanceiraEntidade();
        entity.setId(expense.id());
        entity.setCategory(expense.category());
        entity.setAmount(expense.amount());
        entity.setExpenseDate(expense.expenseDate());
        entity.setResponsible(expense.responsible());
        entity.setNotes(expense.notes());
        repository.save(entity);
    }

    @Override
    public List<DespesaFinanceira> findAll() {
        return repository.findAll().stream()
                .map(entity -> new DespesaFinanceira(
                        entity.getId(),
                        entity.getCategory(),
                        entity.getAmount(),
                        entity.getExpenseDate(),
                        entity.getResponsible(),
                        entity.getNotes()
                ))
                .toList();
    }
}

@Repository
@ConditionalOnMissingBean(SpringDataRepositorioDespesaFinanceira.class)
class InMemoryAdaptadorRepositorioDespesaFinanceira implements RepositorioDespesaFinanceira {

    private final Map<String, DespesaFinanceira> storage = new ConcurrentHashMap<>();

    @Override
    public void save(DespesaFinanceira expense) {
        storage.put(expense.id(), expense);
    }

    @Override
    public List<DespesaFinanceira> findAll() {
        return storage.values().stream().toList();
    }
}

interface SpringDataRepositorioDespesaFinanceira extends JpaRepository<DespesaFinanceiraEntidade, String> {
}

