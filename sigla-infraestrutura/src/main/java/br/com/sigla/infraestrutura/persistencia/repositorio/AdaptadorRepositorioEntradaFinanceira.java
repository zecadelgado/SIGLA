package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.financeiro.porta.saida.RepositorioEntradaFinanceira;
import br.com.sigla.dominio.financeiro.EntradaFinanceira;
import br.com.sigla.infraestrutura.persistencia.PersistenciaIds;
import br.com.sigla.infraestrutura.persistencia.entidade.FinanceiroCategoriaEntidade;
import br.com.sigla.infraestrutura.persistencia.entidade.FinanceiroFormaPagamentoEntidade;
import br.com.sigla.infraestrutura.persistencia.entidade.FinanceiroLancamentoEntidade;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnBean(SpringDataRepositorioFinanceiroLancamento.class)
public class AdaptadorRepositorioEntradaFinanceira implements RepositorioEntradaFinanceira {

    private final SpringDataRepositorioFinanceiroLancamento repository;
    private final SpringDataRepositorioFinanceiroCategoria categoriaRepository;
    private final SpringDataRepositorioFinanceiroFormaPagamento formaPagamentoRepository;

    public AdaptadorRepositorioEntradaFinanceira(
            SpringDataRepositorioFinanceiroLancamento repository,
            SpringDataRepositorioFinanceiroCategoria categoriaRepository,
            SpringDataRepositorioFinanceiroFormaPagamento formaPagamentoRepository
    ) {
        this.repository = repository;
        this.categoriaRepository = categoriaRepository;
        this.formaPagamentoRepository = formaPagamentoRepository;
    }

    @Override
    public void save(EntradaFinanceira entry) {
        FinanceiroLancamentoEntidade entity = new FinanceiroLancamentoEntidade();
        entity.setId(PersistenciaIds.toUuid(entry.id()));
        entity.setTipo("ENTRY");
        entity.setCategoriaId(resolveCategoria("ENTRY", entry.category()));
        entity.setDescricao(entry.description().isBlank() ? entry.entryType().name() : entry.description());
        entity.setClienteId(PersistenciaIds.toUuid(entry.customerId()));
        entity.setOrdemServicoId(PersistenciaIds.toUuid(entry.orderReference()));
        entity.setValorTotal(entry.amount());
        entity.setDataEmissao(entry.entryDate());
        entity.setDataVencimento(entry.dueDate());
        entity.setDataPagamento(entry.paymentDate());
        entity.setStatus(entry.status().name());
        entity.setFormaPagamentoId(resolveFormaPagamento(entry.paymentMethod().isBlank() ? entry.entryType().name() : entry.paymentMethod()));
        entity.setParcelado(false);
        entity.setQuantidadeParcelas(1);
        entity.setObservacoes("");
        entity.setCriadoPor(PersistenciaIds.toUuidIfValid(entry.createdBy()));
        repository.save(entity);
    }

    @Override
    public Optional<EntradaFinanceira> findById(String id) {
        return findAll().stream()
                .filter(entry -> entry.id().equals(id))
                .findFirst();
    }

    @Override
    public List<EntradaFinanceira> findAll() {
        return repository.findByTipo("ENTRY").stream()
                .map(entity -> new EntradaFinanceira(
                        PersistenciaIds.toString(entity.getId()),
                        parseEntryType(resolveFormaPagamentoNome(entity.getFormaPagamentoId())),
                        entity.getValorTotal(),
                        entity.getDataEmissao(),
                        PersistenciaIds.toString(entity.getClienteId()),
                        "",
                        entity.getDescricao(),
                        resolveCategoriaNome(entity.getCategoriaId()),
                        entity.getDataVencimento(),
                        entity.getDataPagamento(),
                        resolveFormaPagamentoNome(entity.getFormaPagamentoId()),
                        PersistenciaIds.toString(entity.getCriadoPor()),
                        PersistenciaIds.toString(entity.getOrdemServicoId()),
                        parseEntryStatus(entity.getStatus())
                ))
                .toList();
    }

    private UUID resolveCategoria(String tipo, String nome) {
        String normalized = nome == null || nome.isBlank() ? "GERAL" : nome.trim();
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

    private EntradaFinanceira.EntryType parseEntryType(String value) {
        if (value == null || value.isBlank()) {
            return EntradaFinanceira.EntryType.PIX;
        }
        return switch (value.trim().toUpperCase()) {
            case "DINHEIRO", "CASH" -> EntradaFinanceira.EntryType.CASH;
            case "BOLETO" -> EntradaFinanceira.EntryType.BOLETO;
            case "CARTAO", "CARTÃO", "CARD" -> EntradaFinanceira.EntryType.CARD;
            default -> EntradaFinanceira.EntryType.PIX;
        };
    }

    private EntradaFinanceira.EntryStatus parseEntryStatus(String value) {
        if (value == null || value.isBlank()) {
            return EntradaFinanceira.EntryStatus.PENDING;
        }
        return switch (value.trim().toUpperCase()) {
            case "PAGO", "PAGA", "RECEBIDO", "RECEBIDA", "PAID", "RECEIVED" -> EntradaFinanceira.EntryStatus.RECEIVED;
            case "CANCELADO", "CANCELADA" -> EntradaFinanceira.EntryStatus.CANCELLED;
            default -> EntradaFinanceira.EntryStatus.PENDING;
        };
    }
}

@Repository
@ConditionalOnMissingBean(SpringDataRepositorioFinanceiroLancamento.class)
class InMemoryAdaptadorRepositorioEntradaFinanceira implements RepositorioEntradaFinanceira {

    private final Map<String, EntradaFinanceira> storage = new ConcurrentHashMap<>();

    @Override
    public void save(EntradaFinanceira entry) {
        storage.put(entry.id(), entry);
    }

    @Override
    public Optional<EntradaFinanceira> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<EntradaFinanceira> findAll() {
        return storage.values().stream().toList();
    }
}

interface SpringDataRepositorioFinanceiroLancamento extends JpaRepository<FinanceiroLancamentoEntidade, UUID> {
    List<FinanceiroLancamentoEntidade> findByTipo(String tipo);

    Optional<FinanceiroLancamentoEntidade> findByOrdemServicoId(UUID ordemServicoId);
}

interface SpringDataRepositorioFinanceiroCategoria extends JpaRepository<FinanceiroCategoriaEntidade, UUID> {
    Optional<FinanceiroCategoriaEntidade> findByTipoAndNomeIgnoreCase(String tipo, String nome);
}

interface SpringDataRepositorioFinanceiroFormaPagamento extends JpaRepository<FinanceiroFormaPagamentoEntidade, UUID> {
    Optional<FinanceiroFormaPagamentoEntidade> findByNomeIgnoreCase(String nome);
}
