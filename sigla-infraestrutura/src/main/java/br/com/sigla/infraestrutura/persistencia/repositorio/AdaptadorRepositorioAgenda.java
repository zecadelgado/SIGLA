package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.agenda.porta.saida.RepositorioAgenda;
import br.com.sigla.dominio.agenda.VisitaAgendada;
import br.com.sigla.infraestrutura.persistencia.PersistenciaIds;
import br.com.sigla.infraestrutura.persistencia.entidade.VisitaAgendadaEntidade;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
        return repository.findById(PersistenciaIds.toUuid(id)).map(this::toDomain);
    }

    private VisitaAgendada toDomain(VisitaAgendadaEntidade entity) {
        return new VisitaAgendada(
                PersistenciaIds.toString(entity.getId()),
                PersistenciaIds.toString(entity.getClienteId()),
                PersistenciaIds.toString(entity.getContratoId()),
                parseVisitType(entity.getType()),
                entity.getStartAt().toLocalDate(),
                entity.getTitle(),
                entity.getServiceType(),
                entity.getInternalResponsible(),
                entity.getStartAt(),
                entity.getEndAt(),
                entity.isAllDay(),
                parseVisitStatus(entity.getStatus()),
                parseVisitPriority(entity.getPriority()),
                PersistenciaIds.toString(entity.getResponsibleId()),
                entity.getNotes()
        );
    }

    private VisitaAgendadaEntidade toEntity(VisitaAgendada schedule) {
        VisitaAgendadaEntidade entity = new VisitaAgendadaEntidade();
        entity.setId(PersistenciaIds.toUuid(schedule.id()));
        entity.setClienteId(PersistenciaIds.toUuid(schedule.customerId()));
        entity.setContratoId(PersistenciaIds.toUuid(schedule.contractId()));
        entity.setType(schedule.serviceType() == null || schedule.serviceType().isBlank() ? schedule.type().name() : schedule.serviceType());
        entity.setTitle(schedule.title());
        entity.setServiceType(schedule.serviceType() == null || schedule.serviceType().isBlank() ? "servico" : schedule.serviceType());
        entity.setInternalResponsible(schedule.internalResponsible());
        entity.setStartAt(schedule.startAt() == null ? schedule.scheduledDate().atStartOfDay() : schedule.startAt());
        entity.setEndAt(schedule.endAt());
        entity.setAllDay(schedule.allDay());
        entity.setStatus(schedule.status().name());
        entity.setPriority(schedule.priority().name());
        entity.setResponsibleId(PersistenciaIds.toUuid(schedule.responsibleId()));
        entity.setNotes(schedule.notes());
        return entity;
    }

    private VisitaAgendada.VisitType parseVisitType(String value) {
        if (value == null || value.isBlank() || "servico".equalsIgnoreCase(value)) {
            return VisitaAgendada.VisitType.ONE_OFF;
        }
        try {
            return VisitaAgendada.VisitType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return VisitaAgendada.VisitType.ONE_OFF;
        }
    }

    private VisitaAgendada.VisitStatus parseVisitStatus(String value) {
        if (value == null || value.isBlank()) {
            return VisitaAgendada.VisitStatus.SCHEDULED;
        }
        return switch (value.trim().toUpperCase()) {
            case "AGENDADO", "AGENDADA" -> VisitaAgendada.VisitStatus.SCHEDULED;
            case "EM_ANDAMENTO" -> VisitaAgendada.VisitStatus.IN_PROGRESS;
            case "CONCLUIDO", "CONCLUIDA" -> VisitaAgendada.VisitStatus.COMPLETED;
            case "CANCELADO", "CANCELADA" -> VisitaAgendada.VisitStatus.CANCELLED;
            case "ATRASADO", "ATRASADA" -> VisitaAgendada.VisitStatus.MISSED;
            default -> VisitaAgendada.VisitStatus.valueOf(value.trim().toUpperCase());
        };
    }

    private VisitaAgendada.VisitPriority parseVisitPriority(String value) {
        if (value == null || value.isBlank()) {
            return VisitaAgendada.VisitPriority.NORMAL;
        }
        return switch (value.trim().toUpperCase()) {
            case "BAIXA" -> VisitaAgendada.VisitPriority.LOW;
            case "MEDIA", "MÉDIA" -> VisitaAgendada.VisitPriority.NORMAL;
            case "ALTA" -> VisitaAgendada.VisitPriority.HIGH;
            case "URGENTE" -> VisitaAgendada.VisitPriority.URGENT;
            default -> VisitaAgendada.VisitPriority.valueOf(value.trim().toUpperCase());
        };
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

interface SpringDataRepositorioAgenda extends JpaRepository<VisitaAgendadaEntidade, UUID> {
}

