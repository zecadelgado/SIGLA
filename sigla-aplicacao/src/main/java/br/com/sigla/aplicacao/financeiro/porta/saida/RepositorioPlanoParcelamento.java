package br.com.sigla.aplicacao.financeiro.porta.saida;

import br.com.sigla.dominio.financeiro.PlanoParcelamento;

import java.util.List;
import java.util.Optional;

public interface RepositorioPlanoParcelamento {

    void save(PlanoParcelamento installmentPlan);

    List<PlanoParcelamento> findAll();

    Optional<PlanoParcelamento> findById(String id);
}

