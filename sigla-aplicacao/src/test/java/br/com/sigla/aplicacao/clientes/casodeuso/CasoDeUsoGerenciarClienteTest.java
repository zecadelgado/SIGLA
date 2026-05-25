package br.com.sigla.aplicacao.clientes.casodeuso;

import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.clientes.porta.saida.RepositorioCliente;
import br.com.sigla.dominio.clientes.Cliente;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CasoDeUsoGerenciarClienteTest {

    @Test
    void cadastraPessoaFisicaEImpedeCpfDuplicado() {
        FakeRepositorioCliente repositorio = new FakeRepositorioCliente();
        CasoDeUsoGerenciarCliente casoDeUso = new CasoDeUsoGerenciarCliente(repositorio);

        casoDeUso.register(pessoaFisica("1", "529.982.247-25", "ana@sigla.local"));

        assertEquals(1, casoDeUso.listAll().size());
        assertThrows(IllegalArgumentException.class, () -> casoDeUso.register(pessoaFisica("2", "52998224725", "outra@sigla.local")));
    }

    @Test
    void cadastraPessoaJuridicaEPermiteEditarMantendoDocumentos() {
        FakeRepositorioCliente repositorio = new FakeRepositorioCliente();
        CasoDeUsoGerenciarCliente casoDeUso = new CasoDeUsoGerenciarCliente(repositorio);

        CasoDeUsoCliente.RegisterClienteCommand empresa = pessoaJuridica("1", "11.222.333/0001-81", "empresa@sigla.local");
        casoDeUso.register(empresa);
        casoDeUso.update(empresa);

        assertEquals(Cliente.TipoCliente.PESSOA_JURIDICA, casoDeUso.listAll().getFirst().tipo());
    }

    @Test
    void inativaReativaEbloqueiaExclusaoComVinculo() {
        FakeRepositorioCliente repositorio = new FakeRepositorioCliente();
        CasoDeUsoGerenciarCliente casoDeUso = new CasoDeUsoGerenciarCliente(repositorio);
        casoDeUso.register(pessoaFisica("1", "52998224725", "ana@sigla.local"));

        casoDeUso.inativar("1");
        assertFalse(casoDeUso.listAll().getFirst().ativo());

        casoDeUso.reativar("1");
        assertTrue(casoDeUso.listAll().getFirst().ativo());

        repositorio.hasLinkedRecords = true;
        assertThrows(IllegalArgumentException.class, () -> casoDeUso.excluirFisicamente("1"));
    }

    @Test
    void mantemApenasUmResponsavelPrincipal() {
        FakeRepositorioCliente repositorio = new FakeRepositorioCliente();
        CasoDeUsoGerenciarCliente casoDeUso = new CasoDeUsoGerenciarCliente(repositorio);

        casoDeUso.register(new CasoDeUsoCliente.RegisterClienteCommand(
                "1",
                Cliente.TipoCliente.PESSOA_FISICA,
                "Ana Silva",
                "",
                "",
                "52998224725",
                "",
                "11999999999",
                "ana@sigla.local",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                List.of(
                        new CasoDeUsoCliente.ContactCommand("", "Maria", "Compras", "11988888888", "maria@sigla.local", true),
                        new CasoDeUsoCliente.ContactCommand("", "Joao", "Financeiro", "11977777777", "joao@sigla.local", true)
                ),
                "",
                true
        ));

        long principais = casoDeUso.listAll().getFirst().contacts().stream().filter(Cliente.ContactPerson::principal).count();
        assertEquals(1, principais);
    }

    private CasoDeUsoCliente.RegisterClienteCommand pessoaFisica(String id, String cpf, String email) {
        return new CasoDeUsoCliente.RegisterClienteCommand(
                id,
                Cliente.TipoCliente.PESSOA_FISICA,
                "Ana Silva",
                "",
                "",
                cpf,
                "",
                "11999999999",
                email,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                List.of(),
                "",
                true
        );
    }

    private CasoDeUsoCliente.RegisterClienteCommand pessoaJuridica(String id, String cnpj, String email) {
        return new CasoDeUsoCliente.RegisterClienteCommand(
                id,
                Cliente.TipoCliente.PESSOA_JURIDICA,
                "Empresa Teste",
                "Empresa Teste Ltda",
                "Empresa Teste",
                "",
                cnpj,
                "11999999999",
                email,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                List.of(),
                "",
                true
        );
    }

    static class FakeRepositorioCliente implements RepositorioCliente {
        private final Map<String, Cliente> storage = new ConcurrentHashMap<>();
        private boolean hasLinkedRecords;

        @Override
        public void save(Cliente customer) {
            storage.put(customer.id(), customer);
        }

        @Override
        public void deleteById(String id) {
            storage.remove(id);
        }

        @Override
        public List<Cliente> findAll() {
            return storage.values().stream().toList();
        }

        @Override
        public Optional<Cliente> findById(String id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public boolean existsActiveCpf(String cpf, String exceptId) {
            return storage.values().stream()
                    .filter(Cliente::ativo)
                    .filter(cliente -> !cliente.id().equals(exceptId))
                    .anyMatch(cliente -> cliente.cpf().replaceAll("\\D", "").equals(cpf));
        }

        @Override
        public boolean existsActiveCnpj(String cnpj, String exceptId) {
            return storage.values().stream()
                    .filter(Cliente::ativo)
                    .filter(cliente -> !cliente.id().equals(exceptId))
                    .anyMatch(cliente -> cliente.cnpj().replaceAll("\\D", "").equals(cnpj));
        }

        @Override
        public boolean existsActiveEmail(String email, String exceptId) {
            return storage.values().stream()
                    .filter(Cliente::ativo)
                    .filter(cliente -> !cliente.id().equals(exceptId))
                    .anyMatch(cliente -> cliente.email().equalsIgnoreCase(email));
        }

        @Override
        public boolean hasLinkedRecords(String id) {
            return hasLinkedRecords;
        }
    }
}
