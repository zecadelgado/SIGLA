package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.servicos.porta.saida.RepositorioServicoPrestado;
import br.com.sigla.dominio.servicos.ServicoPrestado;
import br.com.sigla.infraestrutura.persistencia.entidade.ServicoPrestadoEntidade;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnBean(SpringDataRepositorioServicoPrestado.class)
public class AdaptadorRepositorioServicoPrestado implements RepositorioServicoPrestado {

    private final SpringDataRepositorioServicoPrestado repository;

    public AdaptadorRepositorioServicoPrestado(SpringDataRepositorioServicoPrestado repository) {
        this.repository = repository;
    }

    @Override
    public void save(ServicoPrestado serviceProvided) {
        repository.save(toEntity(serviceProvided));
    }

    @Override
    public List<ServicoPrestado> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<ServicoPrestado> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    private ServicoPrestado toDomain(ServicoPrestadoEntidade entity) {
        return new ServicoPrestado(
                entity.getId(),
                entity.getClienteId(),
                entity.getContratoId(),
                entity.getScheduleId(),
                entity.getFuncionarioId(),
                entity.getExecutionDate(),
                entity.getDescription(),
                entity.getAmountCharged(),
                entity.getPaymentStatus(),
                entity.getSignatureType(),
                entity.getSignaturePath(),
                entity.getAttachments().stream()
                        .map(attachment -> new ServicoPrestado.Attachment(
                                attachment.getName(),
                                attachment.getStoragePath(),
                                attachment.getContentType()
                        ))
                        .toList(),
                entity.getServiceStatus(),
                entity.getPriority(),
                entity.getNotes()
        );
    }

    private ServicoPrestadoEntidade toEntity(ServicoPrestado serviceProvided) {
        ServicoPrestadoEntidade entity = new ServicoPrestadoEntidade();
        entity.setId(serviceProvided.id());
        entity.setClienteId(serviceProvided.customerId());
        entity.setContratoId(serviceProvided.contractId());
        entity.setScheduleId(serviceProvided.scheduleId());
        entity.setFuncionarioId(serviceProvided.employeeId());
        entity.setExecutionDate(serviceProvided.executionDate());
        entity.setDescription(serviceProvided.description());
        entity.setAmountCharged(serviceProvided.amountCharged());
        entity.setPaymentStatus(serviceProvided.paymentStatus());
        entity.setSignatureType(serviceProvided.signatureType());
        entity.setServiceStatus(serviceProvided.serviceStatus());
        entity.setPriority(serviceProvided.priority());
        entity.setSignaturePath(serviceProvided.signaturePath());
        entity.setNotes(serviceProvided.notes());
        List<ServicoPrestadoEntidade.AttachmentEmbeddable> attachments = new ArrayList<>();
        for (ServicoPrestado.Attachment attachment : serviceProvided.attachments()) {
            ServicoPrestadoEntidade.AttachmentEmbeddable embeddable = new ServicoPrestadoEntidade.AttachmentEmbeddable();
            embeddable.setName(attachment.name());
            embeddable.setStoragePath(attachment.storagePath());
            embeddable.setContentType(attachment.contentType());
            attachments.add(embeddable);
        }
        entity.setAttachments(attachments);
        return entity;
    }
}

@Repository
@ConditionalOnMissingBean(SpringDataRepositorioServicoPrestado.class)
class InMemoryAdaptadorRepositorioServicoPrestado implements RepositorioServicoPrestado {

    private final Map<String, ServicoPrestado> storage = new ConcurrentHashMap<>();

    @Override
    public void save(ServicoPrestado serviceProvided) {
        storage.put(serviceProvided.id(), serviceProvided);
    }

    @Override
    public List<ServicoPrestado> findAll() {
        return storage.values().stream().toList();
    }

    @Override
    public Optional<ServicoPrestado> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }
}

interface SpringDataRepositorioServicoPrestado extends JpaRepository<ServicoPrestadoEntidade, String> {
}

