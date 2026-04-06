package br.com.sigla.aplicacao.notificacoes.porta.saida;

import br.com.sigla.dominio.notificacoes.Notificacao;

import java.util.List;

public interface RepositorioNotificacao {

    void replaceAll(List<Notificacao> notificacoes);

    List<Notificacao> findAll();
}

