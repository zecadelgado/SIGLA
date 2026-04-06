package br.com.sigla.aplicacao.financeiro.porta.saida;

import br.com.sigla.dominio.financeiro.EntradaFinanceira;

import java.util.List;

public interface RepositorioEntradaFinanceira {

    void save(EntradaFinanceira entry);

    List<EntradaFinanceira> findAll();
}

