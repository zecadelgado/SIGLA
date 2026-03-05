package br.com.sigla.infrastructure.persistence.repository;

import br.com.sigla.application.boleto.port.out.BoletoRepository;
import br.com.sigla.domain.boleto.BoletoAgreement;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryBoletoRepositoryAdapter implements BoletoRepository {

    private final Map<String, BoletoAgreement> storage = new ConcurrentHashMap<>();

    @Override
    public void save(BoletoAgreement agreement) {
        storage.put(agreement.agreementId(), agreement);
    }
}
