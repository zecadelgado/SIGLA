package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.clientes.porta.saida.RepositorioCliente;
import br.com.sigla.dominio.clientes.Cliente;
import br.com.sigla.infraestrutura.persistencia.entidade.ClienteEntidade;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnBean(SpringDataRepositorioCliente.class)
public class AdaptadorRepositorioCliente implements RepositorioCliente {

    private final SpringDataRepositorioCliente repository;

    public AdaptadorRepositorioCliente(SpringDataRepositorioCliente repository) {
        this.repository = repository;
    }

    @Override
    public void save(Cliente customer) {
        repository.save(toEntity(customer));
    }

    @Override
    public List<Cliente> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Cliente> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    private Cliente toDomain(ClienteEntidade entity) {
        return new Cliente(
                entity.getId(),
                entity.getName(),
                entity.getLocation(),
                entity.getCnpj(),
                entity.getPhone(),
                entity.getContacts().stream()
                        .map(contact -> new Cliente.ContactPerson(contact.getName(), contact.getRole(), contact.getContact()))
                        .toList(),
                entity.getNotes()
        );
    }

    private ClienteEntidade toEntity(Cliente customer) {
        ClienteEntidade entity = new ClienteEntidade();
        entity.setId(customer.id());
        entity.setName(customer.name());
        entity.setLocation(customer.location());
        entity.setCnpj(customer.cnpj());
        entity.setPhone(customer.phone());
        entity.setNotes(customer.notes());
        List<ClienteEntidade.ContactEmbeddable> contacts = new ArrayList<>();
        for (Cliente.ContactPerson contact : customer.contacts()) {
            ClienteEntidade.ContactEmbeddable embeddable = new ClienteEntidade.ContactEmbeddable();
            embeddable.setName(contact.name());
            embeddable.setRole(contact.role());
            embeddable.setContact(contact.contact());
            contacts.add(embeddable);
        }
        entity.setContacts(contacts);
        return entity;
    }
}

@Repository
@ConditionalOnMissingBean(SpringDataRepositorioCliente.class)
class InMemoryAdaptadorRepositorioCliente implements RepositorioCliente {

    private final Map<String, Cliente> storage = new ConcurrentHashMap<>();

    @Override
    public void save(Cliente customer) {
        storage.put(customer.id(), customer);
    }

    @Override
    public List<Cliente> findAll() {
        return storage.values().stream().toList();
    }

    @Override
    public Optional<Cliente> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }
}

interface SpringDataRepositorioCliente extends JpaRepository<ClienteEntidade, String> {
}

