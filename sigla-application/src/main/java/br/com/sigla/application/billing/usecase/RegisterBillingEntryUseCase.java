package br.com.sigla.application.billing.usecase;

import br.com.sigla.application.billing.dto.BillingEntryCommand;
import br.com.sigla.application.billing.port.in.BillingUseCase;
import br.com.sigla.application.billing.port.out.BillingRepository;
import br.com.sigla.domain.billing.BillingLedger;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RegisterBillingEntryUseCase implements BillingUseCase {

    private final BillingRepository repository;

    public RegisterBillingEntryUseCase(BillingRepository repository) {
        this.repository = repository;
    }

    @Override
    public void registerDebit(BillingEntryCommand command) {
        BillingLedger ledger = findOrCreate(command.accountId());
        ledger.recordDebit(command.amount());
        repository.save(ledger);
    }

    @Override
    public void registerCredit(BillingEntryCommand command) {
        BillingLedger ledger = findOrCreate(command.accountId());
        ledger.recordCredit(command.amount());
        repository.save(ledger);
    }

    @Override
    public BigDecimal getBalance(String accountId) {
        return repository.findByAccountId(accountId)
                .map(BillingLedger::balance)
                .orElse(BigDecimal.ZERO);
    }

    private BillingLedger findOrCreate(String accountId) {
        return repository.findByAccountId(accountId)
                .orElseGet(() -> new BillingLedger(accountId, BigDecimal.ZERO));
    }
}
