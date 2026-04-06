package br.com.sigla.aplicacao.notificacoes.porta.entrada;

import br.com.sigla.dominio.notificacoes.Notificacao;

import java.time.LocalDate;
import java.util.List;

public interface CasoDeUsoNotificacao {

    void refresh(LocalDate referenceDate);

    List<Notificacao> listAll();
}

