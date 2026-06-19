package br.com.sigla.aplicacao.notificacoes.porta.saida;

import br.com.sigla.dominio.notificacoes.Destinatario;
import br.com.sigla.dominio.notificacoes.Notificacao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RepositorioNotificacao {

    void replaceAll(List<Notificacao> notificacoes);

    void save(Notificacao notificacao);

    List<Notificacao> findAll();

    Optional<Notificacao> findById(String id);

    /** Notificacoes PENDING cujo horario de disparo ja chegou (scheduled_for ou trigger_date <= momento). */
    List<Notificacao> findDuePending(LocalDateTime momento);

    List<Notificacao> findByRelatedEntityId(String relatedEntityId);

    /** Dedup considerando tipo + entidade relacionada + destinatario, sobre status ativos. */
    default boolean existsAtivoParaDestinatario(
            Notificacao.NotificacaoType type,
            String relatedEntityId,
            Destinatario destinatario
    ) {
        Set<Notificacao.NotificacaoStatus> ativos = Set.of(
                Notificacao.NotificacaoStatus.PENDING,
                Notificacao.NotificacaoStatus.SENT,
                Notificacao.NotificacaoStatus.OPEN
        );
        return findAll().stream().anyMatch(notificacao -> notificacao.type() == type
                && notificacao.relatedEntityId().equals(relatedEntityId)
                && ativos.contains(notificacao.status())
                && notificacao.destinatario() != null
                && notificacao.destinatario().tipo() == destinatario);
    }

    default boolean existsByTypeAndRelatedEntityIdAndStatusIn(
            Notificacao.NotificacaoType type,
            String relatedEntityId,
            Set<Notificacao.NotificacaoStatus> statuses
    ) {
        return findAll().stream()
                .anyMatch(notificacao -> notificacao.type() == type
                        && notificacao.relatedEntityId().equals(relatedEntityId)
                        && statuses.contains(notificacao.status()));
    }
}
