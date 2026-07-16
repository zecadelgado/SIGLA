package br.com.sigla.aplicacao.servicos.casodeuso;

import br.com.sigla.aplicacao.agenda.porta.saida.RepositorioAgenda;
import br.com.sigla.aplicacao.contratos.porta.saida.RepositorioContrato;
import br.com.sigla.aplicacao.estoque.casodeuso.CasoDeUsoGerenciarEstoque;
import br.com.sigla.aplicacao.estoque.porta.entrada.CasoDeUsoEstoque;
import br.com.sigla.aplicacao.estoque.porta.saida.RepositorioEstoque;
import br.com.sigla.aplicacao.financeiro.porta.entrada.CasoDeUsoFinanceiro;
import br.com.sigla.aplicacao.servicos.porta.entrada.CasoDeUsoOrdemServico;
import br.com.sigla.aplicacao.servicos.porta.saida.RepositorioOrdemServico;
import br.com.sigla.dominio.agenda.VisitaAgendada;
import br.com.sigla.dominio.contratos.Contrato;
import br.com.sigla.dominio.estoque.ItemEstoque;
import br.com.sigla.dominio.financeiro.LancamentoFinanceiro;
import br.com.sigla.dominio.servicos.OrdemServico;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CasoDeUsoGerenciarOrdemServicoTest {

    @Test
    void operacoesQuePersistemOsEProjetamAgendaPossuemFronteiraTransacional() throws NoSuchMethodException {
        assertTrue(transacional("create", CasoDeUsoOrdemServico.CreateOrdemServicoCommand.class));
        assertTrue(transacional("update", CasoDeUsoOrdemServico.UpdateOrdemServicoCommand.class));
        assertTrue(transacional("reschedule", CasoDeUsoOrdemServico.ReagendarOrdemServicoCommand.class));
    }

    private static boolean transacional(String nome, Class<?> tipoDoComando) throws NoSuchMethodException {
        Method metodo = CasoDeUsoGerenciarOrdemServico.class.getMethod(nome, tipoDoComando);
        return metodo.isAnnotationPresent(Transactional.class);
    }

    @Test
    void iniciaConcluiComProdutoBaixaEstoqueENaoDuplica() {
        FakeEstoque estoque = new FakeEstoque();
        CasoDeUsoGerenciarEstoque casoEstoque = new CasoDeUsoGerenciarEstoque(estoque);
        casoEstoque.registerItem(new CasoDeUsoEstoque.RegisterItemEstoqueCommand(
                "produto-1", "Produto", "Descricao", "SKU-1", BigDecimal.TEN, BigDecimal.valueOf(15), BigDecimal.valueOf(5), BigDecimal.valueOf(1), "un", true));

        CasoDeUsoGerenciarOrdemServico casoOs = new CasoDeUsoGerenciarOrdemServico(new FakeOs(), estoque, casoEstoque);
        casoOs.create(new CasoDeUsoOrdemServico.CreateOrdemServicoCommand(
                "os-1", "cliente-1", "", "Servico", "Descricao", "Limpeza", OrdemServico.OrdemServicoStatus.AGENDADA,
                LocalDateTime.now(), null, null, "", "", BigDecimal.valueOf(100), ""));
        casoOs.adicionarProduto(new CasoDeUsoOrdemServico.AdicionarProdutoOrdemCommand("os-1", "uso-1", "produto-1", BigDecimal.valueOf(2), BigDecimal.valueOf(15)));
        casoOs.start("os-1");

        OrdemServico concluida = casoOs.conclude(new CasoDeUsoOrdemServico.ConcluirOrdemServicoCommand("os-1", "func-1", null, true));

        assertEquals(OrdemServico.OrdemServicoStatus.CONCLUIDA, concluida.status());
        assertTrue(concluida.foiFeito());
        assertTrue(concluida.assinaturaCliente());
        assertEquals(0, BigDecimal.valueOf(130).compareTo(concluida.totalGeral()));
        assertEquals(0, BigDecimal.valueOf(3).compareTo(estoque.findById("produto-1").orElseThrow().quantity()));
        assertEquals(2, estoque.findById("produto-1").orElseThrow().movements().size());
        assertEquals(OrdemServico.OrdemServicoStatus.CONCLUIDA,
                casoOs.conclude(new CasoDeUsoOrdemServico.ConcluirOrdemServicoCommand("os-1", "func-1", null, true)).status());
        assertEquals(2, estoque.findById("produto-1").orElseThrow().movements().size());
    }

    @Test
    void bloqueiaProdutoSemSaldo() {
        FakeEstoque estoque = new FakeEstoque();
        CasoDeUsoGerenciarEstoque casoEstoque = new CasoDeUsoGerenciarEstoque(estoque);
        casoEstoque.registerItem(new CasoDeUsoEstoque.RegisterItemEstoqueCommand(
                "produto-1", "Produto", "Descricao", "SKU-1", BigDecimal.TEN, BigDecimal.valueOf(15), BigDecimal.valueOf(1), BigDecimal.valueOf(1), "un", true));
        CasoDeUsoGerenciarOrdemServico casoOs = new CasoDeUsoGerenciarOrdemServico(new FakeOs(), estoque, casoEstoque);
        casoOs.create(new CasoDeUsoOrdemServico.CreateOrdemServicoCommand(
                "os-1", "cliente-1", "", "Servico", "Descricao", "Limpeza", OrdemServico.OrdemServicoStatus.AGENDADA,
                LocalDateTime.now(), null, null, "", "", BigDecimal.valueOf(100), ""));

        assertThrows(IllegalArgumentException.class, () -> casoOs.adicionarProduto(
                new CasoDeUsoOrdemServico.AdicionarProdutoOrdemCommand("os-1", "uso-1", "produto-1", BigDecimal.valueOf(2), BigDecimal.valueOf(15))));
    }

    @Test
    void bloqueiaMesmoProdutoRepetidoQuandoTotalUltrapassaSaldo() {
        FakeEstoque estoque = new FakeEstoque();
        CasoDeUsoGerenciarEstoque casoEstoque = new CasoDeUsoGerenciarEstoque(estoque);
        casoEstoque.registerItem(new CasoDeUsoEstoque.RegisterItemEstoqueCommand(
                "produto-1", "Produto", "Descricao", "SKU-1", BigDecimal.TEN, BigDecimal.valueOf(15), BigDecimal.valueOf(10), BigDecimal.valueOf(1), "un", true));
        CasoDeUsoGerenciarOrdemServico casoOs = new CasoDeUsoGerenciarOrdemServico(new FakeOs(), estoque, casoEstoque);
        casoOs.create(new CasoDeUsoOrdemServico.CreateOrdemServicoCommand(
                "os-1", "cliente-1", "", "Servico", "Descricao", "Limpeza", OrdemServico.OrdemServicoStatus.AGENDADA,
                LocalDateTime.now(), null, null, "", "", BigDecimal.ZERO, ""));

        casoOs.adicionarProduto(new CasoDeUsoOrdemServico.AdicionarProdutoOrdemCommand("os-1", "uso-1", "produto-1", BigDecimal.valueOf(6), BigDecimal.valueOf(15)));
        assertThrows(IllegalArgumentException.class, () -> casoOs.adicionarProduto(
                new CasoDeUsoOrdemServico.AdicionarProdutoOrdemCommand("os-1", "uso-2", "produto-1", BigDecimal.valueOf(6), BigDecimal.valueOf(15))));
        assertEquals(0, BigDecimal.valueOf(6).compareTo(casoOs.listAll().getFirst().produtos().getFirst().quantidade()));
    }

    @Test
    void permiteReservaFracionadaAteOLimiteEConsomeSemNovaBaixa() {
        FakeEstoque estoque = new FakeEstoque();
        CasoDeUsoGerenciarEstoque casoEstoque = new CasoDeUsoGerenciarEstoque(estoque);
        casoEstoque.registerItem(new CasoDeUsoEstoque.RegisterItemEstoqueCommand(
                "produto-1", "Produto", "Descricao", "SKU-1", BigDecimal.TEN, BigDecimal.valueOf(15), BigDecimal.valueOf(10), BigDecimal.valueOf(1), "un", true));
        CasoDeUsoGerenciarOrdemServico casoOs = new CasoDeUsoGerenciarOrdemServico(new FakeOs(), estoque, casoEstoque);
        casoOs.create(new CasoDeUsoOrdemServico.CreateOrdemServicoCommand(
                "os-1", "cliente-1", "", "Servico", "Descricao", "Limpeza", OrdemServico.OrdemServicoStatus.AGENDADA,
                LocalDateTime.now(), null, null, "", "", BigDecimal.ZERO, ""));

        casoOs.adicionarProduto(new CasoDeUsoOrdemServico.AdicionarProdutoOrdemCommand("os-1", "uso-1", "produto-1", BigDecimal.valueOf(6), BigDecimal.valueOf(15)));
        casoOs.adicionarProduto(new CasoDeUsoOrdemServico.AdicionarProdutoOrdemCommand("os-1", "uso-2", "produto-1", BigDecimal.valueOf(4), BigDecimal.valueOf(15)));
        assertEquals(0, BigDecimal.valueOf(10).compareTo(estoque.findById("produto-1").orElseThrow().quantity()));
        casoOs.start("os-1");
        assertEquals(0, BigDecimal.valueOf(0).compareTo(estoque.findById("produto-1").orElseThrow().quantity()));

        casoOs.conclude("os-1");
        ItemEstoque item = estoque.findById("produto-1").orElseThrow();
        assertEquals(0, BigDecimal.valueOf(0).compareTo(item.quantity()));
        assertEquals(2, item.movements().size());
        assertEquals(1, item.movements().stream().filter(movimento -> movimento.type() == ItemEstoque.MovementType.CONSUMO_RESERVA_OS).count());
        assertEquals(0, BigDecimal.valueOf(10).compareTo(item.movements().stream().filter(movimento -> movimento.type() == ItemEstoque.MovementType.CONSUMO_RESERVA_OS).findFirst().orElseThrow().amount()));
    }

    @Test
    void cancelarOsNaoExecutadaDevolveMateriaisReservados() {
        FakeEstoque estoque = new FakeEstoque();
        CasoDeUsoGerenciarEstoque casoEstoque = new CasoDeUsoGerenciarEstoque(estoque);
        casoEstoque.registerItem(new CasoDeUsoEstoque.RegisterItemEstoqueCommand(
                "produto-1", "Produto", "Descricao", "SKU-1", BigDecimal.TEN, BigDecimal.valueOf(15), BigDecimal.valueOf(5), BigDecimal.valueOf(1), "un", true));
        CasoDeUsoGerenciarOrdemServico casoOs = new CasoDeUsoGerenciarOrdemServico(new FakeOs(), estoque, casoEstoque);
        casoOs.create(new CasoDeUsoOrdemServico.CreateOrdemServicoCommand(
                "os-1", "cliente-1", "", "Servico", "Descricao", "Limpeza", OrdemServico.OrdemServicoStatus.AGENDADA,
                LocalDateTime.now(), null, null, "", "", BigDecimal.ZERO, ""));
        casoOs.adicionarProduto(new CasoDeUsoOrdemServico.AdicionarProdutoOrdemCommand("os-1", "uso-1", "produto-1", BigDecimal.valueOf(3), BigDecimal.valueOf(15)));
        casoOs.start("os-1");

        casoOs.cancel("os-1");

        ItemEstoque item = estoque.findById("produto-1").orElseThrow();
        assertEquals(0, BigDecimal.valueOf(5).compareTo(item.quantity()));
        assertEquals(ItemEstoque.MovementType.DEVOLUCAO_RESERVA_OS, item.movements().getLast().type());
    }

    @Test
    void cancelarOsConcluidaEProibidoEPreservaConsumo() {
        FakeEstoque estoque = new FakeEstoque();
        CasoDeUsoGerenciarEstoque casoEstoque = new CasoDeUsoGerenciarEstoque(estoque);
        casoEstoque.registerItem(new CasoDeUsoEstoque.RegisterItemEstoqueCommand(
                "produto-1", "Produto", "Descricao", "SKU-1", BigDecimal.TEN, BigDecimal.valueOf(15), BigDecimal.valueOf(5), BigDecimal.valueOf(1), "un", true));
        CasoDeUsoGerenciarOrdemServico casoOs = new CasoDeUsoGerenciarOrdemServico(new FakeOs(), estoque, casoEstoque);
        casoOs.create(new CasoDeUsoOrdemServico.CreateOrdemServicoCommand(
                "os-1", "cliente-1", "", "Servico", "Descricao", "Limpeza", OrdemServico.OrdemServicoStatus.AGENDADA,
                LocalDateTime.now(), null, null, "", "", BigDecimal.ZERO, ""));
        casoOs.adicionarProduto(new CasoDeUsoOrdemServico.AdicionarProdutoOrdemCommand("os-1", "uso-1", "produto-1", BigDecimal.valueOf(3), BigDecimal.valueOf(15)));
        casoOs.conclude("os-1");

        assertThrows(IllegalArgumentException.class, () ->
                casoOs.cancel(new CasoDeUsoOrdemServico.CancelarOrdemServicoCommand("os-1", "Servico desfeito.")));

        ItemEstoque item = estoque.findById("produto-1").orElseThrow();
        assertEquals(OrdemServico.OrdemServicoStatus.CONCLUIDA, casoOs.listAll().getFirst().status());
        assertEquals(0, BigDecimal.valueOf(2).compareTo(item.quantity()));
        assertEquals(ItemEstoque.MovementType.CONSUMO_RESERVA_OS, item.movements().getLast().type());
    }

    @Test
    void cancelarOsNaoConcluidaNaoCancelaCobrancaPaga() {
        FakeEstoque estoque = new FakeEstoque();
        CasoDeUsoGerenciarEstoque casoEstoque = new CasoDeUsoGerenciarEstoque(estoque);
        CasoDeUsoFinanceiro financeiro = Mockito.mock(CasoDeUsoFinanceiro.class);
        LancamentoFinanceiro lancamentoPago = new LancamentoFinanceiro(
                "lancamento-1", LancamentoFinanceiro.Tipo.ENTRY, "", "", "", "", "OS", "cliente-1", "os-1",
                BigDecimal.TEN, LocalDate.now(), LocalDate.now(), LocalDate.now(), LancamentoFinanceiro.Status.PAID,
                false, 1, "", "", List.of());
        Mockito.when(financeiro.buscarLancamentoPorOrdemServico("os-1")).thenReturn(Optional.of(lancamentoPago));
        CasoDeUsoGerenciarOrdemServico casoOs = new CasoDeUsoGerenciarOrdemServico(
                new FakeOs(), estoque, casoEstoque, financeiro, null, new FakeAgenda());
        casoOs.create(new CasoDeUsoOrdemServico.CreateOrdemServicoCommand(
                "os-1", "cliente-1", "", "Servico", "Descricao", "Limpeza", OrdemServico.OrdemServicoStatus.AGENDADA,
                LocalDateTime.now(), null, null, "", "", BigDecimal.TEN, ""));
        casoOs.cancel(new CasoDeUsoOrdemServico.CancelarOrdemServicoCommand("os-1", "Servico desfeito."));

        Mockito.verify(financeiro, Mockito.never()).estornarPagamento(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(financeiro, Mockito.never()).cancel(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void bloqueiaConclusaoOuCancelamentoPorEdicaoComum() {
        FakeEstoque estoque = new FakeEstoque();
        CasoDeUsoGerenciarOrdemServico casoOs = new CasoDeUsoGerenciarOrdemServico(
                new FakeOs(), estoque, new CasoDeUsoGerenciarEstoque(estoque));
        casoOs.create(new CasoDeUsoOrdemServico.CreateOrdemServicoCommand(
                "os-1", "cliente-1", "", "Servico", "Descricao", "Limpeza", OrdemServico.OrdemServicoStatus.AGENDADA,
                LocalDateTime.now(), null, null, "", "", BigDecimal.ZERO, ""));

        assertThrows(IllegalArgumentException.class, () -> casoOs.update(new CasoDeUsoOrdemServico.UpdateOrdemServicoCommand(
                "os-1", "cliente-1", "", "Servico", "Descricao", "Limpeza", OrdemServico.OrdemServicoStatus.CONCLUIDA,
                LocalDateTime.now(), "", "", BigDecimal.ZERO, "", null)));
    }

    @Test
    void bloqueiaCriacaoEmContratoCanceladoOuVencidoEClienteDivergente() {
        FakeEstoque estoque = new FakeEstoque();
        RepositorioContrato contratos = Mockito.mock(RepositorioContrato.class);
        CasoDeUsoGerenciarOrdemServico casoOs = new CasoDeUsoGerenciarOrdemServico(
                new FakeOs(), estoque, new CasoDeUsoGerenciarEstoque(estoque),
                null, null, new FakeAgenda(), contratos);
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

        Mockito.when(contratos.findById("cancelado")).thenReturn(Optional.of(
                contrato("cancelado", "cliente-1", hoje.minusDays(10), hoje.plusDays(10),
                        Contrato.ContratoStatus.CANCELLED)));
        Mockito.when(contratos.findById("vencido")).thenReturn(Optional.of(
                contrato("vencido", "cliente-1", hoje.minusDays(10), hoje.plusDays(10),
                        Contrato.ContratoStatus.EXPIRED)));
        Mockito.when(contratos.findById("cliente-divergente")).thenReturn(Optional.of(
                contrato("cliente-divergente", "cliente-contrato", hoje.minusDays(10), hoje.plusDays(10),
                        Contrato.ContratoStatus.ACTIVE)));

        assertThrows(IllegalArgumentException.class, () -> casoOs.create(comandoContratual(
                "os-cancelada", "cliente-1", "cancelado", hoje.atTime(8, 0))));
        assertThrows(IllegalArgumentException.class, () -> casoOs.create(comandoContratual(
                "os-vencida", "cliente-1", "vencido", hoje.atTime(8, 0))));
        assertThrows(IllegalArgumentException.class, () -> casoOs.create(comandoContratual(
                "os-cliente", "cliente-1", "cliente-divergente", hoje.atTime(8, 0))));
    }

    @Test
    void bloqueiaAlteracaoQueMoveOsParaForaDaVigenciaDoContrato() {
        FakeEstoque estoque = new FakeEstoque();
        RepositorioContrato contratos = Mockito.mock(RepositorioContrato.class);
        CasoDeUsoGerenciarOrdemServico casoOs = new CasoDeUsoGerenciarOrdemServico(
                new FakeOs(), estoque, new CasoDeUsoGerenciarEstoque(estoque),
                null, null, new FakeAgenda(), contratos);
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        Contrato contrato = contrato("contrato-1", "cliente-1", hoje.minusDays(1), hoje.plusDays(10),
                Contrato.ContratoStatus.ACTIVE);
        Mockito.when(contratos.findById("contrato-1")).thenReturn(Optional.of(contrato));
        casoOs.create(comandoContratual("os-1", "cliente-1", "contrato-1", hoje.atTime(8, 0)));

        assertThrows(IllegalArgumentException.class, () -> casoOs.update(
                new CasoDeUsoOrdemServico.UpdateOrdemServicoCommand(
                        "os-1", "cliente-1", "contrato-1", "Visita", "", "visita_contrato",
                        OrdemServico.OrdemServicoStatus.AGENDADA, hoje.plusDays(11).atTime(8, 0),
                        "", "", BigDecimal.ZERO, "", null)));
    }

    @Test
    void updateParcialValidaEstadoEfetivoEPreservaContrato() {
        FakeEstoque estoque = new FakeEstoque();
        FakeOs ordens = new FakeOs();
        RepositorioContrato contratos = Mockito.mock(RepositorioContrato.class);
        CasoDeUsoGerenciarOrdemServico casoOs = new CasoDeUsoGerenciarOrdemServico(
                ordens, estoque, new CasoDeUsoGerenciarEstoque(estoque),
                null, null, new FakeAgenda(), contratos);
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        Mockito.when(contratos.findById("contrato-1")).thenReturn(Optional.of(
                contrato("contrato-1", "cliente-1", hoje.minusDays(1), hoje.plusDays(10),
                        Contrato.ContratoStatus.ACTIVE)));
        casoOs.create(comandoContratual("os-1", "cliente-1", "contrato-1", hoje.plusDays(1).atTime(8, 0)));

        OrdemServico atualizada = casoOs.update(new CasoDeUsoOrdemServico.UpdateOrdemServicoCommand(
                "os-1", null, null, "Visita ajustada", null, null,
                null, null, null, null, null, null, null));

        assertEquals("contrato-1", atualizada.contratoId());
        assertEquals("cliente-1", atualizada.clienteId());
        assertEquals(hoje.plusDays(1).atTime(8, 0), atualizada.dataAgendada());
        assertEquals("Visita ajustada", atualizada.titulo());
    }

    @Test
    void bloqueiaDesvinculacaoOuTrocaDeContratoPorUpdateComum() {
        FakeEstoque estoque = new FakeEstoque();
        FakeOs ordens = new FakeOs();
        RepositorioContrato contratos = Mockito.mock(RepositorioContrato.class);
        CasoDeUsoGerenciarOrdemServico casoOs = new CasoDeUsoGerenciarOrdemServico(
                ordens, estoque, new CasoDeUsoGerenciarEstoque(estoque),
                null, null, new FakeAgenda(), contratos);
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        Mockito.when(contratos.findById("contrato-1")).thenReturn(Optional.of(
                contrato("contrato-1", "cliente-1", hoje.minusDays(1), hoje.plusDays(10),
                        Contrato.ContratoStatus.ACTIVE)));
        casoOs.create(comandoContratual("os-1", "cliente-1", "contrato-1", hoje.plusDays(1).atTime(8, 0)));

        assertThrows(IllegalArgumentException.class, () -> casoOs.update(
                new CasoDeUsoOrdemServico.UpdateOrdemServicoCommand(
                        "os-1", null, "", null, null, null,
                        null, null, null, null, null, null, null)));
        assertThrows(IllegalArgumentException.class, () -> casoOs.update(
                new CasoDeUsoOrdemServico.UpdateOrdemServicoCommand(
                        "os-1", null, "contrato-2", null, null, null,
                        null, null, null, null, null, null, null)));
        assertEquals("contrato-1", ordens.findById("os-1").orElseThrow().contratoId());
    }

    @Test
    void comandoAdministrativoDesvinculaSomenteOsFuturaComMotivoEUsuarioAuditavel() {
        FakeEstoque estoque = new FakeEstoque();
        FakeOs ordens = new FakeOs();
        RepositorioContrato contratos = Mockito.mock(RepositorioContrato.class);
        CasoDeUsoGerenciarOrdemServico casoOs = new CasoDeUsoGerenciarOrdemServico(
                ordens, estoque, new CasoDeUsoGerenciarEstoque(estoque),
                null, null, new FakeAgenda(), contratos);
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        Mockito.when(contratos.findById("contrato-1")).thenReturn(Optional.of(
                contrato("contrato-1", "cliente-1", hoje.minusDays(1), hoje.plusDays(10),
                        Contrato.ContratoStatus.ACTIVE)));
        casoOs.create(comandoContratual("os-1", "cliente-1", "contrato-1", hoje.plusDays(1).atTime(8, 0)));

        OrdemServico desvinculada = casoOs.desvincularContratoAdministrativamente(
                new CasoDeUsoOrdemServico.DesvincularContratoOrdemServicoCommand(
                        "os-1", "Contrato cadastrado por engano", "admin-1"));

        assertTrue(desvinculada.contratoId().isBlank());
        assertEquals("Contrato cadastrado por engano", ordens.ultimoMotivoDesvinculacao);
        assertEquals("admin-1", ordens.ultimoUsuarioDesvinculacao);
    }

    @Test
    void comandoAdministrativoNuncaDesvinculaOsIniciadaOuConcluida() {
        FakeEstoque estoque = new FakeEstoque();
        FakeOs ordens = new FakeOs();
        RepositorioContrato contratos = Mockito.mock(RepositorioContrato.class);
        CasoDeUsoGerenciarOrdemServico casoOs = new CasoDeUsoGerenciarOrdemServico(
                ordens, estoque, new CasoDeUsoGerenciarEstoque(estoque),
                null, null, new FakeAgenda(), contratos);
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        Mockito.when(contratos.findById("contrato-1")).thenReturn(Optional.of(
                contrato("contrato-1", "cliente-1", hoje.minusDays(1), hoje.plusDays(10),
                        Contrato.ContratoStatus.ACTIVE)));
        casoOs.create(comandoContratual("os-iniciada", "cliente-1", "contrato-1", hoje.plusDays(1).atTime(8, 0)));
        casoOs.create(comandoContratual("os-concluida", "cliente-1", "contrato-1", hoje.plusDays(2).atTime(8, 0)));
        casoOs.start("os-iniciada");
        casoOs.start("os-concluida");
        casoOs.conclude(new CasoDeUsoOrdemServico.ConcluirOrdemServicoCommand(
                "os-concluida", "tecnico-1", hoje.plusDays(2).atTime(9, 0), true));

        assertThrows(IllegalArgumentException.class, () -> casoOs.desvincularContratoAdministrativamente(
                new CasoDeUsoOrdemServico.DesvincularContratoOrdemServicoCommand(
                        "os-iniciada", "Correcao", "admin-1")));
        assertThrows(IllegalArgumentException.class, () -> casoOs.desvincularContratoAdministrativamente(
                new CasoDeUsoOrdemServico.DesvincularContratoOrdemServicoCommand(
                        "os-concluida", "Correcao", "admin-1")));
        assertEquals("contrato-1", ordens.findById("os-iniciada").orElseThrow().contratoId());
        assertEquals("contrato-1", ordens.findById("os-concluida").orElseThrow().contratoId());
    }

    private static CasoDeUsoOrdemServico.CreateOrdemServicoCommand comandoContratual(
            String id, String clienteId, String contratoId, LocalDateTime data) {
        return new CasoDeUsoOrdemServico.CreateOrdemServicoCommand(
                id, clienteId, contratoId, "Visita", "", "visita_contrato",
                OrdemServico.OrdemServicoStatus.AGENDADA, data, null, null,
                "", "", BigDecimal.ZERO, "",
                br.com.sigla.dominio.servicos.DadosFormularioServico.vazio(),
                OrdemServico.RegraCobranca.COBERTA_PELO_CONTRATO);
    }

    private static Contrato contrato(String id, String clienteId, LocalDate inicio, LocalDate fim,
                                     Contrato.ContratoStatus status) {
        return new Contrato(id, clienteId, "Contrato", inicio, fim,
                Contrato.ContratoType.MONTHLY, Contrato.ServiceFrequency.MONTHLY, status,
                Contrato.RenewalRule.MANUAL, BigDecimal.TEN, true, 10, "");
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
        assertEquals(VisitaAgendada.Recurrence.NONE, evento.recurrence());

        casoOs.cancel(new CasoDeUsoOrdemServico.CancelarOrdemServicoCommand("os-1", "Cliente cancelou."));
        assertEquals(VisitaAgendada.VisitStatus.CANCELLED, agenda.findById("os-os-1").orElseThrow().status());
    }

    @Test
    void reagendamentoDaOsAtualizaSuaProjecaoSemComandoInverso() {
        FakeEstoque estoque = new FakeEstoque();
        CasoDeUsoGerenciarEstoque casoEstoque = new CasoDeUsoGerenciarEstoque(estoque);
        FakeAgenda agenda = new FakeAgenda();
        CasoDeUsoGerenciarOrdemServico casoOs = new CasoDeUsoGerenciarOrdemServico(new FakeOs(), estoque, casoEstoque, null, null, agenda);
        LocalDateTime inicio = LocalDateTime.of(2026, 4, 10, 8, 0);
        casoOs.create(new CasoDeUsoOrdemServico.CreateOrdemServicoCommand(
                "os-1", "cliente-1", "", "Servico", "Descricao", "Limpeza", OrdemServico.OrdemServicoStatus.AGENDADA,
                inicio, null, null, "resp-1", "", BigDecimal.ZERO, ""));

        OrdemServico reagendada = casoOs.reschedule(new CasoDeUsoOrdemServico.ReagendarOrdemServicoCommand(
                "os-1", inicio.plusDays(2), inicio.plusDays(2).plusHours(1)));
        assertEquals(inicio.plusDays(2), reagendada.dataAgendada());
        assertEquals(inicio.plusDays(2), agenda.findById("os-os-1").orElseThrow().startAt());
    }

    @Test
    void iniciarOsComValorNaoGeraFinanceiro() {
        FakeEstoque estoque = new FakeEstoque();
        CasoDeUsoGerenciarEstoque casoEstoque = new CasoDeUsoGerenciarEstoque(estoque);
        CasoDeUsoFinanceiro financeiro = Mockito.mock(CasoDeUsoFinanceiro.class);
        CasoDeUsoGerenciarOrdemServico casoOs = new CasoDeUsoGerenciarOrdemServico(
                new FakeOs(), estoque, casoEstoque, financeiro, null, new FakeAgenda());
        casoOs.create(new CasoDeUsoOrdemServico.CreateOrdemServicoCommand(
                "os-1", "cliente-1", "", "Servico", "Descricao", "Limpeza", OrdemServico.OrdemServicoStatus.AGENDADA,
                LocalDateTime.now(), null, null, "", "", BigDecimal.valueOf(100), ""));

        // Iniciar uma OS com valor nao pode gerar financeiro (status EM_ANDAMENTO, nao CONCLUIDA).
        OrdemServico iniciada = casoOs.start("os-1");

        assertEquals(OrdemServico.OrdemServicoStatus.EM_ANDAMENTO, iniciada.status());
        Mockito.verify(financeiro, Mockito.never()).gerarContaReceberOrdemServico(Mockito.any());
    }

    @Test
    void concluirOsComValorGeraFinanceiro() {
        FakeEstoque estoque = new FakeEstoque();
        CasoDeUsoGerenciarEstoque casoEstoque = new CasoDeUsoGerenciarEstoque(estoque);
        CasoDeUsoFinanceiro financeiro = Mockito.mock(CasoDeUsoFinanceiro.class);
        CasoDeUsoGerenciarOrdemServico casoOs = new CasoDeUsoGerenciarOrdemServico(
                new FakeOs(), estoque, casoEstoque, financeiro, null, new FakeAgenda());
        casoOs.create(new CasoDeUsoOrdemServico.CreateOrdemServicoCommand(
                "os-1", "cliente-1", "", "Servico", "Descricao", "Limpeza", OrdemServico.OrdemServicoStatus.AGENDADA,
                LocalDateTime.now(), null, null, "", "", BigDecimal.valueOf(100), ""));
        casoOs.start("os-1");

        casoOs.conclude(new CasoDeUsoOrdemServico.ConcluirOrdemServicoCommand("os-1", "func-1", null, true));

        Mockito.verify(financeiro).gerarContaReceberOrdemServico(Mockito.any());
    }

    static final class FakeOs implements RepositorioOrdemServico {
        private final Map<String, OrdemServico> storage = new HashMap<>();
        private String ultimoMotivoDesvinculacao;
        private String ultimoUsuarioDesvinculacao;

        @Override
        public OrdemServico save(OrdemServico ordemServico) {
            storage.put(ordemServico.id(), ordemServico);
            return ordemServico;
        }

        @Override
        public OrdemServico desvincularContratoAdministrativamente(String id, String motivo, String usuarioId) {
            OrdemServico atual = findById(id).orElseThrow();
            ultimoMotivoDesvinculacao = motivo;
            ultimoUsuarioDesvinculacao = usuarioId;
            return save(new OrdemServico(
                    atual.id(), atual.numeroOs(), atual.clienteId(), "", atual.titulo(), atual.descricao(),
                    atual.tipoServico(), atual.status(), atual.dataAgendada(), atual.dataInicio(), atual.dataFim(),
                    atual.responsavelInternoId(), atual.executadoPorId(), atual.foiFeito(), atual.pago(),
                    atual.valorServico(), atual.assinaturaCliente(), atual.produtos(), atual.anexos(),
                    atual.observacoes(), atual.dadosFormulario()));
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
        public void registrarMovimento(ItemEstoque item, ItemEstoque.InventoryMovement movimento) {
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

        @Override
        public List<VisitaAgendada> findByResponsavel(String responsibleId) {
            if (responsibleId == null || responsibleId.isBlank()) {
                return List.of();
            }
            return storage.values().stream()
                    .filter(schedule -> responsibleId.equals(schedule.responsibleId()))
                    .toList();
        }
    }
}
