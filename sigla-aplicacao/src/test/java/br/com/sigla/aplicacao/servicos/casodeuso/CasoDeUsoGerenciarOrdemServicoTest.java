package br.com.sigla.aplicacao.servicos.casodeuso;

import br.com.sigla.aplicacao.agenda.porta.saida.RepositorioAgenda;
import br.com.sigla.aplicacao.estoque.casodeuso.CasoDeUsoGerenciarEstoque;
import br.com.sigla.aplicacao.estoque.porta.entrada.CasoDeUsoEstoque;
import br.com.sigla.aplicacao.estoque.porta.saida.RepositorioEstoque;
import br.com.sigla.aplicacao.servicos.porta.entrada.CasoDeUsoOrdemServico;
import br.com.sigla.aplicacao.servicos.porta.saida.RepositorioOrdemServico;
import br.com.sigla.dominio.agenda.VisitaAgendada;
import br.com.sigla.dominio.estoque.ItemEstoque;
import br.com.sigla.dominio.servicos.OrdemServico;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CasoDeUsoGerenciarOrdemServicoTest {

    @Test
    void iniciaConcluiComProdutoBaixaEstoqueENaoDuplica() {
        FakeEstoque estoque = new FakeEstoque();
        CasoDeUsoGerenciarEstoque casoEstoque = new CasoDeUsoGerenciarEstoque(estoque);
        casoEstoque.registerItem(new CasoDeUsoEstoque.RegisterItemEstoqueCommand(
                "produto-1", "Produto", "Descricao", "SKU-1", BigDecimal.TEN, BigDecimal.valueOf(15), 5, 1, "un", true));

        CasoDeUsoGerenciarOrdemServico casoOs = new CasoDeUsoGerenciarOrdemServico(new FakeOs(), estoque, casoEstoque);
        casoOs.create(new CasoDeUsoOrdemServico.CreateOrdemServicoCommand(
                "os-1", "cliente-1", "", "Servico", "Descricao", "Limpeza", OrdemServico.OrdemServicoStatus.AGENDADA,
                LocalDateTime.now(), null, null, "", "", BigDecimal.valueOf(100), ""));
        casoOs.start("os-1");
        casoOs.adicionarProduto(new CasoDeUsoOrdemServico.AdicionarProdutoOrdemCommand("os-1", "uso-1", "produto-1", 2, BigDecimal.valueOf(15)));

        OrdemServico concluida = casoOs.conclude(new CasoDeUsoOrdemServico.ConcluirOrdemServicoCommand("os-1", "func-1", null, true));

        assertEquals(OrdemServico.OrdemServicoStatus.CONCLUIDA, concluida.status());
        assertTrue(concluida.foiFeito());
        assertTrue(concluida.assinaturaCliente());
        assertEquals(0, BigDecimal.valueOf(130).compareTo(concluida.totalGeral()));
        assertEquals(3, estoque.findById("produto-1").orElseThrow().quantity());
        assertEquals(1, estoque.findById("produto-1").orElseThrow().movements().size());
        assertThrows(IllegalArgumentException.class, () -> casoOs.conclude(new CasoDeUsoOrdemServico.ConcluirOrdemServicoCommand("os-1", "func-1", null, true)));
    }

    @Test
    void bloqueiaProdutoSemSaldo() {
        FakeEstoque estoque = new FakeEstoque();
        CasoDeUsoGerenciarEstoque casoEstoque = new CasoDeUsoGerenciarEstoque(estoque);
        casoEstoque.registerItem(new CasoDeUsoEstoque.RegisterItemEstoqueCommand(
                "produto-1", "Produto", "Descricao", "SKU-1", BigDecimal.TEN, BigDecimal.valueOf(15), 1, 1, "un", true));
        CasoDeUsoGerenciarOrdemServico casoOs = new CasoDeUsoGerenciarOrdemServico(new FakeOs(), estoque, casoEstoque);
        casoOs.create(new CasoDeUsoOrdemServico.CreateOrdemServicoCommand(
                "os-1", "cliente-1", "", "Servico", "Descricao", "Limpeza", OrdemServico.OrdemServicoStatus.AGENDADA,
                LocalDateTime.now(), null, null, "", "", BigDecimal.valueOf(100), ""));

        assertThrows(IllegalArgumentException.class, () -> casoOs.adicionarProduto(
                new CasoDeUsoOrdemServico.AdicionarProdutoOrdemCommand("os-1", "uso-1", "produto-1", 2, BigDecimal.valueOf(15))));
    }

    @Test
    void sincronizaOrdemDeServicoComAgendaSemDuplicarEvento() {
        FakeEstoque estoque = new FakeEstoque();
        CasoDeUsoGerenciarEstoque casoEstoque = new CasoDeUsoGerenciarEstoque(estoque);
        FakeAgenda agenda = new FakeAgenda();
        CasoDeUsoGerenciarOrdemServico casoOs = new CasoDeUsoGerenciarOrdemServico(new FakeOs(), estoque, casoEstoque, null, null, agenda);

        casoOs.create(new CasoDeUsoOrdemServico.CreateOrdemServicoCommand(
                "os-1", "cliente-1", "", "Servico", "Descricao", "Visita mensal", OrdemServico.OrdemServicoStatus.AGENDADA,
                LocalDateTime.of(2026, 4, 10, 8, 0), null, null, "resp-1", "", BigDecimal.valueOf(100), ""));
        casoOs.update(new CasoDeUsoOrdemServico.UpdateOrdemServicoCommand(
                "os-1", "cliente-1", "", "Servico editado", "Descricao", "Visita mensal",
                LocalDateTime.of(2026, 4, 11, 8, 0), "resp-1", BigDecimal.valueOf(100), ""));

        assertEquals(1, agenda.findAll().size());
        VisitaAgendada evento = agenda.findAll().getFirst();
        assertEquals("os-os-1", evento.id());
        assertEquals("os-1", evento.orderId());
        assertEquals(LocalDateTime.of(2026, 4, 11, 8, 0), evento.startAt());
        assertEquals(VisitaAgendada.Recurrence.MONTHLY, evento.recurrence());

        casoOs.cancel(new CasoDeUsoOrdemServico.CancelarOrdemServicoCommand("os-1", "Cliente cancelou."));
        assertEquals(VisitaAgendada.VisitStatus.CANCELLED, agenda.findById("os-os-1").orElseThrow().status());
    }

    static final class FakeOs implements RepositorioOrdemServico {
        private final Map<String, OrdemServico> storage = new HashMap<>();

        @Override
        public OrdemServico save(OrdemServico ordemServico) {
            storage.put(ordemServico.id(), ordemServico);
            return ordemServico;
        }

        @Override
        public List<OrdemServico> findAll() {
            return storage.values().stream().toList();
        }

        @Override
        public Optional<OrdemServico> findById(String id) {
            return Optional.ofNullable(storage.get(id));
        }
    }

    static final class FakeEstoque implements RepositorioEstoque {
        private final Map<String, ItemEstoque> storage = new HashMap<>();

        @Override
        public void save(ItemEstoque item) {
            storage.put(item.id(), item);
        }

        @Override
        public List<ItemEstoque> findAll() {
            return storage.values().stream().toList();
        }

        @Override
        public Optional<ItemEstoque> findById(String id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public boolean existsActiveSku(String sku, String exceptId) {
            return false;
        }

        @Override
        public boolean existsMovementForOrder(String orderId) {
            return storage.values().stream()
                    .flatMap(item -> item.movements().stream())
                    .anyMatch(movement -> movement.orderReference().equals(orderId));
        }
    }

    static final class FakeAgenda implements RepositorioAgenda {
        private final Map<String, VisitaAgendada> storage = new HashMap<>();

        @Override
        public void save(VisitaAgendada schedule) {
            storage.put(schedule.id(), schedule);
        }

        @Override
        public List<VisitaAgendada> findAll() {
            return storage.values().stream().toList();
        }

        @Override
        public Optional<VisitaAgendada> findById(String id) {
            return Optional.ofNullable(storage.get(id));
        }
    }
}
