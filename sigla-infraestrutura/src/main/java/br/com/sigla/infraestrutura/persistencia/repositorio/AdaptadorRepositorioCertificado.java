package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.certificados.porta.saida.RepositorioCertificado;
import br.com.sigla.dominio.certificados.Certificado;
import br.com.sigla.infraestrutura.persistencia.PersistenciaIds;
import br.com.sigla.infraestrutura.persistencia.entidade.CertificadoEntidade;
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
@ConditionalOnBean(SpringDataRepositorioCertificado.class)
public class AdaptadorRepositorioCertificado implements RepositorioCertificado {

    private final SpringDataRepositorioCertificado repository;

    public AdaptadorRepositorioCertificado(SpringDataRepositorioCertificado repository) {
        this.repository = repository;
    }

    @Override
    public void save(Certificado certificate) {
        repository.save(toEntity(certificate));
    }

    @Override
    public List<Certificado> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Certificado> findById(String id) {
        return repository.findById(PersistenciaIds.toUuid(id)).map(this::toDomain);
    }

    private CertificadoEntidade toEntity(Certificado certificate) {
        CertificadoEntidade entity = new CertificadoEntidade();
        entity.setId(PersistenciaIds.toUuid(certificate.id()));
        entity.setClienteId(PersistenciaIds.toUuid(certificate.serviceProvidedId()));
        entity.setDescricao("Certificado");
        entity.setIssuedOn(certificate.issuedOn());
        entity.setValidUntil(certificate.validUntil());
        entity.setStatus(certificate.status().name());
        entity.setRenewalAlertDays(certificate.renewalAlertDays());
        return entity;
    }

    private Certificado toDomain(CertificadoEntidade entity) {
        return new Certificado(
                PersistenciaIds.toString(entity.getId()),
                PersistenciaIds.toString(entity.getClienteId()),
                entity.getIssuedOn(),
                entity.getValidUntil(),
                parseStatus(entity.getStatus()),
                entity.getRenewalAlertDays()
        );
    }

    private Certificado.CertificadoStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return Certificado.CertificadoStatus.ACTIVE;
        }
        return switch (value.trim().toUpperCase()) {
            case "VALIDO", "VÁLIDO", "ACTIVE" -> Certificado.CertificadoStatus.ACTIVE;
            case "VENCIDO", "EXPIRED" -> Certificado.CertificadoStatus.EXPIRED;
            default -> Certificado.CertificadoStatus.REPLACED;
        };
    }
}

@Repository
@ConditionalOnMissingBean(SpringDataRepositorioCertificado.class)
class InMemoryAdaptadorRepositorioCertificado implements RepositorioCertificado {

    private final Map<String, Certificado> storage = new ConcurrentHashMap<>();

    @Override
    public void save(Certificado certificate) {
        storage.put(certificate.id(), certificate);
    }

    @Override
    public List<Certificado> findAll() {
        return storage.values().stream().toList();
    }

    @Override
    public Optional<Certificado> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }
}

interface SpringDataRepositorioCertificado extends JpaRepository<CertificadoEntidade, UUID> {
}

