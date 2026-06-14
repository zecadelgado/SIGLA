package br.com.sigla.aplicacao.contratos.casodeuso;

import br.com.sigla.aplicacao.agenda.porta.saida.RepositorioAgenda;
import br.com.sigla.aplicacao.contratos.porta.entrada.CasoDeUsoContrato;
import br.com.sigla.aplicacao.contratos.porta.saida.RepositorioContrato;
import br.com.sigla.dominio.agenda.VisitaAgendada;
import br.com.sigla.dominio.contratos.Contrato;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
