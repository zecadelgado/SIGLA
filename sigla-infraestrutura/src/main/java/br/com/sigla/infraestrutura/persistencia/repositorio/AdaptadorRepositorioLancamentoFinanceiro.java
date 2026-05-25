package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.financeiro.porta.saida.RepositorioLancamentoFinanceiro;
import br.com.sigla.dominio.financeiro.CategoriaFinanceira;
import br.com.sigla.dominio.financeiro.FormaPagamentoFinanceira;
import br.com.sigla.dominio.financeiro.LancamentoFinanceiro;
import br.com.sigla.infraestrutura.persistencia.PersistenciaIds;
import br.com.sigla.infraestrutura.persistencia.entidade.FinanceiroCategoriaEntidade;
import br.com.sigla.infraestrutura.persistencia.entidade.FinanceiroFormaPagamentoEntidade;
import br.com.sigla.infraestrutura.persistencia.entidade.FinanceiroLancamentoEntidade;
import br.com.sigla.infraestrutura.persistencia.entidade.FinanceiroParcelaEntidade;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnBean(SpringDataRepositorioFinanceiroLancamento.class)
public class AdaptadorRepositorioLancamentoFinanceiro implements RepositorioLancamentoFinanceiro {

    private final SpringDataRepositorioFinanceiroLancamento lancamentoRepository;
    private final SpringDataRepositorioFinanceiroCategoria categoriaRepository;
    private final SpringDataRepositorioFinanceiroFormaPagamento formaPagamentoRepository;

    public AdaptadorRepositorioLancamentoFinanceiro(
            SpringDataRepositorioFinanceiroLancamento lancamentoRepository,
            SpringDataRepositorioFinanceiroCategoria categoriaRepository,
            SpringDataRepositorioFinanceiroFormaPagamento formaPagamentoRepository
    ) {
        this.lancamentoRepository = lancamentoRepository;
        this.categoriaRepository = categoriaRepository;
        this.formaPagamentoRepository = formaPagamentoRepository;
    }

    @Override
    public LancamentoFinanceiro save(LancamentoFinanceiro lancamento) {
        FinanceiroLancamentoEntidade entity = lancamentoRepository.findById(PersistenciaIds.toUuid(lancamento.id()))
                .orElseGet(FinanceiroLancamentoEntidade::new);
        entity.setId(PersistenciaIds.toUuid(lancamento.id()));
        entity.setTipo(lancamento.tipo().name());
        entity.setCategoriaId(PersistenciaIds.toUuid(lancamento.categoriaId()));
        entity.setFormaPagamentoId(PersistenciaIds.toUuid(lancamento.formaPagamentoId()));
        entity.setDescricao(lancamento.descricao());
        entity.setClienteId(PersistenciaIds.toUuid(lancamento.clienteId()));
        entity.setOrdemServicoId(PersistenciaIds.toUuid(lancamento.ordemServicoId()));
        entity.setValorTotal(lancamento.valorTotal());
        entity.setDataEmissao(lancamento.dataEmissao());
        entity.setDataVencimento(lancamento.dataVencimento());
        entity.setDataPagamento(lancamento.dataPagamento());
        entity.setStatus(lancamento.status().name());
        entity.setParcelado(lancamento.parcelado());
        entity.setQuantidadeParcelas(lancamento.quantidadeParcelas());
        entity.setObservacoes(lancamento.observacoes());
        entity.setCriadoPor(PersistenciaIds.toUuidIfValid(lancamento.criadoPor()));
        entity.getParcelas().clear();
        for (LancamentoFinanceiro.ParcelaFinanceira parcela : lancamento.parcelas()) {
            FinanceiroParcelaEntidade parcelaEntity = new FinanceiroParcelaEntidade();
            parcelaEntity.setId(PersistenciaIds.toUuid(parcela.id()));
            parcelaEntity.setNumeroParcela(parcela.numeroParcela());
            parcelaEntity.setValorParcela(parcela.valorParcela());
            parcelaEntity.setDataVencimento(parcela.dataVencimento());
            parcelaEntity.setDataPagamento(parcela.dataPagamento());
            parcelaEntity.setStatus(parcela.status().name());
            entity.getParcelas().add(parcelaEntity);
        }
        return toDomain(lancamentoRepository.save(entity));
    }

    @Override
    public Optional<LancamentoFinanceiro> findById(String id) {
        return lancamentoRepository.findById(PersistenciaIds.toUuid(id)).map(this::toDomain);
    }

    @Override
    public Optional<LancamentoFinanceiro> findByOrdemServicoId(String ordemServicoId) {
        UUID id = PersistenciaIds.toUuid(ordemServicoId);
        if (id == null) {
            return Optional.empty();
        }
        return lancamentoRepository.findByOrdemServicoId(id).map(this::toDomain);
    }

    @Override
    public List<LancamentoFinanceiro> findAll() {
        return lancamentoRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<CategoriaFinanceira> findCategoriasAtivas() {
        return categoriaRepository.findAll().stream()
                .filter(FinanceiroCategoriaEntidade::isAtivo)
                .map(entity -> new CategoriaFinanceira(
                        PersistenciaIds.toString(entity.getId()),
                        entity.getTipo(),
                        entity.getNome(),
                        entity.isAtivo()))
                .toList();
    }

    @Override
    public List<FormaPagamentoFinanceira> findFormasPagamentoAtivas() {
        return formaPagamentoRepository.findAll().stream()
                .filter(FinanceiroFormaPagamentoEntidade::isAtivo)
                .map(entity -> new FormaPagamentoFinanceira(
                        PersistenciaIds.toString(entity.getId()),
                        entity.getNome(),
                        entity.isAtivo()))
                .toList();
    }

    private LancamentoFinanceiro toDomain(FinanceiroLancamentoEntidade entity) {
        return new LancamentoFinanceiro(
                PersistenciaIds.toString(entity.getId()),
                LancamentoFinanceiro.Tipo.from(entity.getTipo()),
                PersistenciaIds.toString(entity.getCategoriaId()),
                categoriaNome(entity.getCategoriaId()),
                PersistenciaIds.toString(entity.getFormaPagamentoId()),
                formaNome(entity.getFormaPagamentoId()),
                entity.getDescricao() == null || entity.getDescricao().isBlank() ? "Lancamento financeiro" : entity.getDescricao(),
                PersistenciaIds.toString(entity.getClienteId()),
                PersistenciaIds.toString(entity.getOrdemServicoId()),
                entity.getValorTotal() == null || entity.getValorTotal().signum() <= 0 ? java.math.BigDecimal.ONE : entity.getValorTotal(),
                entity.getDataEmissao() == null ? java.time.LocalDate.now() : entity.getDataEmissao(),
                entity.getDataVencimento(),
                entity.getDataPagamento(),
                LancamentoFinanceiro.Status.from(entity.getStatus()),
                entity.isParcelado(),
                entity.getQuantidadeParcelas() == null ? 1 : entity.getQuantidadeParcelas(),
                entity.getObservacoes(),
                PersistenciaIds.toString(entity.getCriadoPor()),
                entity.getParcelas().stream()
                        .map(parcela -> new LancamentoFinanceiro.ParcelaFinanceira(
                                PersistenciaIds.toString(parcela.getId()),
                                parcela.getNumeroParcela(),
                                parcela.getValorParcela() == null || parcela.getValorParcela().signum() <= 0 ? java.math.BigDecimal.ONE : parcela.getValorParcela(),
                                parcela.getDataVencimento() == null ? java.time.LocalDate.now() : parcela.getDataVencimento(),
                                parcela.getDataPagamento(),
                                LancamentoFinanceiro.Status.from(parcela.getStatus())))
                        .toList()
        );
    }

    private String categoriaNome(UUID id) {
        return id == null ? "" : categoriaRepository.findById(id).map(FinanceiroCategoriaEntidade::getNome).orElse("");
    }

    private String formaNome(UUID id) {
        return id == null ? "" : formaPagamentoRepository.findById(id).map(FinanceiroFormaPagamentoEntidade::getNome).orElse("");
    }
}

@Repository
@ConditionalOnMissingBean(SpringDataRepositorioFinanceiroLancamento.class)
class InMemoryAdaptadorRepositorioLancamentoFinanceiro implements RepositorioLancamentoFinanceiro {

    private final Map<String, LancamentoFinanceiro> storage = new ConcurrentHashMap<>();
    private final List<CategoriaFinanceira> categorias = new ArrayList<>(List.of(
            new CategoriaFinanceira("cat-servicos", "ENTRY", "SERVICOS", true),
            new CategoriaFinanceira("cat-extras", "EXPENSE", "EXTRAS", true),
            new CategoriaFinanceira("cat-produtos", "EXPENSE", "PRODUTOS", true)
    ));
    private final List<FormaPagamentoFinanceira> formas = new ArrayList<>(List.of(
            new FormaPagamentoFinanceira("forma-pix", "PIX", true),
            new FormaPagamentoFinanceira("forma-dinheiro", "DINHEIRO", true),
            new FormaPagamentoFinanceira("forma-boleto", "BOLETO", true),
            new FormaPagamentoFinanceira("forma-cartao", "CARTAO", true)
    ));

    @Override
    public LancamentoFinanceiro save(LancamentoFinanceiro lancamento) {
        storage.put(lancamento.id(), lancamento);
        return lancamento;
    }

    @Override
    public Optional<LancamentoFinanceiro> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<LancamentoFinanceiro> findByOrdemServicoId(String ordemServicoId) {
        return storage.values().stream()
                .filter(lancamento -> lancamento.ordemServicoId().equals(ordemServicoId))
                .findFirst();
    }

    @Override
    public List<LancamentoFinanceiro> findAll() {
        return storage.values().stream().toList();
    }

    @Override
    public List<CategoriaFinanceira> findCategoriasAtivas() {
        return categorias.stream().filter(CategoriaFinanceira::ativo).toList();
    }

    @Override
    public List<FormaPagamentoFinanceira> findFormasPagamentoAtivas() {
        return formas.stream().filter(FormaPagamentoFinanceira::ativo).toList();
    }
}
