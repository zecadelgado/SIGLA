package br.com.sigla.aplicacao.potenciaisclientes.casodeuso;

import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.potenciaisclientes.porta.entrada.CasoDeUsoPotencialCliente;
import br.com.sigla.aplicacao.potenciaisclientes.porta.saida.RepositorioPotencialCliente;
import br.com.sigla.dominio.potenciaisclientes.PotencialCliente;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class CasoDeUsoGerenciarPotencialCliente implements CasoDeUsoPotencialCliente {

    private final RepositorioPotencialCliente repository;
    private final CasoDeUsoCliente casoDeUsoCliente;

    public CasoDeUsoGerenciarPotencialCliente(RepositorioPotencialCliente repository, CasoDeUsoCliente casoDeUsoCliente) {
        this.repository = repository;
        this.casoDeUsoCliente = casoDeUsoCliente;
    }

    @Override
    public void register(RegisterPotencialClienteCommand command) {
        PotencialCliente lead = toLead(command);
        validar(lead);
        repository.save(lead);
    }

    @Override
    public void update(RegisterPotencialClienteCommand command) {
        PotencialCliente lead = toLead(command);
        repository.findById(lead.id()).orElseThrow(() -> new IllegalArgumentException("Indicacao nao encontrada."));
        validar(lead);
        repository.save(lead);
    }

    @Override
    public void alterarStatus(AlterarStatusIndicacaoCommand command) {
        PotencialCliente atual = repository.findById(command.id())
                .orElseThrow(() -> new IllegalArgumentException("Indicacao nao encontrada."));
        PotencialCliente.PotencialClienteStatus status = PotencialCliente.PotencialClienteStatus.normalizar(command.status());
        String observacoes = anexarObservacao(atual.observacoes(), status.name(), command.motivoOuObservacao());
        repository.save(new PotencialCliente(
                atual.id(),
                atual.name(),
                atual.contact(),
                atual.origin(),
                status,
                List.of(new PotencialCliente.Interaction(atual.dataIndicacao(), "Indicacao", observacoes)),
                atual.clienteIndicadorId(),
                atual.dataIndicacao(),
                observacoes
        ));
    }

    @Override
    public String converterEmCliente(ConverterIndicacaoCommand command) {
        PotencialCliente atual = repository.findById(command.indicacaoId())
                .orElseThrow(() -> new IllegalArgumentException("Indicacao nao encontrada."));
        if (atual.status().isConvertido()) {
            throw new IllegalArgumentException("Indicacao ja convertida.");
        }
        String clienteId = command.clienteId() == null || command.clienteId().isBlank()
                ? command.cliente().id()
                : command.clienteId();
        if (repository.findConvertedByClienteId(clienteId).isPresent()) {
            throw new IllegalArgumentException("Ja existe conversao registrada para este cliente.");
        }
        casoDeUsoCliente.register(command.cliente());
        String observacoes = anexarObservacao(atual.observacoes(), "CONVERTIDO", "Cliente gerado: " + clienteId);
        repository.save(new PotencialCliente(
                atual.id(),
                atual.name(),
                atual.contact(),
                atual.origin(),
                PotencialCliente.PotencialClienteStatus.CONVERTIDO,
                List.of(new PotencialCliente.Interaction(atual.dataIndicacao(), "Indicacao", observacoes)),
                atual.clienteIndicadorId(),
                atual.dataIndicacao(),
                observacoes
        ));
        return clienteId;
    }

    @Override
    public List<PotencialCliente> listAll() {
        return repository.findAll();
    }

    @Override
    public List<PotencialCliente> filtrar(FiltroIndicacao filtro) {
        String texto = normalize(filtro == null ? "" : filtro.texto()).toLowerCase(Locale.ROOT);
        PotencialCliente.PotencialClienteStatus status = filtro == null ? null : filtro.status();
        String indicadorId = normalize(filtro == null ? "" : filtro.clienteIndicadorId());
        return repository.findAll().stream()
                .filter(lead -> status == null || PotencialCliente.PotencialClienteStatus.normalizar(lead.status()) == PotencialCliente.PotencialClienteStatus.normalizar(status))
                .filter(lead -> filtro == null || filtro.dataInicio() == null || !lead.dataIndicacao().isBefore(filtro.dataInicio()))
                .filter(lead -> filtro == null || filtro.dataFim() == null || !lead.dataIndicacao().isAfter(filtro.dataFim()))
                .filter(lead -> indicadorId.isBlank() || indicadorId.equals(lead.clienteIndicadorId()))
                .filter(lead -> texto.isBlank()
                        || lead.name().toLowerCase(Locale.ROOT).contains(texto)
                        || lead.contact().toLowerCase(Locale.ROOT).contains(texto)
                        || lead.observacoes().toLowerCase(Locale.ROOT).contains(texto))
                .toList();
    }

    private PotencialCliente toLead(RegisterPotencialClienteCommand command) {
        Objects.requireNonNull(command, "command is required");
        List<PotencialCliente.Interaction> interactions = command.interactionDate() == null ? List.of() : List.of(
                new PotencialCliente.Interaction(command.interactionDate(), command.interactionChannel(), command.interactionNotes())
        );
        String indicadorId = normalize(command.clienteIndicadorId()).isBlank() ? extractCustomerId(command.origin()) : normalize(command.clienteIndicadorId());
        return new PotencialCliente(
                command.id(),
                command.name(),
                command.contact(),
                indicadorId.isBlank() ? "INDICACAO" : "INDICACAO:" + indicadorId,
                PotencialCliente.PotencialClienteStatus.normalizar(command.status()),
                interactions,
                indicadorId,
                command.interactionDate(),
                command.interactionNotes()
        );
    }

    private void validar(PotencialCliente lead) {
        require(lead.name(), "Informe o nome indicado.");
        if (!lead.contact().isBlank()) {
            String digits = lead.contact().replaceAll("\\D", "");
            if (digits.length() < 10 || digits.length() > 13) {
                throw new IllegalArgumentException("Telefone da indicacao invalido.");
            }
        }
        if (lead.status() == null) {
            throw new IllegalArgumentException("Informe o status da indicacao.");
        }
        if (!lead.clienteIndicadorId().isBlank() && casoDeUsoCliente.listAll().stream().noneMatch(cliente -> cliente.id().equals(lead.clienteIndicadorId()))) {
            throw new IllegalArgumentException("Cliente indicador nao encontrado.");
        }
    }

    private String anexarObservacao(String atual, String status, String motivo) {
        String complemento = "[" + status + "] " + normalize(motivo);
        if (normalize(atual).isBlank()) {
            return complemento;
        }
        return atual + System.lineSeparator() + complemento;
    }

    private void require(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String extractCustomerId(String origin) {
        if (origin == null || !origin.contains(":")) {
            return "";
        }
        return origin.substring(origin.indexOf(':') + 1).trim();
    }
}
