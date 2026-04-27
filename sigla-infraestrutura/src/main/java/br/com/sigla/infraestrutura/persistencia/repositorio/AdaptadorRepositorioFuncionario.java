package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.funcionarios.porta.saida.RepositorioFuncionario;
import br.com.sigla.dominio.funcionarios.Funcionario;
import br.com.sigla.infraestrutura.persistencia.PersistenciaIds;
import br.com.sigla.infraestrutura.persistencia.entidade.ClienteEntidade;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnBean(SpringDataRepositorioFuncionario.class)
public class AdaptadorRepositorioFuncionario implements RepositorioFuncionario {

    private final SpringDataRepositorioFuncionario repository;

    public AdaptadorRepositorioFuncionario(SpringDataRepositorioFuncionario repository) {
        this.repository = repository;
    }

    @Override
    public void save(Funcionario employee) {
        repository.save(toEntity(employee));
    }

    @Override
    public List<Funcionario> findAll() {
        return repository.findByTipo("FUNCIONARIO").stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Funcionario> findById(String id) {
        return repository.findById(PersistenciaIds.toUuid(id)).filter(entity -> "FUNCIONARIO".equals(entity.getTipo())).map(this::toDomain);
    }

    private Funcionario toDomain(ClienteEntidade entity) {
        return new Funcionario(
                PersistenciaIds.toString(entity.getId()),
                entity.getNome(),
                blankAs(entity.getObservacoes(), "Equipe"),
                blankAs(entity.getTelefonePrincipal(), entity.getEmail()),
                entity.isAtivo() ? Funcionario.FuncionarioStatus.ACTIVE : Funcionario.FuncionarioStatus.INACTIVE
        );
    }

    private ClienteEntidade toEntity(Funcionario employee) {
        ClienteEntidade entity = new ClienteEntidade();
        entity.setId(PersistenciaIds.toUuid(employee.id()));
        entity.setTipo("FUNCIONARIO");
        entity.setNome(employee.name());
        entity.setNomeFantasia(employee.name());
        entity.setTelefonePrincipal(employee.contact());
        entity.setEmail(employee.contact().contains("@") ? employee.contact() : "");
        entity.setObservacoes(employee.role());
        entity.setAtivo(employee.status() == Funcionario.FuncionarioStatus.ACTIVE);
        return entity;
    }

    private String blankAs(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}

@Repository
@ConditionalOnMissingBean(SpringDataRepositorioFuncionario.class)
class InMemoryAdaptadorRepositorioFuncionario implements RepositorioFuncionario {

    private final Map<String, Funcionario> storage = new ConcurrentHashMap<>();

    @Override
    public void save(Funcionario employee) {
        storage.put(employee.id(), employee);
    }

    @Override
    public List<Funcionario> findAll() {
        return storage.values().stream().toList();
    }

    @Override
    public Optional<Funcionario> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }
}

interface SpringDataRepositorioFuncionario extends JpaRepository<ClienteEntidade, UUID> {
    List<ClienteEntidade> findByTipo(String tipo);
}
