package br.com.sigla.application.customer.usecase;

import br.com.sigla.application.customer.dto.RegisterCustomerCommand;
import br.com.sigla.application.customer.port.in.CustomerUseCase;
import br.com.sigla.application.customer.port.out.CustomerRepository;
import br.com.sigla.domain.customer.CustomerProfile;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RegisterCustomerUseCase implements CustomerUseCase {

    private final CustomerRepository repository;

    public RegisterCustomerUseCase(CustomerRepository repository) {
        this.repository = repository;
    }

    @Override
    public void register(RegisterCustomerCommand command) {
        CustomerProfile profile = new CustomerProfile(
                command.customerId(),
                command.fullName(),
                command.birthDate(),
                command.hasPrescription(),
                command.glassesType(),
                command.paymentUpToDate()
        );
        repository.save(profile);
    }

    @Override
    public Optional<CustomerProfile> findById(String customerId) {
        return repository.findById(customerId);
    }
}
