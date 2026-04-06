package br.com.sigla.aplicacao.potenciaisclientes.porta.entrada;

import br.com.sigla.dominio.potenciaisclientes.PotencialCliente;

import java.time.LocalDate;
import java.util.List;

public interface CasoDeUsoPotencialCliente {

    void register(RegisterPotencialClienteCommand command);

    List<PotencialCliente> listAll();

    record RegisterPotencialClienteCommand(
            String id,
            String name,
            String contact,
            String origin,
            PotencialCliente.PotencialClienteStatus status,
            LocalDate interactionDate,
            String interactionChannel,
            String interactionNotes
    ) {
    }
}

