package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.notificacoes.porta.saida.RepositorioNotificacao;
import br.com.sigla.dominio.notificacoes.Notificacao;
import br.com.sigla.infraestrutura.persistencia.entidade.NotificacaoEntidade;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnBean(SpringDataRepositorioNotificacao.class)
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

    private NotificacaoEntidade toEntity(Notificacao notification) {
        NotificacaoEntidade entity = new NotificacaoEntidade();
        entity.setId(notification.id());
        entity.setType(notification.type());
        entity.setTitle(notification.title());
        entity.setMessage(notification.message());
        entity.setRelatedEntityId(notification.relatedEntityId());
        entity.setTriggerDate(notification.triggerDate());
        entity.setStatus(notification.status());
        return entity;
    }

    private Notificacao toDomain(NotificacaoEntidade entity) {
        return new Notificacao(
                entity.getId(),
                entity.getType(),
                entity.getTitle(),
                entity.getMessage(),
                entity.getRelatedEntityId(),
                entity.getTriggerDate(),
                entity.getStatus()
        );
    }
}

@Repository
@ConditionalOnMissingBean(SpringDataRepositorioNotificacao.class)
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
}

interface SpringDataRepositorioNotificacao extends JpaRepository<NotificacaoEntidade, String> {
}

