package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.contratos.porta.saida.RepositorioContrato;
import br.com.sigla.dominio.contratos.Contrato;
import br.com.sigla.infraestrutura.persistencia.entidade.ContratoEntidade;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnBean(SpringDataRepositorioContrato.class)
public class AdaptadorRepositorioContrato implements RepositorioContrato {

    private final SpringDataRepositorioContrato repository;

    public AdaptadorRepositorioContrato(SpringDataRepositorioContrato repository) {
        this.repository = repository;
    }

    @Override
    public void save(Contrato contract) {
        repository.save(toEntity(contract));
    }

    @Override
    public List<Contrato> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Contrato> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    private Contrato toDomain(ContratoEntidade entity) {
        return new Contrato(
                entity.getId(),
                entity.getClienteId(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getType(),
                entity.getServiceFrequency(),
                entity.getStatus(),
                entity.getRenewalRule(),
                entity.getAlertDaysBeforeEnd()
        );
    }

    private ContratoEntidade toEntity(Contrato contract) {
        ContratoEntidade entity = new ContratoEntidade();
        entity.setId(contract.id());
        entity.setClienteId(contract.customerId());
        entity.setStartDate(contract.startDate());
        entity.setEndDate(contract.endDate());
        entity.setType(contract.type());
        entity.setServiceFrequency(contract.serviceFrequency());
        entity.setStatus(contract.status());
        entity.setRenewalRule(contract.renewalRule());
        entity.setAlertDaysBeforeEnd(contract.alertDaysBeforeEnd());
        return entity;
    }
}

@Repository
@ConditionalOnMissingBean(SpringDataRepositorioContrato.class)
class InMemoryAdaptadorRepositorioContrato implements RepositorioContrato {

    private final Map<String, Contrato> storage = new ConcurrentHashMap<>();

    @Override
    public void save(Contrato contract) {
        storage.put(contract.id(), contract);
    }

    @Override
    public List<Contrato> findAll() {
        return storage.values().stream().toList();
    }

    @Override
    public Optional<Contrato> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }
}

interface SpringDataRepositorioContrato extends JpaRepository<ContratoEntidade, String> {
}

