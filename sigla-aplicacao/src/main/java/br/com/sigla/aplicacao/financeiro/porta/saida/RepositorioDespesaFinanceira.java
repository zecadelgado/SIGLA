package br.com.sigla.aplicacao.financeiro.porta.saida;

import br.com.sigla.dominio.financeiro.DespesaFinanceira;

import java.util.List;
import java.util.Optional;

public interface RepositorioDespesaFinanceira {

    void save(DespesaFinanceira expense);

    Optional<DespesaFinanceira> findById(String id);

    List<DespesaFinanceira> findAll();
}

