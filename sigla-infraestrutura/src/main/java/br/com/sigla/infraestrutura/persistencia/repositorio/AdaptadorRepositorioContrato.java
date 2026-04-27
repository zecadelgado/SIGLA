package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.contratos.porta.saida.RepositorioContrato;
import br.com.sigla.dominio.contratos.Contrato;
import br.com.sigla.infraestrutura.persistencia.PersistenciaIds;
import br.com.sigla.infraestrutura.persistencia.entidade.ContratoEntidade;
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
@ConditionalOnBean(SpringDataRepositorioContrato.class)
public class AdaptadorRepositorioContrato implements RepositorioContrato {

    private final SpringDataRepositorioContrato repository;

    public AdaptadorRepositorioContrato(SpringDataRepositorioContrato repository) {
        this.repository = repository;
    }

    @Override
    public void save(Contrato contract) {
        repository.save(toEntity(contract));
    }

    @Override
    public List<Contrato> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Contrato> findById(String id) {
        return repository.findById(PersistenciaIds.toUuid(id)).map(this::toDomain);
    }

    private Contrato toDomain(ContratoEntidade entity) {
        return new Contrato(
                PersistenciaIds.toString(entity.getId()),
                PersistenciaIds.toString(entity.getClienteId()),
                entity.getDataInicio(),
                entity.getDataFim() == null ? entity.getDataInicio() : entity.getDataFim(),
                parseType(entity.getTipoContrato()),
                parseFrequency(entity.getTipoContrato()),
                parseStatus(entity.getStatus()),
                Contrato.RenewalRule.MANUAL,
                entity.getDiasAlertaFim()
        );
    }

    private ContratoEntidade toEntity(Contrato contract) {
        ContratoEntidade entity = new ContratoEntidade();
        entity.setId(PersistenciaIds.toUuid(contract.id()));
        entity.setClienteId(PersistenciaIds.toUuid(contract.customerId()));
        entity.setDescricao(contract.type().name());
        entity.setTipoContrato(contract.type().name());
        entity.setDataInicio(contract.startDate());
        entity.setDataFim(contract.endDate());
        entity.setValorMensal(java.math.BigDecimal.ZERO);
        entity.setAlertaAtivo(true);
        entity.setDiasAlertaFim(contract.alertDaysBeforeEnd());
        entity.setStatus(contract.status().name());
        entity.setObservacoes(contract.renewalRule().name());
        return entity;
    }

    private Contrato.ContratoType parseType(String value) {
        if (value == null || value.isBlank()) {
            return Contrato.ContratoType.MONTHLY;
        }
        return switch (value.trim().toUpperCase()) {
            case "MENSAL" -> Contrato.ContratoType.MONTHLY;
            case "QUINZENAL" -> Contrato.ContratoType.QUINZENAL;
            case "AVULSO" -> Contrato.ContratoType.AVULSO;
            default -> Contrato.ContratoType.CORPORATE;
        };
    }

    private Contrato.ServiceFrequency parseFrequency(String value) {
        if (value == null || value.isBlank()) {
            return Contrato.ServiceFrequency.MONTHLY;
        }
        return switch (value.trim().toUpperCase()) {
            case "QUINZENAL" -> Contrato.ServiceFrequency.BIWEEKLY;
            case "AVULSO" -> Contrato.ServiceFrequency.ONE_OFF;
            default -> Contrato.ServiceFrequency.MONTHLY;
        };
    }

    private Contrato.ContratoStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return Contrato.ContratoStatus.ACTIVE;
        }
        return switch (value.trim().toUpperCase()) {
            case "ATIVO", "ACTIVE" -> Contrato.ContratoStatus.ACTIVE;
            case "CANCELADO", "CANCELLED" -> Contrato.ContratoStatus.CANCELLED;
            case "EXPIRADO", "EXPIRED" -> Contrato.ContratoStatus.EXPIRED;
            default -> Contrato.ContratoStatus.DRAFT;
        };
    }
}

@Repository
@ConditionalOnMissingBean(SpringDataRepositorioContrato.class)
class InMemoryAdaptadorRepositorioContrato implements RepositorioContrato {

    private final Map<String, Contrato> storage = new ConcurrentHashMap<>();

    @Override
    public void save(Contrato contract) {
        storage.put(contract.id(), contract);
    }

    @Override
    public List<Contrato> findAll() {
        return storage.values().stream().toList();
    }

    @Override
    public Optional<Contrato> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }
}

interface SpringDataRepositorioContrato extends JpaRepository<ContratoEntidade, UUID> {
}

