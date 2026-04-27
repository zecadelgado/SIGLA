package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.financeiro.porta.saida.RepositorioPlanoParcelamento;
import br.com.sigla.dominio.financeiro.PlanoParcelamento;
import br.com.sigla.infraestrutura.persistencia.PersistenciaIds;
import br.com.sigla.infraestrutura.persistencia.entidade.FinanceiroLancamentoEntidade;
import br.com.sigla.infraestrutura.persistencia.entidade.FinanceiroParcelaEntidade;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnBean(SpringDataRepositorioFinanceiroLancamento.class)
public class AdaptadorRepositorioPlanoParcelamento implements RepositorioPlanoParcelamento {

    private final SpringDataRepositorioFinanceiroLancamento repository;

    public AdaptadorRepositorioPlanoParcelamento(SpringDataRepositorioFinanceiroLancamento repository) {
        this.repository = repository;
    }

    @Override
    public void save(PlanoParcelamento plan) {
        UUID lancamentoId = PersistenciaIds.toUuid(plan.id().replace("-plan", ""));
        FinanceiroLancamentoEntidade lancamento = repository.findById(lancamentoId).orElse(null);
        if (lancamento == null) {
            return;
        }
        lancamento.setParcelado(plan.totalInstallments() > 1);
        lancamento.setQuantidadeParcelas(plan.totalInstallments());
        lancamento.getParcelas().clear();
        BigDecimal valorParcela = plan.totalAmount().divide(BigDecimal.valueOf(plan.totalInstallments()), 2, RoundingMode.HALF_UP);
        LocalDate base = plan.nextDueDate();
        for (int index = 1; index <= plan.totalInstallments(); index++) {
            FinanceiroParcelaEntidade parcela = new FinanceiroParcelaEntidade();
            parcela.setId(UUID.randomUUID());
            parcela.setNumeroParcela(index);
            parcela.setValorParcela(valorParcela);
            parcela.setDataVencimento(base.plusMonths(index - 1L));
            parcela.setStatus(index <= plan.paidInstallments() ? "PAID" : plan.status().name());
            lancamento.getParcelas().add(parcela);
        }
        repository.save(lancamento);
    }

    @Override
    public List<PlanoParcelamento> findAll() {
        return repository.findAll().stream()
                .filter(FinanceiroLancamentoEntidade::isParcelado)
                .map(entity -> {
                    int total = entity.getParcelas().size();
                    long paid = entity.getParcelas().stream().filter(parcela -> "PAID".equalsIgnoreCase(parcela.getStatus())).count();
                    LocalDate nextDueDate = entity.getParcelas().stream()
                            .filter(parcela -> !"PAID".equalsIgnoreCase(parcela.getStatus()))
                            .map(FinanceiroParcelaEntidade::getDataVencimento)
                            .sorted()
                            .findFirst()
                            .orElse(entity.getDataVencimento() == null ? entity.getDataEmissao() : entity.getDataVencimento());
                    return new PlanoParcelamento(
                            PersistenciaIds.toString(entity.getId()) + "-plan",
                            PersistenciaIds.toString(entity.getClienteId()),
                            entity.getValorTotal(),
                            Math.max(total, 1),
                            (int) paid,
                            paid == total ? PlanoParcelamento.InstallmentStatus.PAID : PlanoParcelamento.InstallmentStatus.ACTIVE,
                            nextDueDate
                    );
                })
                .toList();
    }

    @Override
    public Optional<PlanoParcelamento> findById(String id) {
        return findAll().stream().filter(plan -> plan.id().equals(id)).findFirst();
    }
}

@Repository
@ConditionalOnMissingBean(SpringDataRepositorioFinanceiroLancamento.class)
class InMemoryAdaptadorRepositorioPlanoParcelamento implements RepositorioPlanoParcelamento {

    private final Map<String, PlanoParcelamento> storage = new ConcurrentHashMap<>();

    @Override
    public void save(PlanoParcelamento plan) {
        storage.put(plan.id(), plan);
    }

    @Override
    public List<PlanoParcelamento> findAll() {
        return storage.values().stream().toList();
    }

    @Override
    public Optional<PlanoParcelamento> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }
}
