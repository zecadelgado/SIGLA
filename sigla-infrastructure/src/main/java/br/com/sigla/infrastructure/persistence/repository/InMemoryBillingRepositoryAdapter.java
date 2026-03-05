package br.com.sigla.infrastructure.persistence.repository;

import br.com.sigla.application.billing.port.out.BillingRepository;
import br.com.sigla.domain.billing.BillingLedger;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryBillingRepositoryAdapter implements BillingRepository {

    private final Map<String, BillingLedger> storage = new ConcurrentHashMap<>();

    @Override
    public Optional<BillingLedger> findByAccountId(String accountId) {
        return Optional.ofNullable(storage.get(accountId));
    }

    @Override
    public void save(BillingLedger ledger) {
        storage.put(ledger.accountId(), ledger);
    }
}
