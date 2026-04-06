package br.com.sigla.aplicacao.agenda.porta.saida;

import br.com.sigla.dominio.agenda.VisitaAgendada;

import java.util.List;
import java.util.Optional;

public interface RepositorioAgenda {

    void save(VisitaAgendada schedule);

    List<VisitaAgendada> findAll();

    Optional<VisitaAgendada> findById(String id);
}

