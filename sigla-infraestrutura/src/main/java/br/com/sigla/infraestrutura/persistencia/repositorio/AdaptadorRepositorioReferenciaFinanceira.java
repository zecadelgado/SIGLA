package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.financeiro.porta.saida.RepositorioReferenciaFinanceira;
import br.com.sigla.infraestrutura.persistencia.PersistenciaIds;
import br.com.sigla.infraestrutura.persistencia.entidade.FinanceiroCategoriaEntidade;
import br.com.sigla.infraestrutura.persistencia.entidade.FinanceiroFormaPagamentoEntidade;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@ConditionalOnBean(SpringDataRepositorioFinanceiroCategoria.class)
public class AdaptadorRepositorioReferenciaFinanceira implements RepositorioReferenciaFinanceira {

    private final SpringDataRepositorioFinanceiroCategoria categoriaRepository;
    private final SpringDataRepositorioFinanceiroFormaPagamento formaPagamentoRepository;

    public AdaptadorRepositorioReferenciaFinanceira(
            SpringDataRepositorioFinanceiroCategoria categoriaRepository,
            SpringDataRepositorioFinanceiroFormaPagamento formaPagamentoRepository
    ) {
        this.categoriaRepository = categoriaRepository;
        this.formaPagamentoRepository = formaPagamentoRepository;
    }

    @Override
    public List<ReferenciaFinanceira> categoriasAtivas(String tipo) {
        String normalized = tipo == null || tipo.isBlank() ? "ENTRY" : tipo.trim();
        return categoriaRepository.findByTipoIgnoreCaseAndAtivoTrueOrderByNomeAsc(normalized).stream()
                .map(this::toReferencia)
                .toList();
    }

    @Override
    public List<ReferenciaFinanceira> formasPagamentoAtivas() {
        return formaPagamentoRepository.findByAtivoTrueOrderByNomeAsc().stream()
                .map(this::toReferencia)
                .toList();
    }

    private ReferenciaFinanceira toReferencia(FinanceiroCategoriaEntidade entity) {
        return new ReferenciaFinanceira(PersistenciaIds.toString(entity.getId()), entity.getNome());
    }

    private ReferenciaFinanceira toReferencia(FinanceiroFormaPagamentoEntidade entity) {
        return new ReferenciaFinanceira(PersistenciaIds.toString(entity.getId()), entity.getNome());
    }
}

@Repository
@ConditionalOnMissingBean(SpringDataRepositorioFinanceiroCategoria.class)
class InMemoryAdaptadorRepositorioReferenciaFinanceira implements RepositorioReferenciaFinanceira {

    @Override
    public List<ReferenciaFinanceira> categoriasAtivas(String tipo) {
        return List.of();
    }

    @Override
    public List<ReferenciaFinanceira> formasPagamentoAtivas() {
        return List.of();
    }
}
