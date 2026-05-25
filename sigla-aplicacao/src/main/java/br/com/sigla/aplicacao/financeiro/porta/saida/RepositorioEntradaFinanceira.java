package br.com.sigla.aplicacao.financeiro.porta.saida;

import br.com.sigla.dominio.financeiro.EntradaFinanceira;

import java.util.List;
import java.util.Optional;

public interface RepositorioEntradaFinanceira {

    void save(EntradaFinanceira entry);

    Optional<EntradaFinanceira> findById(String id);

    List<EntradaFinanceira> findAll();
}

