package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.clientes.porta.saida.RepositorioCliente;
import br.com.sigla.dominio.clientes.Cliente;
import br.com.sigla.infraestrutura.persistencia.PersistenciaIds;
import br.com.sigla.infraestrutura.persistencia.entidade.ClienteEntidade;
import jakarta.persistence.EntityManager;
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
    private final EntityManager entityManager;

    public AdaptadorRepositorioCliente(SpringDataRepositorioCliente repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public void save(Cliente customer) {
        repository.save(toEntity(customer));
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(PersistenciaIds.toUuid(id));
    }

    @Override
    public List<Cliente> findAll() {
        return repository.findAll().stream()
                .filter(entity -> !"FUNCIONARIO".equals(entity.getTipo()))
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Cliente> findById(String id) {
        return repository.findById(PersistenciaIds.toUuid(id))
                .filter(entity -> !"FUNCIONARIO".equals(entity.getTipo()))
                .map(this::toDomain);
    }

    @Override
    public boolean existsActiveCpf(String cpf, String exceptId) {
        String digits = onlyDigits(cpf);
        if (digits.isBlank()) {
            return false;
        }
        return findAll().stream()
                .filter(Cliente::ativo)
                .filter(cliente -> !cliente.id().equals(exceptId))
                .anyMatch(cliente -> onlyDigits(cliente.cpf()).equals(digits));
    }

    @Override
    public boolean existsActiveCnpj(String cnpj, String exceptId) {
        String digits = onlyDigits(cnpj);
        if (digits.isBlank()) {
            return false;
        }
        return findAll().stream()
                .filter(Cliente::ativo)
                .filter(cliente -> !cliente.id().equals(exceptId))
                .anyMatch(cliente -> onlyDigits(cliente.cnpj()).equals(digits));
    }

    @Override
    public boolean existsActiveEmail(String email, String exceptId) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        if (normalized.isBlank()) {
            return false;
        }
        return findAll().stream()
                .filter(Cliente::ativo)
                .filter(cliente -> !cliente.id().equals(exceptId))
                .anyMatch(cliente -> cliente.email().equalsIgnoreCase(normalized));
    }

    @Override
    public boolean hasLinkedRecords(String id) {
        UUID uuid = PersistenciaIds.toUuid(id);
        return count("select count(*) from ordens_servico where cliente_id = :id or responsavel_interno_id = :id or executado_por_id = :id", uuid) > 0
                || count("select count(*) from financeiro_lancamentos where cliente_id = :id", uuid) > 0
                || count("select count(*) from cliente_indicacoes where cliente_indicador_id = :id", uuid) > 0
                || count("select count(*) from contratos where cliente_id = :id", uuid) > 0
                || count("select count(*) from certificados where cliente_id = :id", uuid) > 0
                || count("select count(*) from agenda_eventos where cliente_id = :id or responsavel_id = :id", uuid) > 0
                || count("select count(*) from estoque_movimentacoes where cliente_id = :id or funcionario_id = :id", uuid) > 0;
    }

    private Cliente toDomain(ClienteEntidade entity) {
        return new Cliente(
                PersistenciaIds.toString(entity.getId()),
                tipoCliente(entity),
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
                        .map(contact -> new Cliente.ContactPerson(
                                PersistenciaIds.toString(contact.getId()),
                                contact.getNome(),
                                contact.getCargo(),
                                contact.getTelefone(),
                                contact.getEmail(),
                                contact.isPrincipal()
                        ))
                        .toList(),
                entity.getObservacoes(),
                entity.isAtivo()
        );
    }

    private ClienteEntidade toEntity(Cliente customer) {
        ClienteEntidade entity = new ClienteEntidade();
        entity.setId(PersistenciaIds.toUuid(customer.id()));
        entity.setTipo(customer.tipo().name().toLowerCase());
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
            embeddable.setId(PersistenciaIds.toUuidIfValid(contact.id()) == null ? UUID.randomUUID() : PersistenciaIds.toUuidIfValid(contact.id()));
            embeddable.setNome(contact.name());
            embeddable.setCargo(contact.role());
            embeddable.setTelefone(contact.phone());
            embeddable.setEmail(contact.email());
            embeddable.setPrincipal(contact.principal() && principal);
            if (embeddable.isPrincipal()) {
                principal = false;
            }
            contacts.add(embeddable);
        }
        entity.setResponsaveis(contacts);
        return entity;
    }

    private long count(String sql, UUID id) {
        Number result = (Number) entityManager.createNativeQuery(sql)
                .setParameter("id", id)
                .getSingleResult();
        return result.longValue();
    }

    private String prefer(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }

    private Cliente.TipoCliente tipoCliente(ClienteEntidade entity) {
        if ("CLIENTE".equals(entity.getTipo())) {
            return entity.getCnpj() == null || entity.getCnpj().isBlank()
                    ? Cliente.TipoCliente.PESSOA_FISICA
                    : Cliente.TipoCliente.PESSOA_JURIDICA;
        }
        return Cliente.TipoCliente.from(entity.getTipo());
    }

    private String onlyDigits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
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
    public void deleteById(String id) {
        storage.remove(id);
    }

    @Override
    public List<Cliente> findAll() {
        return storage.values().stream().toList();
    }

    @Override
    public Optional<Cliente> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public boolean existsActiveCpf(String cpf, String exceptId) {
        String digits = onlyDigits(cpf);
        return storage.values().stream()
                .filter(Cliente::ativo)
                .filter(cliente -> !cliente.id().equals(exceptId))
                .anyMatch(cliente -> onlyDigits(cliente.cpf()).equals(digits));
    }

    @Override
    public boolean existsActiveCnpj(String cnpj, String exceptId) {
        String digits = onlyDigits(cnpj);
        return storage.values().stream()
                .filter(Cliente::ativo)
                .filter(cliente -> !cliente.id().equals(exceptId))
                .anyMatch(cliente -> onlyDigits(cliente.cnpj()).equals(digits));
    }

    @Override
    public boolean existsActiveEmail(String email, String exceptId) {
        String normalized = email == null ? "" : email.trim();
        return storage.values().stream()
                .filter(Cliente::ativo)
                .filter(cliente -> !cliente.id().equals(exceptId))
                .anyMatch(cliente -> cliente.email().equalsIgnoreCase(normalized));
    }

    @Override
    public boolean hasLinkedRecords(String id) {
        return false;
    }

    private String onlyDigits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }
}

interface SpringDataRepositorioCliente extends JpaRepository<ClienteEntidade, UUID> {
}

