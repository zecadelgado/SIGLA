package br.com.sigla.aplicacao.potenciaisclientes.porta.entrada;

import br.com.sigla.dominio.potenciaisclientes.PotencialCliente;

import java.time.LocalDate;
import java.util.List;

public interface CasoDeUsoPotencialCliente {

    void register(RegisterPotencialClienteCommand command);

    void update(RegisterPotencialClienteCommand command);

    void alterarStatus(AlterarStatusIndicacaoCommand command);

    String converterEmCliente(ConverterIndicacaoCommand command);

    List<PotencialCliente> listAll();

    List<PotencialCliente> filtrar(FiltroIndicacao filtro);

    record RegisterPotencialClienteCommand(
            String id,
            String name,
            String contact,
            String origin,
            String clienteIndicadorId,
            PotencialCliente.PotencialClienteStatus status,
            LocalDate interactionDate,
            String interactionChannel,
            String interactionNotes
    ) {
        public RegisterPotencialClienteCommand(
                String id,
                String name,
                String contact,
                String origin,
                PotencialCliente.PotencialClienteStatus status,
                LocalDate interactionDate,
                String interactionChannel,
                String interactionNotes
        ) {
            this(id, name, contact, origin, extractCustomerId(origin), status, interactionDate, interactionChannel, interactionNotes);
        }
    }

    record AlterarStatusIndicacaoCommand(
            String id,
            PotencialCliente.PotencialClienteStatus status,
            String motivoOuObservacao
    ) {
    }

    record ConverterIndicacaoCommand(
            String indicacaoId,
            String clienteId,
            br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente.RegisterClienteCommand cliente
    ) {
    }

    record FiltroIndicacao(
            String texto,
            PotencialCliente.PotencialClienteStatus status,
            LocalDate dataInicio,
            LocalDate dataFim,
            String clienteIndicadorId
    ) {
    }

    private static String extractCustomerId(String origin) {
        if (origin == null || !origin.contains(":")) {
            return "";
        }
        return origin.substring(origin.indexOf(':') + 1).trim();
    }
}

