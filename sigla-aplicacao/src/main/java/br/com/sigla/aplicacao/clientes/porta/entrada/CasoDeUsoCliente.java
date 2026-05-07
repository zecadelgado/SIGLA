package br.com.sigla.aplicacao.clientes.porta.entrada;

import br.com.sigla.dominio.clientes.Cliente;

import java.util.List;

public interface CasoDeUsoCliente {

    void register(RegisterClienteCommand command);

    void update(RegisterClienteCommand command);

    void inativar(String id);

    void reativar(String id);

    void excluirFisicamente(String id);

    List<Cliente> listAll();

    List<Cliente> filtrar(FiltroCliente filtro);

    record RegisterClienteCommand(
            String id,
            Cliente.TipoCliente tipo,
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
            this(
                    id,
                    cnpj == null || cnpj.isBlank() ? Cliente.TipoCliente.PESSOA_FISICA : Cliente.TipoCliente.PESSOA_JURIDICA,
                    name,
                    razaoSocial,
                    nomeFantasia,
                    cpf,
                    cnpj,
                    phone,
                    email,
                    cep,
                    rua,
                    numero,
                    complemento,
                    bairro,
                    cidade,
                    estado,
                    contacts,
                    notes,
                    ativo
            );
        }

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
                    cnpj == null || cnpj.isBlank() ? Cliente.TipoCliente.PESSOA_FISICA : Cliente.TipoCliente.PESSOA_JURIDICA,
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
            String id,
            String name,
            String role,
            String phone,
            String email,
            boolean principal
    ) {
        public ContactCommand(String name, String role, String contact) {
            this("", name, role, contact == null || contact.contains("@") ? "" : contact, contact != null && contact.contains("@") ? contact : "", false);
        }
    }

    record FiltroCliente(
            String texto,
            Boolean ativo,
            Cliente.TipoCliente tipo
    ) {
    }
}
