package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.auditoria.porta.saida.RepositorioAuditoriaFuncional;
import br.com.sigla.dominio.auditoria.EventoAuditoria;
import br.com.sigla.infraestrutura.persistencia.PersistenciaIds;
import br.com.sigla.infraestrutura.persistencia.entidade.AuditoriaEventoEntidade;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnBean(SpringDataRepositorioAuditoriaEvento.class)
public class AdaptadorRepositorioAuditoriaFuncional implements RepositorioAuditoriaFuncional {

    private final SpringDataRepositorioAuditoriaEvento repository;

    public AdaptadorRepositorioAuditoriaFuncional(SpringDataRepositorioAuditoriaEvento repository) {
        this.repository = repository;
    }

    @Override
    public void save(EventoAuditoria evento) {
        AuditoriaEventoEntidade entity = new AuditoriaEventoEntidade();
        entity.setId(PersistenciaIds.toUuid(evento.id()));
        entity.setEntidadeTipo(evento.entidadeTipo());
        entity.setEntidadeId(evento.entidadeId());
        entity.setAcao(evento.acao());
        entity.setDetalhe(evento.detalhe());
        entity.setUsuarioId(PersistenciaIds.toUuidIfValid(evento.usuarioId()));
        entity.setCreatedAt(evento.createdAt());
        repository.save(entity);
    }

    @Override
    public List<EventoAuditoria> findByEntidade(String entidadeTipo, String entidadeId) {
        return repository.findByEntidadeTipoAndEntidadeId(entidadeTipo, entidadeId).stream()
                .map(entity -> new EventoAuditoria(
                        PersistenciaIds.toString(entity.getId()),
                        entity.getEntidadeTipo(),
                        entity.getEntidadeId(),
                        entity.getAcao(),
                        entity.getDetalhe(),
                        PersistenciaIds.toString(entity.getUsuarioId()),
                        entity.getCreatedAt()))
                .toList();
    }
}

@Repository
@ConditionalOnMissingBean(SpringDataRepositorioAuditoriaEvento.class)
class InMemoryAdaptadorRepositorioAuditoriaFuncional implements RepositorioAuditoriaFuncional {

    private final Map<String, EventoAuditoria> storage = new ConcurrentHashMap<>();

    @Override
    public void save(EventoAuditoria evento) {
        storage.put(evento.id(), evento);
    }

    @Override
    public List<EventoAuditoria> findByEntidade(String entidadeTipo, String entidadeId) {
        return storage.values().stream()
                .filter(evento -> evento.entidadeTipo().equals(entidadeTipo))
                .filter(evento -> evento.entidadeId().equals(entidadeId))
                .toList();
    }
}

interface SpringDataRepositorioAuditoriaEvento extends JpaRepository<AuditoriaEventoEntidade, UUID> {
    List<AuditoriaEventoEntidade> findByEntidadeTipoAndEntidadeId(String entidadeTipo, String entidadeId);
}
