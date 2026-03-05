package br.com.sigla.infrastructure.persistence.repository;

import br.com.sigla.application.credit.port.out.CreditRepository;
import br.com.sigla.domain.credit.CreditInstallmentPlan;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryCreditRepositoryAdapter implements CreditRepository {

    private final Map<String, CreditInstallmentPlan> storage = new ConcurrentHashMap<>();

    @Override
    public void save(CreditInstallmentPlan plan) {
        storage.put(plan.planId(), plan);
    }
}
