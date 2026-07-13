package br.com.sigla.aplicacao.contratos.casodeuso;

import br.com.sigla.aplicacao.agenda.porta.saida.RepositorioAgenda;
import br.com.sigla.aplicacao.contratos.porta.entrada.CasoDeUsoContrato;
import br.com.sigla.aplicacao.contratos.porta.saida.RepositorioContrato;
import br.com.sigla.aplicacao.financeiro.porta.entrada.CasoDeUsoFinanceiro;
import br.com.sigla.aplicacao.servicos.porta.entrada.CasoDeUsoOrdemServico;
import br.com.sigla.dominio.agenda.VisitaAgendada;
import br.com.sigla.dominio.contratos.Contrato;
import br.com.sigla.dominio.servicos.OrdemServico;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

class CasoDeUsoGerenciarContratoTest {

    @Test
    void registraVencimentoDeContratoNoCalendarioSemDuplicarEvento() {
        FakeRepositorioContrato contratos = new FakeRepositorioContrato();
        FakeRepositorioAgenda agenda = new FakeRepositorioAgenda();
        CasoDeUsoGerenciarContrato casoDeUso = new CasoDeUsoGerenciarContrato(contratos, agenda);

        CasoDeUsoContrato.CreateContratoCommand command = new CasoDeUsoContrato.CreateContratoCommand(
                "contrato-1",
                "cliente-1",
                "Contrato mensal",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 6, 30),
                Contrato.ContratoType.MONTHLY,
                Contrato.ServiceFrequency.MONTHLY,
                Contrato.ContratoStatus.ACTIVE,
                Contrato.RenewalRule.MANUAL,
                BigDecimal.valueOf(500),
                true,
                15,
                ""
        );
        casoDeUso.create(command);
        casoDeUso.create(command);

        assertEquals(1, agenda.findAll().size());
        VisitaAgendada evento = agenda.findAll().getFirst();
        assertEquals("contrato-vencimento-contrato-1", evento.id());
        assertEquals("contrato-1", evento.contractId());
        assertEquals("contrato_vencimento", evento.serviceType());
        assertEquals(LocalDate.of(2026, 6, 30), evento.scheduledDate());
        assertTrue(evento.endAt().isAfter(evento.startAt()));
    }

    @Test
    void encerrarMarcaCanceladoEExigeMotivo() {
        FakeRepositorioContrato contratos = new FakeRepositorioContrato();
        CasoDeUsoGerenciarContrato casoDeUso = new CasoDeUsoGerenciarContrato(contratos, new FakeRepositorioAgenda());
        casoDeUso.create(comando("contrato-1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)));

        assertThrows(IllegalArgumentException.class,
                () -> casoDeUso.encerrar(new CasoDeUsoContrato.EncerrarContratoCommand("contrato-1", " ")));

        casoDeUso.encerrar(new CasoDeUsoContrato.EncerrarContratoCommand("contrato-1", "Cliente cancelou"));
        Contrato contrato = contratos.findById("contrato-1").orElseThrow();
        assertEquals(Contrato.ContratoStatus.CANCELLED, contrato.status());
        assertTrue(contrato.notes().contains("Cliente cancelou"));
    }

    @Test
    void encerrarCancelaOsECobrancasContratuaisFuturasPendentes() {
        FakeRepositorioContrato contratos = new FakeRepositorioContrato();
        CasoDeUsoFinanceiro financeiro = mock(CasoDeUsoFinanceiro.class);
        CasoDeUsoOrdemServico os = mock(CasoDeUsoOrdemServico.class);
        LocalDateTime agora = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
        OrdemServico futura = ordem("os-futura", "contrato-1", OrdemServico.OrdemServicoStatus.AGENDADA, agora.plusDays(3));
        OrdemServico andamento = ordem("os-andamento", "contrato-1", OrdemServico.OrdemServicoStatus.EM_ANDAMENTO, agora.plusDays(2));
        OrdemServico concluida = ordem("os-concluida", "contrato-1", OrdemServico.OrdemServicoStatus.CONCLUIDA, agora.plusDays(1));
        OrdemServico passada = ordem("os-passada", "contrato-1", OrdemServico.OrdemServicoStatus.AGENDADA, agora.minusDays(1));
        OrdemServico outroContrato = ordem("os-outro", "contrato-2", OrdemServico.OrdemServicoStatus.AGENDADA, agora.plusDays(1));
        when(os.listAll()).thenReturn(List.of(futura, andamento, concluida, passada, outroContrato));
        CasoDeUsoGerenciarContrato casoDeUso = new CasoDeUsoGerenciarContrato(
                contratos, new FakeRepositorioAgenda(), financeiro, os, chave -> false);
        casoDeUso.create(comando("contrato-1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)));

        casoDeUso.encerrar(new CasoDeUsoContrato.EncerrarContratoCommand("contrato-1", "Cliente cancelou"));

        verify(os).cancel(new CasoDeUsoOrdemServico.CancelarOrdemServicoCommand(
                "os-futura", "Contrato encerrado: Cliente cancelou"));
        verify(os, never()).cancel(new CasoDeUsoOrdemServico.CancelarOrdemServicoCommand(
                "os-andamento", "Contrato encerrado: Cliente cancelou"));
        verify(os, never()).cancel(new CasoDeUsoOrdemServico.CancelarOrdemServicoCommand(
                "os-concluida", "Contrato encerrado: Cliente cancelou"));
        verify(os, never()).cancel(new CasoDeUsoOrdemServico.CancelarOrdemServicoCommand(
                "os-passada", "Contrato encerrado: Cliente cancelou"));
        verify(os, never()).cancel(new CasoDeUsoOrdemServico.CancelarOrdemServicoCommand(
                "os-outro", "Contrato encerrado: Cliente cancelou"));
        verify(financeiro).cancelarLancamentosPendentesDoContrato(
                org.mockito.ArgumentMatchers.eq("contrato-1"),
                org.mockito.ArgumentMatchers.eq("Contrato encerrado: Cliente cancelou"),
                org.mockito.ArgumentMatchers.any(LocalDate.class));
    }

    @Test
    void renovarProrrogaDataFimEReativa() {
        FakeRepositorioContrato contratos = new FakeRepositorioContrato();
        CasoDeUsoGerenciarContrato casoDeUso = new CasoDeUsoGerenciarContrato(contratos, new FakeRepositorioAgenda());
        casoDeUso.create(comando("contrato-1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30)));

        casoDeUso.renovar(new CasoDeUsoContrato.RenovarContratoCommand("contrato-1", LocalDate.of(2026, 12, 31)));
        Contrato contrato = contratos.findById("contrato-1").orElseThrow();
        assertEquals(LocalDate.of(2026, 12, 31), contrato.endDate());
        assertEquals(Contrato.ContratoStatus.ACTIVE, contrato.status());
    }

    @Test
    void marcarVencidosMudaAtivoVencidoParaExpired() {
        FakeRepositorioContrato contratos = new FakeRepositorioContrato();
        CasoDeUsoGerenciarContrato casoDeUso = new CasoDeUsoGerenciarContrato(contratos, new FakeRepositorioAgenda());
        casoDeUso.create(comando("contrato-1", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 6, 30)));

        List<Contrato> afetados = casoDeUso.marcarVencidos(LocalDate.of(2026, 6, 8));
        assertEquals(1, afetados.size());
        assertEquals(Contrato.ContratoStatus.EXPIRED, contratos.findById("contrato-1").orElseThrow().status());
    }

    @Test
    void materializaOsPropriaPorOcorrenciaFuturaSemDuplicarNoRetry() {
        FakeRepositorioContrato contratos = new FakeRepositorioContrato();
        CasoDeUsoOrdemServico os = mock(CasoDeUsoOrdemServico.class);
        List<OrdemServico> materializadas = new ArrayList<>();
        when(os.listAll()).thenAnswer(ignored -> List.copyOf(materializadas));
        when(os.create(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            CasoDeUsoOrdemServico.CreateOrdemServicoCommand c = invocation.getArgument(0);
            OrdemServico criada = new OrdemServico(c.id(), null, c.clienteId(), c.contratoId(), c.titulo(),
                    c.descricao(), c.tipoServico(), OrdemServico.OrdemServicoStatus.AGENDADA,
                    c.dataAgendada(), null, null, c.responsavelInternoId(), c.executadoPorId(), false,
                    false, c.valorServico(), false, List.of(), List.of(), c.observacoes(), c.dadosFormulario());
            materializadas.add(criada);
            return criada;
        });
        CasoDeUsoGerenciarContrato casoDeUso = new CasoDeUsoGerenciarContrato(
                contratos, new FakeRepositorioAgenda(), null, os, chave -> true);
        CasoDeUsoContrato.CreateContratoCommand command = comando(
                "contrato-recorrente", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 10, 1));

        casoDeUso.create(command);
        casoDeUso.create(command);

        assertEquals(3, materializadas.size());
        assertEquals(3, materializadas.stream().map(OrdemServico::id).distinct().count());
        assertTrue(materializadas.stream().allMatch(item -> item.contratoId().equals("contrato-recorrente")));
    }

    @Test
    void flagDesabilitadaImpedeMaterializacaoNoJava() {
        FakeRepositorioContrato contratos = new FakeRepositorioContrato();
        CasoDeUsoOrdemServico os = mock(CasoDeUsoOrdemServico.class);
        CasoDeUsoGerenciarContrato casoDeUso = new CasoDeUsoGerenciarContrato(
                contratos, new FakeRepositorioAgenda(), null, os, chave -> false);

        casoDeUso.create(comando("contrato-sem-ocorrencia", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 10, 1)));

        verify(os, never()).create(org.mockito.ArgumentMatchers.any());
    }

    private static OrdemServico ordem(String id, String contratoId, OrdemServico.OrdemServicoStatus status,
                                      LocalDateTime dataAgendada) {
        return new OrdemServico(id, null, "cliente-1", contratoId, "Visita", "", "visita_contrato",
                status, dataAgendada, status == OrdemServico.OrdemServicoStatus.EM_ANDAMENTO ? dataAgendada.minusHours(1) : null,
                status == OrdemServico.OrdemServicoStatus.CONCLUIDA ? dataAgendada : null, "", "",
                status == OrdemServico.OrdemServicoStatus.CONCLUIDA, false, BigDecimal.ZERO, false,
                List.of(), List.of(), "", null);
    }

    private static CasoDeUsoContrato.CreateContratoCommand comando(String id, LocalDate inicio, LocalDate fim) {
        return new CasoDeUsoContrato.CreateContratoCommand(
                id, "cliente-1", "Contrato mensal", inicio, fim,
                Contrato.ContratoType.MONTHLY, Contrato.ServiceFrequency.MONTHLY,
                Contrato.ContratoStatus.ACTIVE, Contrato.RenewalRule.MANUAL,
                BigDecimal.valueOf(500), true, 15, "");
    }

    private static final class FakeRepositorioContrato implements RepositorioContrato {
        private final Map<String, Contrato> storage = new ConcurrentHashMap<>();

        @Override
        public void save(Contrato contract) {
            storage.put(contract.id(), contract);
        }

        @Override
        public List<Contrato> findAll() {
            return storage.values().stream().toList();
        }

        @Override
        public Optional<Contrato> findById(String id) {
            return Optional.ofNullable(storage.get(id));
        }
    }

    private static final class FakeRepositorioAgenda implements RepositorioAgenda {
        private final Map<String, VisitaAgendada> storage = new ConcurrentHashMap<>();

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
