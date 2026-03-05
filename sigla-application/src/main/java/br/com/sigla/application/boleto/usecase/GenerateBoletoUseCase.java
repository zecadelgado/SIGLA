package br.com.sigla.application.boleto.usecase;

import br.com.sigla.application.boleto.dto.GenerateBoletoCommand;
import br.com.sigla.application.boleto.port.in.BoletoUseCase;
import br.com.sigla.application.boleto.port.out.BoletoRepository;
import br.com.sigla.domain.boleto.BoletoAgreement;
import org.springframework.stereotype.Service;

@Service
public class GenerateBoletoUseCase implements BoletoUseCase {

    private final BoletoRepository repository;

    public GenerateBoletoUseCase(BoletoRepository repository) {
        this.repository = repository;
    }

    @Override
    public BoletoAgreement generate(GenerateBoletoCommand command) {
        BoletoAgreement agreement = new BoletoAgreement(command.agreementId(), command.dueDate(), false);
        repository.save(agreement);
        return agreement;
    }
}
