package br.com.sigla.aplicacao.potenciaisclientes.casodeuso;

import br.com.sigla.aplicacao.potenciaisclientes.porta.entrada.CasoDeUsoPotencialCliente;
import br.com.sigla.aplicacao.potenciaisclientes.porta.saida.RepositorioPotencialCliente;
import br.com.sigla.dominio.potenciaisclientes.PotencialCliente;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CasoDeUsoGerenciarPotencialCliente implements CasoDeUsoPotencialCliente {

    private final RepositorioPotencialCliente repository;

    public CasoDeUsoGerenciarPotencialCliente(RepositorioPotencialCliente repository) {
        this.repository = repository;
    }

    @Override
    public void register(RegisterPotencialClienteCommand command) {
        List<PotencialCliente.Interaction> interactions = command.interactionDate() == null ? List.of() : List.of(
                new PotencialCliente.Interaction(command.interactionDate(), command.interactionChannel(), command.interactionNotes())
        );
        repository.save(new PotencialCliente(
                command.id(),
                command.name(),
                command.contact(),
                command.origin(),
                command.status(),
                interactions
        ));
    }

    @Override
    public List<PotencialCliente> listAll() {
        return repository.findAll();
    }
}

