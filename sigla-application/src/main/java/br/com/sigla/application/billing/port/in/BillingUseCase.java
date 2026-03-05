package br.com.sigla.application.billing.port.in;

import br.com.sigla.application.billing.dto.BillingEntryCommand;

import java.math.BigDecimal;

public interface BillingUseCase {

    void registerDebit(BillingEntryCommand command);

    void registerCredit(BillingEntryCommand command);

    BigDecimal getBalance(String accountId);
}
