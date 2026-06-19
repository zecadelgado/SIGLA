package br.com.sigla.aplicacao.notificacoes.porta.saida;

import br.com.sigla.dominio.notificacoes.Notificacao;
import br.com.sigla.dominio.notificacoes.NotificacaoConfiguracao;

import java.util.List;
import java.util.Optional;

public interface RepositorioNotificacaoConfiguracao {

    void save(NotificacaoConfiguracao configuracao);

    void deleteById(String id);

    Optional<NotificacaoConfiguracao> findById(String id);

    List<NotificacaoConfiguracao> findAll();

    /** Configuracoes ativas para um tipo de evento (usadas na geracao automatica). */
    List<NotificacaoConfiguracao> findAtivasPorEvento(Notificacao.NotificacaoType eventType);
}
