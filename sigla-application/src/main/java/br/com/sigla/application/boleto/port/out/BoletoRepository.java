package br.com.sigla.application.boleto.port.out;

import br.com.sigla.domain.boleto.BoletoAgreement;

public interface BoletoRepository {

    void save(BoletoAgreement agreement);
}
