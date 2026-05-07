package br.com.sigla.aplicacao.notificacoes.porta.saida;

import br.com.sigla.dominio.notificacoes.Notificacao;

import java.util.List;

public interface RepositorioNotificacao {

    void replaceAll(List<Notificacao> notificacoes);

    void save(Notificacao notificacao);

    List<Notificacao> findAll();

    default boolean existsByTypeAndRelatedEntityIdAndStatusIn(
            Notificacao.NotificacaoType type,
            String relatedEntityId,
            java.util.Set<Notificacao.NotificacaoStatus> statuses
    ) {
        return findAll().stream()
                .anyMatch(notificacao -> notificacao.type() == type
                        && notificacao.relatedEntityId().equals(relatedEntityId)
                        && statuses.contains(notificacao.status()));
    }
}

