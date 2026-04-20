package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.agenda.porta.saida.RepositorioAgenda;
import br.com.sigla.dominio.agenda.VisitaAgendada;
import br.com.sigla.infraestrutura.persistencia.entidade.VisitaAgendadaEntidade;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnBean(SpringDataRepositorioAgenda.class)
public class AdaptadorRepositorioAgenda implements RepositorioAgenda {

    private final SpringDataRepositorioAgenda repository;

    public AdaptadorRepositorioAgenda(SpringDataRepositorioAgenda repository) {
        this.repository = repository;
    }

    @Override
    public void save(VisitaAgendada schedule) {
        repository.save(toEntity(schedule));
    }

    @Override
    public List<VisitaAgendada> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<VisitaAgendada> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    private VisitaAgendada toDomain(VisitaAgendadaEntidade entity) {
        return new VisitaAgendada(
                entity.getId(),
                entity.getClienteId(),
                entity.getContratoId(),
                entity.getType(),
                entity.getScheduledDate(),
                entity.getTitle(),
                entity.getServiceType(),
                entity.getInternalResponsible(),
                entity.getStartAt(),
                entity.getEndAt(),
                entity.isAllDay(),
                entity.getStatus(),
                entity.getNotes()
        );
    }

    private VisitaAgendadaEntidade toEntity(VisitaAgendada schedule) {
        VisitaAgendadaEntidade entity = new VisitaAgendadaEntidade();
        entity.setId(schedule.id());
        entity.setClienteId(schedule.customerId());
        entity.setContratoId(schedule.contractId());
        entity.setType(schedule.type());
        entity.setScheduledDate(schedule.scheduledDate());
        entity.setTitle(schedule.title());
        entity.setServiceType(schedule.serviceType());
        entity.setInternalResponsible(schedule.internalResponsible());
        entity.setStartAt(schedule.startAt());
        entity.setEndAt(schedule.endAt());
        entity.setAllDay(schedule.allDay());
        entity.setStatus(schedule.status());
        entity.setNotes(schedule.notes());
        return entity;
    }
}

@Repository
@ConditionalOnMissingBean(SpringDataRepositorioAgenda.class)
class InMemoryAdaptadorRepositorioAgenda implements RepositorioAgenda {

    private final Map<String, VisitaAgendada> storage = new ConcurrentHashMap<>();

    @Override
    public void save(VisitaAgendada schedule) {
        storage.put(schedule.id(), schedule);
    }

    @Override
    public List<VisitaAgendada> findAll() {
        return storage.values().stream().toList();
    }

    @Override
    public Optional<VisitaAgendada> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }
}

interface SpringDataRepositorioAgenda extends JpaRepository<VisitaAgendadaEntidade, String> {
}

