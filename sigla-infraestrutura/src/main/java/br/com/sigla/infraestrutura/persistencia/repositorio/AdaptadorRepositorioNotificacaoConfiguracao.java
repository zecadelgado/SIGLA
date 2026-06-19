package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.notificacoes.porta.saida.RepositorioNotificacaoConfiguracao;
import br.com.sigla.dominio.notificacoes.Notificacao;
import br.com.sigla.dominio.notificacoes.NotificacaoConfiguracao;
import br.com.sigla.infraestrutura.persistencia.entidade.NotificacaoConfiguracaoEntidade;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class AdaptadorRepositorioNotificacaoConfiguracao implements RepositorioNotificacaoConfiguracao {

    private final SpringDataRepositorioNotificacaoConfiguracao repository;

    public AdaptadorRepositorioNotificacaoConfiguracao(SpringDataRepositorioNotificacaoConfiguracao repository) {
        this.repository = repository;
    }

    @Override
    public void save(NotificacaoConfiguracao configuracao) {
        repository.save(toEntity(configuracao));
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<NotificacaoConfiguracao> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<NotificacaoConfiguracao> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<NotificacaoConfiguracao> findAtivasPorEvento(Notificacao.NotificacaoType eventType) {
        return repository.findByEventTypeAndAtivoTrue(eventType).stream().map(this::toDomain).toList();
    }

    private NotificacaoConfiguracaoEntidade toEntity(NotificacaoConfiguracao configuracao) {
        NotificacaoConfiguracaoEntidade entity = new NotificacaoConfiguracaoEntidade();
        entity.setId(configuracao.id());
        entity.setEventType(configuracao.eventType());
        entity.setNome(configuracao.nome());
        entity.setTitulo(configuracao.titulo());
        entity.setTemplateMensagem(configuracao.templateMensagem());
        entity.setDestinatario(configuracao.destinatario());
        entity.setOrigemTipo(configuracao.origemTipo());
        entity.setCanal(configuracao.canal());
        entity.setFonteTelefone(configuracao.fonteTelefone());
        entity.setTelefoneInformado(emptyToNull(configuracao.telefoneInformado()));
        entity.setAutomatico(configuracao.automatico());
        entity.setDiasAntecedencia(configuracao.diasAntecedencia());
        entity.setAtivo(configuracao.ativo());
        entity.setCriadoPor(emptyToNull(configuracao.criadoPor()));
        entity.setCreatedAt(configuracao.criadoEm());
        entity.setUpdatedAt(configuracao.atualizadoEm());
        return entity;
    }

    private NotificacaoConfiguracao toDomain(NotificacaoConfiguracaoEntidade entity) {
        return new NotificacaoConfiguracao(
                entity.getId(),
                entity.getEventType(),
                entity.getNome(),
                entity.getTitulo(),
                entity.getTemplateMensagem(),
                entity.getDestinatario(),
                entity.getOrigemTipo(),
                entity.getCanal(),
                entity.getFonteTelefone(),
                entity.getTelefoneInformado(),
                entity.isAutomatico(),
                entity.getDiasAntecedencia(),
                entity.isAtivo(),
                entity.getCriadoPor(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}

@Repository
@Profile("memoria")
class InMemoryAdaptadorRepositorioNotificacaoConfiguracao implements RepositorioNotificacaoConfiguracao {

    private final Map<String, NotificacaoConfiguracao> storage = new ConcurrentHashMap<>();

    @Override
    public void save(NotificacaoConfiguracao configuracao) {
        storage.put(configuracao.id(), configuracao);
    }

    @Override
    public void deleteById(String id) {
        storage.remove(id);
    }

    @Override
    public Optional<NotificacaoConfiguracao> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<NotificacaoConfiguracao> findAll() {
        return storage.values().stream().toList();
    }

    @Override
    public List<NotificacaoConfiguracao> findAtivasPorEvento(Notificacao.NotificacaoType eventType) {
        return storage.values().stream()
                .filter(NotificacaoConfiguracao::ativo)
                .filter(configuracao -> configuracao.eventType() == eventType)
                .toList();
    }
}

interface SpringDataRepositorioNotificacaoConfiguracao extends JpaRepository<NotificacaoConfiguracaoEntidade, String> {

    List<NotificacaoConfiguracaoEntidade> findByEventTypeAndAtivoTrue(Notificacao.NotificacaoType eventType);
}
