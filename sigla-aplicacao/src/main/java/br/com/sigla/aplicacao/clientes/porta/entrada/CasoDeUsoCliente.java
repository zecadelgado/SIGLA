package br.com.sigla.aplicacao.clientes.porta.entrada;

import br.com.sigla.dominio.clientes.Cliente;

import java.util.List;

public interface CasoDeUsoCliente {

    void register(RegisterClienteCommand command);

    List<Cliente> listAll();

    record RegisterClienteCommand(
            String id,
            String name,
            String razaoSocial,
            String nomeFantasia,
            String cpf,
            String cnpj,
            String phone,
            String email,
            String cep,
            String rua,
            String numero,
            String complemento,
            String bairro,
            String cidade,
            String estado,
            List<ContactCommand> contacts,
            String notes,
            boolean ativo
    ) {
        public RegisterClienteCommand(
                String id,
                String name,
                String location,
                String cnpj,
                String phone,
                List<ContactCommand> contacts,
                String notes
        ) {
            this(
                    id,
                    name,
                    name,
                    name,
                    "",
                    cnpj,
                    phone,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    contacts,
                    notes,
                    true
            );
        }
    }

    record ContactCommand(
            String name,
            String role,
            String contact
    ) {
    }
}
