package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.funcionarios.porta.saida.RepositorioFuncionario;
import br.com.sigla.dominio.funcionarios.Funcionario;
import br.com.sigla.infraestrutura.persistencia.PersistenciaIds;
import br.com.sigla.infraestrutura.persistencia.entidade.ClienteEntidade;
import jakarta.persistence.EntityManager;
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
    private final EntityManager entityManager;

    public AdaptadorRepositorioFuncionario(SpringDataRepositorioFuncionario repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public void save(Funcionario employee) {
        repository.save(toEntity(employee));
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(PersistenciaIds.toUuid(id));
    }

    @Override
    public List<Funcionario> findAll() {
        return repository.findByTipo("FUNCIONARIO").stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Funcionario> findById(String id) {
        return repository.findById(PersistenciaIds.toUuid(id)).filter(entity -> "FUNCIONARIO".equals(entity.getTipo())).map(this::toDomain);
    }

    @Override
    public boolean hasLinkedRecords(String id) {
        UUID uuid = PersistenciaIds.toUuid(id);
        return count("select count(*) from ordens_servico where responsavel_interno_id = :id or executado_por_id = :id", uuid) > 0
                || count("select count(*) from agenda_eventos where responsavel_id = :id", uuid) > 0
                || count("select count(*) from estoque_movimentacoes where funcionario_id = :id", uuid) > 0;
    }

    private Funcionario toDomain(ClienteEntidade entity) {
        String telefone = blankAs(entity.getTelefonePrincipal(), "");
        String email = blankAs(entity.getEmail(), "");
        if (telefone.isBlank() && email.isBlank()) {
            telefone = "-";
        }
        // role: coluna cargo (atual) com fallback para observacoes (linhas legadas).
        String cargo = blankAs(entity.getCargo(), blankAs(entity.getObservacoes(), "Equipe"));
        return new Funcionario(
                PersistenciaIds.toString(entity.getId()),
                entity.getNome(),
                blankAs(entity.getCpf(), ""),
                cargo,
                telefone,
                email,
                blankAs(entity.getCep(), ""),
                blankAs(entity.getRua(), ""),
                blankAs(entity.getNumero(), ""),
                blankAs(entity.getComplemento(), ""),
                blankAs(entity.getBairro(), ""),
                blankAs(entity.getCidade(), ""),
                blankAs(entity.getEstado(), ""),
                statusDe(entity.getSituacao(), entity.isAtivo())
        );
    }

    private ClienteEntidade toEntity(Funcionario employee) {
        ClienteEntidade entity = new ClienteEntidade();
        entity.setId(PersistenciaIds.toUuid(employee.id()));
        entity.setTipo("FUNCIONARIO");
        entity.setNome(employee.name());
        entity.setNomeFantasia(employee.name());
        entity.setCpf(employee.cpf());
        entity.setTelefonePrincipal(employee.telefone());
        entity.setEmail(employee.email());
        entity.setCep(employee.cep());
        entity.setRua(employee.rua());
        entity.setNumero(employee.numero());
        entity.setComplemento(employee.complemento());
        entity.setBairro(employee.bairro());
        entity.setCidade(employee.cidade());
        entity.setEstado(employee.estado());
        entity.setCargo(employee.role());
        entity.setSituacao(situacaoDe(employee.status()));
        // "Afastado" continua contando como ativo no boolean; apenas INATIVO desativa.
        entity.setAtivo(employee.status() != Funcionario.FuncionarioStatus.INACTIVE);
        return entity;
    }

    private String situacaoDe(Funcionario.FuncionarioStatus status) {
        return switch (status) {
            case INACTIVE -> "INATIVO";
            case ON_LEAVE -> "AFASTADO";
            case ACTIVE -> "ATIVO";
        };
    }

    private Funcionario.FuncionarioStatus statusDe(String situacao, boolean ativo) {
        if (situacao == null || situacao.isBlank()) {
            return ativo ? Funcionario.FuncionarioStatus.ACTIVE : Funcionario.FuncionarioStatus.INACTIVE;
        }
        return switch (situacao.trim().toUpperCase()) {
            case "INATIVO", "INACTIVE" -> Funcionario.FuncionarioStatus.INACTIVE;
            case "AFASTADO", "ON_LEAVE" -> Funcionario.FuncionarioStatus.ON_LEAVE;
            default -> Funcionario.FuncionarioStatus.ACTIVE;
        };
    }

    private String blankAs(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private long count(String sql, UUID id) {
        Number result = (Number) entityManager.createNativeQuery(sql)
                .setParameter("id", id)
                .getSingleResult();
        return result.longValue();
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
    public void deleteById(String id) {
        storage.remove(id);
    }

    @Override
    public List<Funcionario> findAll() {
        return storage.values().stream().toList();
    }

    @Override
    public Optional<Funcionario> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public boolean hasLinkedRecords(String id) {
        return false;
    }
}

interface SpringDataRepositorioFuncionario extends JpaRepository<ClienteEntidade, UUID> {
    List<ClienteEntidade> findByTipo(String tipo);
}
