package br.com.sigla.application.credit.usecase;

import br.com.sigla.application.credit.dto.CreateCreditPlanCommand;
import br.com.sigla.application.credit.port.in.CreditUseCase;
import br.com.sigla.application.credit.port.out.CreditRepository;
import br.com.sigla.domain.credit.CreditInstallmentPlan;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class CreateCreditPlanUseCase implements CreditUseCase {

    private final CreditRepository repository;

    public CreateCreditPlanUseCase(CreditRepository repository) {
        this.repository = repository;
    }

    @Override
    public CreditInstallmentPlan createPlan(CreateCreditPlanCommand command) {
        List<CreditInstallmentPlan.Installment> installments = command.installments().stream()
                .map(input -> new CreditInstallmentPlan.Installment(
                        input.number(),
                        LocalDate.parse(input.dueDate()),
                        input.paid()
                ))
                .toList();

        CreditInstallmentPlan plan = new CreditInstallmentPlan(command.planId(), command.customerId(), installments);
        repository.save(plan);
        return plan;
    }
}
