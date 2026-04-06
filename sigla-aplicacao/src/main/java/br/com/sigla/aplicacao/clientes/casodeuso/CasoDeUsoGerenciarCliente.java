package br.com.sigla.aplicacao.clientes.casodeuso;

import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.clientes.porta.saida.RepositorioCliente;
import br.com.sigla.dominio.clientes.Cliente;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CasoDeUsoGerenciarCliente implements CasoDeUsoCliente {

    private final RepositorioCliente repository;

    public CasoDeUsoGerenciarCliente(RepositorioCliente repository) {
        this.repository = repository;
    }

    @Override
    public void register(RegisterClienteCommand command) {
        List<Cliente.ContactPerson> contacts = command.contacts() == null ? List.of() : command.contacts().stream()
                .map(contact -> new Cliente.ContactPerson(contact.name(), contact.role(), contact.contact()))
                .toList();
        Cliente customer = new Cliente(
                command.id(),
                command.name(),
                command.location(),
                command.cnpj(),
                command.phone(),
                contacts,
                command.notes()
        );
        repository.save(customer);
    }

    @Override
    public List<Cliente> listAll() {
        return repository.findAll();
    }
}

