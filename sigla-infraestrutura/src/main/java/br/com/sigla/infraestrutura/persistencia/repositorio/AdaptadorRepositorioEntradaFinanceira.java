package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.financeiro.porta.saida.RepositorioEntradaFinanceira;
import br.com.sigla.dominio.financeiro.EntradaFinanceira;
import br.com.sigla.infraestrutura.persistencia.entidade.EntradaFinanceiraEntidade;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnBean(SpringDataRepositorioEntradaFinanceira.class)
public class AdaptadorRepositorioEntradaFinanceira implements RepositorioEntradaFinanceira {

    private final SpringDataRepositorioEntradaFinanceira repository;

    public AdaptadorRepositorioEntradaFinanceira(SpringDataRepositorioEntradaFinanceira repository) {
        this.repository = repository;
    }

    @Override
    public void save(EntradaFinanceira entry) {
        EntradaFinanceiraEntidade entity = new EntradaFinanceiraEntidade();
        entity.setId(entry.id());
        entity.setEntryType(entry.entryType());
        entity.setAmount(entry.amount());
        entity.setEntryDate(entry.entryDate());
        entity.setClienteId(entry.customerId());
        entity.setServicoPrestadoId(entry.serviceProvidedId());
        entity.setStatus(entry.status());
        repository.save(entity);
    }

    @Override
    public List<EntradaFinanceira> findAll() {
        return repository.findAll().stream()
                .map(entity -> new EntradaFinanceira(
                        entity.getId(),
                        entity.getEntryType(),
                        entity.getAmount(),
                        entity.getEntryDate(),
                        entity.getClienteId(),
                        entity.getServicoPrestadoId(),
                        entity.getStatus()
                ))
                .toList();
    }
}

@Repository
@ConditionalOnMissingBean(SpringDataRepositorioEntradaFinanceira.class)
class InMemoryAdaptadorRepositorioEntradaFinanceira implements RepositorioEntradaFinanceira {

    private final Map<String, EntradaFinanceira> storage = new ConcurrentHashMap<>();

    @Override
    public void save(EntradaFinanceira entry) {
        storage.put(entry.id(), entry);
    }

    @Override
    public List<EntradaFinanceira> findAll() {
        return storage.values().stream().toList();
    }
}

interface SpringDataRepositorioEntradaFinanceira extends JpaRepository<EntradaFinanceiraEntidade, String> {
}

