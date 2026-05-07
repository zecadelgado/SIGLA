package br.com.sigla.aplicacao.financeiro.porta.saida;

import br.com.sigla.dominio.financeiro.CategoriaFinanceira;
import br.com.sigla.dominio.financeiro.FormaPagamentoFinanceira;
import br.com.sigla.dominio.financeiro.LancamentoFinanceiro;

import java.util.List;
import java.util.Optional;

public interface RepositorioLancamentoFinanceiro {

    LancamentoFinanceiro save(LancamentoFinanceiro lancamento);

    Optional<LancamentoFinanceiro> findById(String id);

    Optional<LancamentoFinanceiro> findByOrdemServicoId(String ordemServicoId);

    List<LancamentoFinanceiro> findAll();

    List<CategoriaFinanceira> findCategoriasAtivas();

    List<FormaPagamentoFinanceira> findFormasPagamentoAtivas();
}
