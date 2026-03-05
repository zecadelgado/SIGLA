package br.com.sigla.application.credit.port.in;

import br.com.sigla.application.credit.dto.CreateCreditPlanCommand;
import br.com.sigla.domain.credit.CreditInstallmentPlan;

public interface CreditUseCase {

    CreditInstallmentPlan createPlan(CreateCreditPlanCommand command);
}
