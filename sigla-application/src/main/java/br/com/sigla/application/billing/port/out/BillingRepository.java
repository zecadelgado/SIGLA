package br.com.sigla.application.billing.port.out;

import br.com.sigla.domain.billing.BillingLedger;

import java.util.Optional;

public interface BillingRepository {

    Optional<BillingLedger> findByAccountId(String accountId);

    void save(BillingLedger ledger);
}
