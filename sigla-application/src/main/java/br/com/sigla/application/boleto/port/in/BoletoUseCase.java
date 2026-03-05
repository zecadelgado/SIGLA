package br.com.sigla.application.boleto.port.in;

import br.com.sigla.application.boleto.dto.GenerateBoletoCommand;
import br.com.sigla.domain.boleto.BoletoAgreement;

public interface BoletoUseCase {

    BoletoAgreement generate(GenerateBoletoCommand command);
}
