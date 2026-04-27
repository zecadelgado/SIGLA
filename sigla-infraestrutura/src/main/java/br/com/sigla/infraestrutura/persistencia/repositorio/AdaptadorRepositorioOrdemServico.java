package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.servicos.porta.saida.RepositorioOrdemServico;
import br.com.sigla.dominio.servicos.OrdemServico;
import br.com.sigla.infraestrutura.persistencia.PersistenciaIds;
import br.com.sigla.infraestrutura.persistencia.entidade.OrdemServicoEntidade;
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
@ConditionalOnBean(SpringDataRepositorioOrdemServico.class)
public class AdaptadorRepositorioOrdemServico implements RepositorioOrdemServico {

    private final SpringDataRepositorioOrdemServico repository;

    public AdaptadorRepositorioOrdemServico(SpringDataRepositorioOrdemServico repository) {
        this.repository = repository;
    }

    @Override
    public OrdemServico save(OrdemServico ordemServico) {
        return toDomain(repository.save(toEntity(ordemServico)));
    }

    @Override
    public List<OrdemServico> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<OrdemServico> findById(String id) {
        return repository.findById(PersistenciaIds.toUuid(id)).map(this::toDomain);
    }

    private OrdemServico toDomain(OrdemServicoEntidade entity) {
        return new OrdemServico(
                PersistenciaIds.toString(entity.getId()),
                entity.getNumeroOs(),
                PersistenciaIds.toString(entity.getClienteId()),
                entity.getTitulo(),
                entity.getDescricao(),
                entity.getTipoServico(),
                parseStatus(entity.getStatus()),
                entity.getDataAgendada(),
                entity.getDataInicio(),
                entity.getDataFim(),
                PersistenciaIds.toString(entity.getResponsavelInternoId()),
                PersistenciaIds.toString(entity.getExecutadoPorId()),
                entity.isFoiFeito(),
                entity.isPago(),
                entity.getValorServico(),
                entity.getObservacoes()
        );
    }

    private OrdemServicoEntidade toEntity(OrdemServico ordemServico) {
        OrdemServicoEntidade entity = new OrdemServicoEntidade();
        entity.setId(PersistenciaIds.toUuid(ordemServico.id()));
        entity.setClienteId(PersistenciaIds.toUuid(ordemServico.clienteId()));
        entity.setTitulo(ordemServico.titulo());
        entity.setDescricao(ordemServico.descricao());
        entity.setTipoServico(ordemServico.tipoServico());
        entity.setStatus(ordemServico.status().name());
        entity.setDataAgendada(ordemServico.dataAgendada());
        entity.setDataInicio(ordemServico.dataInicio());
        entity.setDataFim(ordemServico.dataFim());
        entity.setResponsavelInternoId(PersistenciaIds.toUuid(ordemServico.responsavelInternoId()));
        entity.setExecutadoPorId(PersistenciaIds.toUuid(ordemServico.executadoPorId()));
        entity.setFoiFeito(ordemServico.foiFeito());
        entity.setPago(ordemServico.pago());
        entity.setValorServico(ordemServico.valorServico());
        entity.setObservacoes(ordemServico.observacoes());
        return entity;
    }

    private OrdemServico.OrdemServicoStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return OrdemServico.OrdemServicoStatus.AGENDADA;
        }
        return switch (value.trim().toUpperCase()) {
            case "AGENDADO", "AGENDADA" -> OrdemServico.OrdemServicoStatus.AGENDADA;
            case "CONCLUIDO", "CONCLUIDA" -> OrdemServico.OrdemServicoStatus.CONCLUIDA;
            case "CANCELADO", "CANCELADA" -> OrdemServico.OrdemServicoStatus.CANCELADA;
            default -> OrdemServico.OrdemServicoStatus.valueOf(value.trim().toUpperCase());
        };
    }
}

@Repository
@ConditionalOnMissingBean(SpringDataRepositorioOrdemServico.class)
class InMemoryAdaptadorRepositorioOrdemServico implements RepositorioOrdemServico {

    private final Map<String, OrdemServico> storage = new ConcurrentHashMap<>();

    @Override
    public OrdemServico save(OrdemServico ordemServico) {
        storage.put(ordemServico.id(), ordemServico);
        return ordemServico;
    }

    @Override
    public List<OrdemServico> findAll() {
        return storage.values().stream().toList();
    }

    @Override
    public Optional<OrdemServico> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }
}

interface SpringDataRepositorioOrdemServico extends JpaRepository<OrdemServicoEntidade, UUID> {
}
