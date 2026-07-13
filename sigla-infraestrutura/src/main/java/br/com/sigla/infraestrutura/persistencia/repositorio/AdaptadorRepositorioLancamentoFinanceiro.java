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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
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
    @Transactional
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
        entity.setContratoId(PersistenciaIds.toUuid(lancamento.contratoId()));
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
    @Transactional(readOnly = true)
    public Optional<LancamentoFinanceiro> findById(String id) {
        return lancamentoRepository.findById(PersistenciaIds.toUuid(id)).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LancamentoFinanceiro> findByOrdemServicoId(String ordemServicoId) {
        UUID id = PersistenciaIds.toUuid(ordemServicoId);
        if (id == null) {
            return Optional.empty();
        }
        return lancamentoRepository.findByOrdemServicoId(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LancamentoFinanceiro> findByContratoId(String contratoId) {
        UUID id = PersistenciaIds.toUuid(contratoId);
        return id == null ? List.of() : lancamentoRepository.findByContratoId(id).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LancamentoFinanceiro> findAll() {
        Map<UUID, String> categorias = nomesCategorias();
        Map<UUID, String> formas = nomesFormas();
        return lancamentoRepository.findAll().stream()
                .map(entity -> toDomain(entity, categorias, formas))
                .toList();
    }

    @Override
    @Cacheable("ref.categoriasFinanceiras")
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
    @Cacheable("ref.formasPagamento")
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
        return toDomain(entity, null, null);
    }

    private LancamentoFinanceiro toDomain(FinanceiroLancamentoEntidade entity, Map<UUID, String> categorias, Map<UUID, String> formas) {
        String categoriaNome = categorias != null
                ? categorias.getOrDefault(entity.getCategoriaId(), "")
                : categoriaNome(entity.getCategoriaId());
        String formaNome = formas != null
                ? formas.getOrDefault(entity.getFormaPagamentoId(), "")
                : formaNome(entity.getFormaPagamentoId());
        return new LancamentoFinanceiro(
                PersistenciaIds.toString(entity.getId()),
                LancamentoFinanceiro.Tipo.from(entity.getTipo()),
                PersistenciaIds.toString(entity.getCategoriaId()),
                categoriaNome,
                PersistenciaIds.toString(entity.getFormaPagamentoId()),
                formaNome,
                entity.getDescricao() == null || entity.getDescricao().isBlank() ? "Lancamento financeiro" : entity.getDescricao(),
                PersistenciaIds.toString(entity.getClienteId()),
                PersistenciaIds.toString(entity.getOrdemServicoId()),
                PersistenciaIds.toString(entity.getContratoId()),
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

    private Map<UUID, String> nomesCategorias() {
        return categoriaRepository.findAll().stream()
                .collect(Collectors.toMap(
                        FinanceiroCategoriaEntidade::getId,
                        entity -> entity.getNome() == null ? "" : entity.getNome(),
                        (left, right) -> left));
    }

    private Map<UUID, String> nomesFormas() {
        return formaPagamentoRepository.findAll().stream()
                .collect(Collectors.toMap(
                        FinanceiroFormaPagamentoEntidade::getId,
                        entity -> entity.getNome() == null ? "" : entity.getNome(),
                        (left, right) -> left));
    }
}

@Repository
@Profile("memoria")
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
    public List<LancamentoFinanceiro> findByContratoId(String contratoId) {
        return storage.values().stream()
                .filter(lancamento -> lancamento.contratoId().equals(contratoId))
                .toList();
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
