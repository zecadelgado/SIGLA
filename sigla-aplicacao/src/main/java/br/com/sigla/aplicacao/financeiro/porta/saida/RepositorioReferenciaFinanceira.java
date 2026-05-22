package br.com.sigla.aplicacao.financeiro.porta.saida;

import java.util.List;

public interface RepositorioReferenciaFinanceira {

    List<ReferenciaFinanceira> categoriasAtivas(String tipo);

    List<ReferenciaFinanceira> formasPagamentoAtivas();

    record ReferenciaFinanceira(String id, String nome) {
    }
}
