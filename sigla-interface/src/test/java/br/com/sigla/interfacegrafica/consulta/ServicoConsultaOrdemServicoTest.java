package br.com.sigla.interfacegrafica.consulta;

import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.servicos.porta.entrada.CasoDeUsoOrdemServico;
import br.com.sigla.dominio.clientes.Cliente;
import br.com.sigla.dominio.servicos.OrdemServico;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServicoConsultaOrdemServicoTest {

    @Test
    void listByDateReturnsOnlyOrdersScheduledForTheRequestedDate() {
        FakeOrdemServico casoOrdemServico = new FakeOrdemServico(List.of(
                ordem("os-1", "cliente-1", "Servico A", LocalDateTime.of(2026, 5, 9, 8, 0)),
                ordem("os-2", "cliente-1", "Servico B", LocalDateTime.of(2026, 5, 10, 8, 0)),
                ordem("os-3", "cliente-2", "Servico C", null)
        ));
        FakeCliente casoCliente = new FakeCliente(List.of(
                new Cliente("cliente-1", "Cliente Um", "", "", "", "", "", "", "", "", "", "", "", "", "", List.of(), "", true),
                new Cliente("cliente-2", "Cliente Dois", "", "", "", "", "", "", "", "", "", "", "", "", "", List.of(), "", true)
        ));
        ServicoConsultaOrdemServico consulta = new ServicoConsultaOrdemServico(casoOrdemServico, casoCliente);

        var ordens = consulta.listByDate(LocalDate.of(2026, 5, 9));

        assertEquals(1, ordens.size());
        assertEquals("os-1", ordens.getFirst().id());
        assertEquals("Cliente Um", ordens.getFirst().customerName());
    }

    private OrdemServico ordem(String id, String clienteId, String titulo, LocalDateTime dataAgendada) {
        return new OrdemServico(
                id,
                null,
                clienteId,
                "",
                titulo,
                "Descricao",
                "Dedetizacao",
                OrdemServico.OrdemServicoStatus.AGENDADA,
                dataAgendada,
                null,
                null,
                "",
                "",
                false,
                false,
                BigDecimal.valueOf(100),
                false,
                List.of(),
                List.of(),
                ""
        );
    }

    private static final class FakeOrdemServico implements CasoDeUsoOrdemServico {
        private final List<OrdemServico> ordens;

        private FakeOrdemServico(List<OrdemServico> ordens) {
            this.ordens = ordens;
        }

        @Override
        public OrdemServico create(CreateOrdemServicoCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public OrdemServico update(UpdateOrdemServicoCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public OrdemServico start(String id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public OrdemServico conclude(ConcluirOrdemServicoCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public OrdemServico cancel(CancelarOrdemServicoCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public OrdemServico marcarPago(String id, boolean pago) {
            throw new UnsupportedOperationException();
        }

        @Override
        public OrdemServico adicionarProduto(AdicionarProdutoOrdemCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public OrdemServico anexar(AnexarOrdemServicoCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<OrdemServico> listAll() {
            return ordens;
        }

        @Override
        public List<OrdemServico> filtrar(FiltroOrdemServico filtro) {
            return ordens;
        }
    }

    private static final class FakeCliente implements CasoDeUsoCliente {
        private final List<Cliente> clientes;

        private FakeCliente(List<Cliente> clientes) {
            this.clientes = clientes;
        }

        @Override
        public void register(RegisterClienteCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void update(RegisterClienteCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void inativar(String id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void reativar(String id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void excluirFisicamente(String id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Cliente> listAll() {
            return clientes;
        }

        @Override
        public List<Cliente> filtrar(FiltroCliente filtro) {
            return clientes;
        }
    }
}
