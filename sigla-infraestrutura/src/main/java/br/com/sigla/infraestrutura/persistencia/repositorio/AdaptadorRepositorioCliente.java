package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.clientes.porta.saida.RepositorioCliente;
import br.com.sigla.dominio.clientes.Cliente;
import br.com.sigla.infraestrutura.persistencia.PersistenciaIds;
import br.com.sigla.infraestrutura.persistencia.entidade.ClienteEntidade;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
        return repository.findById(PersistenciaIds.toUuid(id)).map(this::toDomain);
    }

    private Cliente toDomain(ClienteEntidade entity) {
        return new Cliente(
                PersistenciaIds.toString(entity.getId()),
                prefer(entity.getNome(), prefer(entity.getNomeFantasia(), entity.getRazaoSocial())),
                entity.getRazaoSocial(),
                entity.getNomeFantasia(),
                entity.getCpf(),
                entity.getCnpj(),
                entity.getTelefonePrincipal(),
                entity.getEmail(),
                entity.getCep(),
                entity.getRua(),
                entity.getNumero(),
                entity.getComplemento(),
                entity.getBairro(),
                entity.getCidade(),
                entity.getEstado(),
                entity.getResponsaveis().stream()
                        .map(contact -> new Cliente.ContactPerson(contact.getNome(), contact.getCargo(), prefer(contact.getTelefone(), contact.getEmail())))
                        .toList(),
                entity.getObservacoes(),
                entity.isAtivo()
        );
    }

    private ClienteEntidade toEntity(Cliente customer) {
        ClienteEntidade entity = new ClienteEntidade();
        entity.setId(PersistenciaIds.toUuid(customer.id()));
        entity.setTipo("CLIENTE");
        entity.setNome(customer.name());
        entity.setRazaoSocial(customer.razaoSocial());
        entity.setNomeFantasia(customer.nomeFantasia());
        entity.setCpf(customer.cpf());
        entity.setCnpj(customer.cnpj());
        entity.setTelefonePrincipal(customer.phone());
        entity.setEmail(customer.email());
        entity.setCep(customer.cep());
        entity.setRua(customer.rua());
        entity.setNumero(customer.numero());
        entity.setComplemento(customer.complemento());
        entity.setBairro(customer.bairro());
        entity.setCidade(customer.cidade());
        entity.setEstado(customer.estado());
        entity.setObservacoes(customer.notes());
        entity.setAtivo(customer.ativo());
        List<ClienteEntidade.ResponsavelEntidade> contacts = new ArrayList<>();
        boolean principal = true;
        for (Cliente.ContactPerson contact : customer.contacts()) {
            ClienteEntidade.ResponsavelEntidade embeddable = new ClienteEntidade.ResponsavelEntidade();
            embeddable.setId(UUID.randomUUID());
            embeddable.setNome(contact.name());
            embeddable.setCargo(contact.role());
            embeddable.setTelefone(contact.contact());
            embeddable.setEmail("");
            embeddable.setPrincipal(principal);
            principal = false;
            contacts.add(embeddable);
        }
        entity.setResponsaveis(contacts);
        return entity;
    }

    private String prefer(String first, String second) {
        return first == null || first.isBlank() ? second : first;
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

interface SpringDataRepositorioCliente extends JpaRepository<ClienteEntidade, UUID> {
}

