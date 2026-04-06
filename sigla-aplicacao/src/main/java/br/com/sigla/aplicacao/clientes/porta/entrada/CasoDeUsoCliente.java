package br.com.sigla.aplicacao.clientes.porta.entrada;

import br.com.sigla.dominio.clientes.Cliente;

import java.util.List;

public interface CasoDeUsoCliente {

    void register(RegisterClienteCommand command);

    List<Cliente> listAll();

    record RegisterClienteCommand(
            String id,
            String name,
            String location,
            String cnpj,
            String phone,
            List<ContactCommand> contacts,
            String notes
    ) {
    }

    record ContactCommand(
            String name,
            String role,
            String contact
    ) {
    }
}

