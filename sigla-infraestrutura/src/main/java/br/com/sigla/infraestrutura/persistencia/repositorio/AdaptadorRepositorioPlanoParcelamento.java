package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.financeiro.porta.saida.RepositorioPlanoParcelamento;
import br.com.sigla.dominio.financeiro.PlanoParcelamento;
import br.com.sigla.infraestrutura.persistencia.entidade.PlanoParcelamentoEntidade;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnBean(SpringDataRepositorioPlanoParcelamento.class)
public class AdaptadorRepositorioPlanoParcelamento implements RepositorioPlanoParcelamento {

    private final SpringDataRepositorioPlanoParcelamento repository;

    public AdaptadorRepositorioPlanoParcelamento(SpringDataRepositorioPlanoParcelamento repository) {
        this.repository = repository;
    }

    @Override
    public void save(PlanoParcelamento plan) {
        PlanoParcelamentoEntidade entity = new PlanoParcelamentoEntidade();
        entity.setId(plan.id());
        entity.setClienteId(plan.customerId());
        entity.setTotalAmount(plan.totalAmount());
        entity.setTotalInstallments(plan.totalInstallments());
        entity.setPaidInstallments(plan.paidInstallments());
        entity.setStatus(plan.status());
        entity.setNextDueDate(plan.nextDueDate());
        repository.save(entity);
    }

    @Override
    public List<PlanoParcelamento> findAll() {
        return repository.findAll().stream()
                .map(entity -> new PlanoParcelamento(
                        entity.getId(),
                        entity.getClienteId(),
                        entity.getTotalAmount(),
                        entity.getTotalInstallments(),
                        entity.getPaidInstallments(),
                        entity.getStatus(),
                        entity.getNextDueDate()
                ))
                .toList();
    }

    @Override
    public Optional<PlanoParcelamento> findById(String id) {
        return repository.findById(id).map(entity -> new PlanoParcelamento(
                entity.getId(),
                entity.getClienteId(),
                entity.getTotalAmount(),
                entity.getTotalInstallments(),
                entity.getPaidInstallments(),
                entity.getStatus(),
                entity.getNextDueDate()
        ));
    }
}

@Repository
@ConditionalOnMissingBean(SpringDataRepositorioPlanoParcelamento.class)
class InMemoryAdaptadorRepositorioPlanoParcelamento implements RepositorioPlanoParcelamento {

    private final Map<String, PlanoParcelamento> storage = new ConcurrentHashMap<>();

    @Override
    public void save(PlanoParcelamento plan) {
        storage.put(plan.id(), plan);
    }

    @Override
    public List<PlanoParcelamento> findAll() {
        return storage.values().stream().toList();
    }

    @Override
    public Optional<PlanoParcelamento> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }
}

interface SpringDataRepositorioPlanoParcelamento extends JpaRepository<PlanoParcelamentoEntidade, String> {
}

