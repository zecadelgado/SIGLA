package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.potenciaisclientes.porta.saida.RepositorioPotencialCliente;
import br.com.sigla.dominio.potenciaisclientes.PotencialCliente;
import br.com.sigla.infraestrutura.persistencia.PersistenciaIds;
import br.com.sigla.infraestrutura.persistencia.entidade.PotencialClienteEntidade;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnBean(SpringDataRepositorioPotencialCliente.class)
public class AdaptadorRepositorioPotencialCliente implements RepositorioPotencialCliente {

    private final SpringDataRepositorioPotencialCliente repository;

    public AdaptadorRepositorioPotencialCliente(SpringDataRepositorioPotencialCliente repository) {
        this.repository = repository;
    }

    @Override
    public void save(PotencialCliente lead) {
        repository.save(toEntity(lead));
    }

    @Override
    public List<PotencialCliente> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<PotencialCliente> findById(String id) {
        return repository.findById(PersistenciaIds.toUuid(id)).map(this::toDomain);
    }

    @Override
    public Optional<PotencialCliente> findConvertedByClienteId(String clienteId) {
        if (clienteId == null || clienteId.isBlank()) {
            return Optional.empty();
        }
        return repository.findAll().stream()
                .map(this::toDomain)
                .filter(lead -> lead.status().isConvertido())
                .filter(lead -> lead.observacoes().contains("Cliente gerado: " + clienteId))
                .findFirst();
    }

    private PotencialCliente toDomain(PotencialClienteEntidade entity) {
        LocalDate data = entity.getDataIndicacao() == null ? LocalDate.now() : entity.getDataIndicacao();
        String indicadorId = PersistenciaIds.toString(entity.getClienteIndicadorId());
        String observacoes = entity.getObservacoes() == null ? "" : entity.getObservacoes();
        return new PotencialCliente(
                PersistenciaIds.toString(entity.getId()),
                entity.getNomeIndicado(),
                entity.getTelefone() == null ? "" : entity.getTelefone(),
                indicadorId.isBlank() ? "INDICACAO" : "INDICACAO:" + indicadorId,
                parseStatus(entity.getStatus()),
                List.of(new PotencialCliente.Interaction(data, "Indicacao", observacoes.isBlank() ? "Indicacao cadastrada." : observacoes)),
                indicadorId,
                data,
                observacoes
        );
    }

    private PotencialClienteEntidade toEntity(PotencialCliente lead) {
        PotencialClienteEntidade entity = new PotencialClienteEntidade();
        entity.setId(PersistenciaIds.toUuid(lead.id()));
        entity.setNomeIndicado(lead.name());
        entity.setTelefone(lead.contact());
        entity.setClienteIndicadorId(PersistenciaIds.toUuid(extractCustomerId(lead.origin())));
        entity.setDataIndicacao(lead.dataIndicacao());
        entity.setStatus(PotencialCliente.PotencialClienteStatus.normalizar(lead.status()).name().toLowerCase());
        entity.setObservacoes(lead.observacoes());
        return entity;
    }

    private String extractCustomerId(String origin) {
        if (origin == null || !origin.contains(":")) {
            return "";
        }
        return origin.substring(origin.indexOf(':') + 1).trim();
    }

    private PotencialCliente.PotencialClienteStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return PotencialCliente.PotencialClienteStatus.NEW;
        }
        try {
            return PotencialCliente.PotencialClienteStatus.from(value);
        } catch (IllegalArgumentException exception) {
            return PotencialCliente.PotencialClienteStatus.NOVO;
        }
    }
}

@Repository
@ConditionalOnMissingBean(SpringDataRepositorioPotencialCliente.class)
class InMemoryAdaptadorRepositorioPotencialCliente implements RepositorioPotencialCliente {

    private final Map<String, PotencialCliente> storage = new ConcurrentHashMap<>();

    @Override
    public void save(PotencialCliente lead) {
        storage.put(lead.id(), lead);
    }

    @Override
    public List<PotencialCliente> findAll() {
        return storage.values().stream().toList();
    }

    @Override
    public Optional<PotencialCliente> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<PotencialCliente> findConvertedByClienteId(String clienteId) {
        return storage.values().stream()
                .filter(lead -> lead.status().isConvertido())
                .filter(lead -> lead.observacoes().contains("Cliente gerado: " + clienteId))
                .findFirst();
    }
}

interface SpringDataRepositorioPotencialCliente extends JpaRepository<PotencialClienteEntidade, UUID> {
}
