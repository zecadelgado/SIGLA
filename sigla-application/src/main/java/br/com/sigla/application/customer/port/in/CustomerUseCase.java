package br.com.sigla.application.customer.port.in;

import br.com.sigla.application.customer.dto.RegisterCustomerCommand;
import br.com.sigla.domain.customer.CustomerProfile;

import java.util.Optional;

public interface CustomerUseCase {

    void register(RegisterCustomerCommand command);

    Optional<CustomerProfile> findById(String customerId);
}
