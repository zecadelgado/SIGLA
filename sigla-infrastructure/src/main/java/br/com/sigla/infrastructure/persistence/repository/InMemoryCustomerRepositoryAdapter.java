package br.com.sigla.infrastructure.persistence.repository;

import br.com.sigla.application.customer.port.out.CustomerRepository;
import br.com.sigla.domain.customer.CustomerProfile;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryCustomerRepositoryAdapter implements CustomerRepository {

    private final Map<String, CustomerProfile> storage = new ConcurrentHashMap<>();

    @Override
    public void save(CustomerProfile customerProfile) {
        storage.put(customerProfile.customerId(), customerProfile);
    }

    @Override
    public Optional<CustomerProfile> findById(String customerId) {
        return Optional.ofNullable(storage.get(customerId));
    }
}
