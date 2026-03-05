package br.com.sigla.application.credit.port.out;

import br.com.sigla.domain.credit.CreditInstallmentPlan;

public interface CreditRepository {

    void save(CreditInstallmentPlan plan);
}
