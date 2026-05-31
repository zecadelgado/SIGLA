package br.com.sigla.aplicacao.potenciaisclientes.casodeuso;

import br.com.sigla.aplicacao.clientes.casodeuso.CasoDeUsoGerenciarCliente;
import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.clientes.porta.saida.RepositorioCliente;
import br.com.sigla.aplicacao.potenciaisclientes.porta.entrada.CasoDeUsoPotencialCliente;
import br.com.sigla.aplicacao.potenciaisclientes.porta.saida.RepositorioPotencialCliente;
import br.com.sigla.dominio.clientes.Cliente;
import br.com.sigla.dominio.potenciaisclientes.PotencialCliente;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CasoDeUsoGerenciarPotencialClienteTest {

    @Test
    void criaFiltraAlteraStatusEConverteIndicacao() {
        CasoDeUsoGerenciarCliente casoCliente = new CasoDeUsoGerenciarCliente(new FakeRepositorioCliente());
        casoCliente.register(cliente("cliente-indicador", "52998224725", "indicador@sigla.local"));

        FakeRepositorioPotencial repositorioIndicacao = new FakeRepositorioPotencial();
        CasoDeUsoGerenciarPotencialCliente casoIndicacao = new CasoDeUsoGerenciarPotencialCliente(repositorioIndicacao, casoCliente);

        casoIndicacao.register(new CasoDeUsoPotencialCliente.RegisterPotencialClienteCommand(
                "indicacao-1",
                "Cliente Indicado",
                "11999999999",
                "INDICACAO:cliente-indicador",
                "cliente-indicador",
                PotencialCliente.PotencialClienteStatus.NOVO,
                LocalDate.of(2026, 5, 5),
                "Indicacao",
                "Contato recebido por indicacao."
        ));

        assertEquals(1, casoIndicacao.filtrar(new CasoDeUsoPotencialCliente.FiltroIndicacao("", PotencialCliente.PotencialClienteStatus.NOVO, null, null, "cliente-indicador")).size());

        casoIndicacao.alterarStatus(new CasoDeUsoPotencialCliente.AlterarStatusIndicacaoCommand(
                "indicacao-1",
                PotencialCliente.PotencialClienteStatus.PERDIDO,
                "Sem interesse no momento."
        ));
        assertTrue(casoIndicacao.listAll().getFirst().observacoes().contains("Sem interesse"));

        String clienteConvertido = casoIndicacao.converterEmCliente(new CasoDeUsoPotencialCliente.ConverterIndicacaoCommand(
                "indicacao-1",
                "cliente-convertido",
                cliente("cliente-convertido", "15350946056", "convertido@sigla.local")
        ));

        assertEquals("cliente-convertido", clienteConvertido);
        assertEquals(PotencialCliente.PotencialClienteStatus.CONVERTIDO, casoIndicacao.listAll().getFirst().status());
        assertThrows(IllegalArgumentException.class, () -> casoIndicacao.converterEmCliente(new CasoDeUsoPotencialCliente.ConverterIndicacaoCommand(
                "indicacao-1",
                "cliente-convertido",
                cliente("cliente-convertido", "15350946056", "convertido@sigla.local")
        )));
    }

    private CasoDeUsoCliente.RegisterClienteCommand cliente(String id, String cpf, String email) {
        return new CasoDeUsoCliente.RegisterClienteCommand(
                id,
                Cliente.TipoCliente.PESSOA_FISICA,
                "Cliente " + id,
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

    static class FakeRepositorioCliente implements RepositorioCliente {
        private final Map<String, Cliente> storage = new ConcurrentHashMap<>();

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
            return false;
        }
    }

    static class FakeRepositorioPotencial implements RepositorioPotencialCliente {
        private final Map<String, PotencialCliente> storage = new ConcurrentHashMap<>();

        @Override
        public void save(PotencialCliente lead) {
            storage.put(lead.id(), lead);
        }

        @Override
        public List<PotencialCliente> findAll() {
            return storage.values().stream().toList();
        }

        @Override
        public Optional<PotencialCliente> findById(String id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public Optional<PotencialCliente> findConvertedByClienteId(String clienteId) {
            return storage.values().stream()
                    .filter(lead -> lead.status().isConvertido())
                    .filter(lead -> lead.observacoes().contains("Cliente gerado: " + clienteId))
                    .findFirst();
        }
    }
}
