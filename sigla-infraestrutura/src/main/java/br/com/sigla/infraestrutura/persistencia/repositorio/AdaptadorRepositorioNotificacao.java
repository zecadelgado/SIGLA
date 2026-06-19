package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.notificacoes.porta.saida.RepositorioNotificacao;
import br.com.sigla.dominio.notificacoes.DestinatarioNotificacao;
import br.com.sigla.dominio.notificacoes.Notificacao;
import br.com.sigla.dominio.notificacoes.RemetenteNotificacao;
import br.com.sigla.infraestrutura.persistencia.MetadataMapCodec;
import br.com.sigla.infraestrutura.persistencia.entidade.NotificacaoEntidade;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class AdaptadorRepositorioNotificacao implements RepositorioNotificacao {

    private final SpringDataRepositorioNotificacao repository;

    public AdaptadorRepositorioNotificacao(SpringDataRepositorioNotificacao repository) {
        this.repository = repository;
    }

    @Override
    public void replaceAll(List<Notificacao> notificacoes) {
        repository.saveAll(notificacoes.stream().map(this::toEntity).toList());
    }

    @Override
    public void save(Notificacao notificacao) {
        repository.save(toEntity(notificacao));
    }

    @Override
    public List<Notificacao> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Notificacao> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Notificacao> findDuePending(LocalDateTime momento) {
        return repository.findDuePending(Notificacao.NotificacaoStatus.PENDING, momento, momento.toLocalDate())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Notificacao> findByRelatedEntityId(String relatedEntityId) {
        return repository.findByRelatedEntityId(relatedEntityId).stream().map(this::toDomain).toList();
    }

    private NotificacaoEntidade toEntity(Notificacao notification) {
        NotificacaoEntidade entity = new NotificacaoEntidade();
        entity.setId(notification.id());
        entity.setType(notification.type());
        entity.setTitle(notification.title());
        entity.setMessage(notification.message());
        entity.setRelatedEntityId(notification.relatedEntityId());
        entity.setTriggerDate(notification.triggerDate());
        entity.setStatus(notification.status());
        entity.setTemplateId(emptyToNull(notification.templateId()));
        entity.setScheduledFor(notification.scheduledFor());
        entity.setSource(notification.source());
        entity.setChannel(notification.canal());
        entity.setAttempts(notification.attempts());
        entity.setLastError(emptyToNull(notification.lastError()));
        entity.setMetadata(MetadataMapCodec.encode(notification.metadata()));
        entity.setSentAt(notification.sentAt());
        entity.setCreatedBy(emptyToNull(notification.createdBy()));

        DestinatarioNotificacao destinatario = notification.destinatario();
        if (destinatario != null) {
            entity.setRecipientType(destinatario.tipo());
            entity.setRecipientName(emptyToNull(destinatario.nome()));
            entity.setRecipientPhone(emptyToNull(destinatario.telefone()));
            entity.setCustomerId(emptyToNull(destinatario.clienteId()));
            entity.setEmployeeId(emptyToNull(destinatario.funcionarioId()));
        }
        RemetenteNotificacao remetente = notification.remetente();
        if (remetente != null) {
            entity.setSenderType(remetente.tipo());
            entity.setSenderName(emptyToNull(remetente.nome()));
        }
        return entity;
    }

    private Notificacao toDomain(NotificacaoEntidade entity) {
        DestinatarioNotificacao destinatario = entity.getRecipientType() == null
                ? null
                : new DestinatarioNotificacao(
                entity.getRecipientType(),
                entity.getRecipientName(),
                entity.getRecipientPhone(),
                entity.getCustomerId(),
                entity.getEmployeeId());
        RemetenteNotificacao remetente = entity.getSenderType() == null
                ? null
                : new RemetenteNotificacao(entity.getSenderType(), entity.getSenderName());

        return Notificacao.builder()
                .id(entity.getId())
                .type(entity.getType())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .relatedEntityId(entity.getRelatedEntityId())
                .triggerDate(entity.getTriggerDate())
                .status(entity.getStatus())
                .destinatario(destinatario)
                .remetente(remetente)
                .templateId(entity.getTemplateId())
                .scheduledFor(entity.getScheduledFor())
                .source(entity.getSource())
                .canal(entity.getChannel())
                .attempts(entity.getAttempts())
                .lastError(entity.getLastError())
                .metadata(MetadataMapCodec.decode(entity.getMetadata()))
                .sentAt(entity.getSentAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}

@Repository
@Profile("memoria")
class InMemoryAdaptadorRepositorioNotificacao implements RepositorioNotificacao {

    private final Map<String, Notificacao> storage = new ConcurrentHashMap<>();

    @Override
    public void replaceAll(List<Notificacao> notificacoes) {
        for (Notificacao notification : notificacoes) {
            storage.put(notification.id(), notification);
        }
    }

    @Override
    public void save(Notificacao notificacao) {
        storage.put(notificacao.id(), notificacao);
    }

    @Override
    public List<Notificacao> findAll() {
        return storage.values().stream().toList();
    }

    @Override
    public Optional<Notificacao> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Notificacao> findDuePending(LocalDateTime momento) {
        return storage.values().stream()
                .filter(notificacao -> notificacao.status() == Notificacao.NotificacaoStatus.PENDING)
                .filter(notificacao -> !notificacao.momentoDisparo().isAfter(momento))
                .toList();
    }

    @Override
    public List<Notificacao> findByRelatedEntityId(String relatedEntityId) {
        return storage.values().stream()
                .filter(notificacao -> notificacao.relatedEntityId().equals(relatedEntityId))
                .toList();
    }
}

interface SpringDataRepositorioNotificacao extends JpaRepository<NotificacaoEntidade, String> {

    List<NotificacaoEntidade> findByRelatedEntityId(String relatedEntityId);

    @Query("select n from NotificacaoEntidade n where n.status = :status "
            + "and (n.scheduledFor <= :momento or (n.scheduledFor is null and n.triggerDate <= :data))")
    List<NotificacaoEntidade> findDuePending(
            @Param("status") Notificacao.NotificacaoStatus status,
            @Param("momento") LocalDateTime momento,
            @Param("data") LocalDate data);
}
