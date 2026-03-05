package br.com.sigla.application.customer.port.out;

import br.com.sigla.domain.customer.CustomerProfile;

import java.util.Optional;

public interface CustomerRepository {

    void save(CustomerProfile customerProfile);

    Optional<CustomerProfile> findById(String customerId);
}
