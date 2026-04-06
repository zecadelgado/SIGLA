package br.com.sigla.aplicacao.financeiro.porta.saida;

import br.com.sigla.dominio.financeiro.DespesaFinanceira;

import java.util.List;

public interface RepositorioDespesaFinanceira {

    void save(DespesaFinanceira expense);

    List<DespesaFinanceira> findAll();
}

