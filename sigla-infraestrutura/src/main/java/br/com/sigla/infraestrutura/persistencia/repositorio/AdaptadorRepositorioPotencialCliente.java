package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.potenciaisclientes.porta.saida.RepositorioPotencialCliente;
import br.com.sigla.dominio.potenciaisclientes.PotencialCliente;
import br.com.sigla.infraestrutura.persistencia.entidade.PotencialClienteEntidade;
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
        return repository.findById(id).map(this::toDomain);
    }

    private PotencialCliente toDomain(PotencialClienteEntidade entity) {
        return new PotencialCliente(
                entity.getId(),
                entity.getName(),
                entity.getContact(),
                entity.getOrigin(),
                entity.getStatus(),
                entity.getInteractions().stream()
                        .map(interaction -> new PotencialCliente.Interaction(
                                interaction.getInteractionDate(),
                                interaction.getChannel(),
                                interaction.getNotes()
                        ))
                        .toList()
        );
    }

    private PotencialClienteEntidade toEntity(PotencialCliente lead) {
        PotencialClienteEntidade entity = new PotencialClienteEntidade();
        entity.setId(lead.id());
        entity.setName(lead.name());
        entity.setContact(lead.contact());
        entity.setOrigin(lead.origin());
        entity.setStatus(lead.status());
        List<PotencialClienteEntidade.InteractionEmbeddable> interactions = new ArrayList<>();
        for (PotencialCliente.Interaction interaction : lead.interactionHistory()) {
            PotencialClienteEntidade.InteractionEmbeddable embeddable = new PotencialClienteEntidade.InteractionEmbeddable();
            embeddable.setInteractionDate(interaction.interactionDate());
            embeddable.setChannel(interaction.channel());
            embeddable.setNotes(interaction.notes());
            interactions.add(embeddable);
        }
        entity.setInteractions(interactions);
        return entity;
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
}

interface SpringDataRepositorioPotencialCliente extends JpaRepository<PotencialClienteEntidade, String> {
}

