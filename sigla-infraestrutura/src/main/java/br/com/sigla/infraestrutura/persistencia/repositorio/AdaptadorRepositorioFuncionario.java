package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.funcionarios.porta.saida.RepositorioFuncionario;
import br.com.sigla.dominio.funcionarios.Funcionario;
import br.com.sigla.infraestrutura.persistencia.entidade.FuncionarioEntidade;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Funcionario> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    private Funcionario toDomain(FuncionarioEntidade entity) {
        return new Funcionario(entity.getId(), entity.getName(), entity.getRole(), entity.getContact(), entity.getStatus());
    }

    private FuncionarioEntidade toEntity(Funcionario employee) {
        FuncionarioEntidade entity = new FuncionarioEntidade();
        entity.setId(employee.id());
        entity.setName(employee.name());
        entity.setRole(employee.role());
        entity.setContact(employee.contact());
        entity.setStatus(employee.status());
        return entity;
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

interface SpringDataRepositorioFuncionario extends JpaRepository<FuncionarioEntidade, String> {
}

