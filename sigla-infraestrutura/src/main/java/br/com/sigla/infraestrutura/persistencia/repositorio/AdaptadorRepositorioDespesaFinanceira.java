package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.financeiro.porta.saida.RepositorioDespesaFinanceira;
import br.com.sigla.dominio.financeiro.DespesaFinanceira;
import br.com.sigla.infraestrutura.persistencia.PersistenciaIds;
import br.com.sigla.infraestrutura.persistencia.entidade.FinanceiroCategoriaEntidade;
import br.com.sigla.infraestrutura.persistencia.entidade.FinanceiroFormaPagamentoEntidade;
import br.com.sigla.infraestrutura.persistencia.entidade.FinanceiroLancamentoEntidade;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class AdaptadorRepositorioDespesaFinanceira implements RepositorioDespesaFinanceira {

    private final SpringDataRepositorioFinanceiroLancamento repository;
    private final SpringDataRepositorioFinanceiroCategoria categoriaRepository;
    private final SpringDataRepositorioFinanceiroFormaPagamento formaPagamentoRepository;

    public AdaptadorRepositorioDespesaFinanceira(
            SpringDataRepositorioFinanceiroLancamento repository,
            SpringDataRepositorioFinanceiroCategoria categoriaRepository,
            SpringDataRepositorioFinanceiroFormaPagamento formaPagamentoRepository
    ) {
        this.repository = repository;
        this.categoriaRepository = categoriaRepository;
        this.formaPagamentoRepository = formaPagamentoRepository;
    }

    @Override
    public void save(DespesaFinanceira expense) {
        FinanceiroLancamentoEntidade entity = new FinanceiroLancamentoEntidade();
        entity.setId(PersistenciaIds.toUuid(expense.id()));
        entity.setTipo("EXPENSE");
        entity.setCategoriaId(resolveCategoria("EXPENSE", expense.category().name()));
        entity.setDescricao(expense.description());
        entity.setValorTotal(expense.amount());
        entity.setDataEmissao(expense.expenseDate());
        entity.setDataVencimento(expense.dueDate());
        entity.setDataPagamento(expense.paymentDate());
        entity.setStatus(expense.status().name());
        entity.setFormaPagamentoId(resolveFormaPagamento(expense.paymentMethod().isBlank() ? "NAO_INFORMADO" : expense.paymentMethod()));
        entity.setParcelado(false);
        entity.setQuantidadeParcelas(1);
        entity.setObservacoes(expense.notes());
        entity.setCriadoPor(PersistenciaIds.toUuidIfValid(expense.createdBy()));
        entity.setOrdemServicoId(PersistenciaIds.toUuid(expense.orderReference()));
        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DespesaFinanceira> findById(String id) {
        return repository.findById(PersistenciaIds.toUuid(id))
                .filter(entity -> "EXPENSE".equals(entity.getTipo()))
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DespesaFinanceira> findAll() {
        Map<UUID, String> categorias = nomesCategorias();
        Map<UUID, String> formas = nomesFormas();
        return repository.findByTipo("EXPENSE").stream()
                .map(entity -> toDomain(entity, categorias, formas))
                .toList();
    }

    private DespesaFinanceira toDomain(FinanceiroLancamentoEntidade entity) {
        return toDomain(entity, null, null);
    }

    private DespesaFinanceira toDomain(FinanceiroLancamentoEntidade entity, Map<UUID, String> categorias, Map<UUID, String> formas) {
        String categoriaNome = categorias != null
                ? categorias.getOrDefault(entity.getCategoriaId(), "")
                : resolveCategoriaNome(entity.getCategoriaId());
        String formaNome = formas != null
                ? formas.getOrDefault(entity.getFormaPagamentoId(), "")
                : resolveFormaPagamentoNome(entity.getFormaPagamentoId());
        return new DespesaFinanceira(
                PersistenciaIds.toString(entity.getId()),
                parseCategory(categoriaNome),
                entity.getValorTotal(),
                entity.getDataEmissao(),
                PersistenciaIds.toString(entity.getCriadoPor()).isBlank() ? "Sistema" : PersistenciaIds.toString(entity.getCriadoPor()),
                entity.getDescricao(),
                entity.getDataVencimento(),
                entity.getDataPagamento(),
                formaNome,
                PersistenciaIds.toString(entity.getCriadoPor()),
                PersistenciaIds.toString(entity.getOrdemServicoId()),
                parseStatus(entity.getStatus()),
                entity.getObservacoes()
        );
    }

    private DespesaFinanceira.ExpenseCategory parseCategory(String value) {
        if (value == null || value.isBlank()) {
            return DespesaFinanceira.ExpenseCategory.EXTRAS;
        }
        try {
            return DespesaFinanceira.ExpenseCategory.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return DespesaFinanceira.ExpenseCategory.EXTRAS;
        }
    }

    private DespesaFinanceira.ExpenseStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return DespesaFinanceira.ExpenseStatus.PENDING;
        }
        return switch (value.trim().toUpperCase()) {
            case "PAGO", "PAGA", "PAID" -> DespesaFinanceira.ExpenseStatus.PAID;
            case "CANCELADO", "CANCELADA" -> DespesaFinanceira.ExpenseStatus.CANCELLED;
            default -> DespesaFinanceira.ExpenseStatus.PENDING;
        };
    }

    private UUID resolveCategoria(String tipo, String nome) {
        String normalized = nome == null || nome.isBlank() ? "EXTRAS" : nome.trim();
        return categoriaRepository.findByTipoAndNomeIgnoreCase(tipo, normalized)
                .map(FinanceiroCategoriaEntidade::getId)
                .orElseGet(() -> {
                    FinanceiroCategoriaEntidade entity = new FinanceiroCategoriaEntidade();
                    entity.setId(UUID.randomUUID());
                    entity.setTipo(tipo);
                    entity.setNome(normalized);
                    entity.setAtivo(true);
                    return categoriaRepository.save(entity).getId();
                });
    }

    private UUID resolveFormaPagamento(String nome) {
        String normalized = nome == null || nome.isBlank() ? "NAO_INFORMADO" : nome.trim();
        return formaPagamentoRepository.findByNomeIgnoreCase(normalized)
                .map(FinanceiroFormaPagamentoEntidade::getId)
                .orElseGet(() -> {
                    FinanceiroFormaPagamentoEntidade entity = new FinanceiroFormaPagamentoEntidade();
                    entity.setId(UUID.randomUUID());
                    entity.setNome(normalized);
                    entity.setAtivo(true);
                    return formaPagamentoRepository.save(entity).getId();
                });
    }

    private String resolveCategoriaNome(UUID id) {
        return id == null ? "" : categoriaRepository.findById(id).map(FinanceiroCategoriaEntidade::getNome).orElse("");
    }

    private String resolveFormaPagamentoNome(UUID id) {
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
class InMemoryAdaptadorRepositorioDespesaFinanceira implements RepositorioDespesaFinanceira {

    private final Map<String, DespesaFinanceira> storage = new ConcurrentHashMap<>();

    @Override
    public void save(DespesaFinanceira expense) {
        storage.put(expense.id(), expense);
    }

    @Override
    public Optional<DespesaFinanceira> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<DespesaFinanceira> findAll() {
        return storage.values().stream().toList();
    }
}
