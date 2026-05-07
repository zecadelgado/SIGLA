package br.com.sigla.aplicacao.agenda.casodeuso;

import br.com.sigla.aplicacao.agenda.porta.entrada.CasoDeUsoAgenda;
import br.com.sigla.aplicacao.agenda.porta.saida.RepositorioAgenda;
import br.com.sigla.dominio.agenda.VisitaAgendada;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CasoDeUsoGerenciarAgendaTest {

    @Test
    void expandeRecorrenciaMensalEQuinzenalNoPeriodo() {
        FakeRepositorioAgenda repositorio = new FakeRepositorioAgenda();
        CasoDeUsoGerenciarAgenda casoDeUso = new CasoDeUsoGerenciarAgenda(repositorio);

        casoDeUso.schedule(evento("mensal", VisitaAgendada.VisitType.MONTHLY, VisitaAgendada.Recurrence.MONTHLY, LocalDate.of(2026, 1, 10), "resp-1"));
        casoDeUso.schedule(evento("quinzenal", VisitaAgendada.VisitType.BIWEEKLY, VisitaAgendada.Recurrence.BIWEEKLY, LocalDate.of(2026, 1, 1), "resp-2"));

        List<VisitaAgendada> janeiro = casoDeUso.listBetween(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertEquals(4, janeiro.size());
    }

    @Test
    void bloqueiaConflitoParaMesmoResponsavelEValidaLembrete() {
        FakeRepositorioAgenda repositorio = new FakeRepositorioAgenda();
        CasoDeUsoGerenciarAgenda casoDeUso = new CasoDeUsoGerenciarAgenda(repositorio);
        casoDeUso.schedule(evento("evento-1", VisitaAgendada.VisitType.ONE_OFF, VisitaAgendada.Recurrence.NONE, LocalDate.of(2026, 2, 1), "resp-1"));

        assertThrows(IllegalArgumentException.class, () ->
                casoDeUso.schedule(evento("evento-2", VisitaAgendada.VisitType.ONE_OFF, VisitaAgendada.Recurrence.NONE, LocalDate.of(2026, 2, 1), "resp-1")));
        assertThrows(IllegalArgumentException.class, () ->
                casoDeUso.schedule(new CasoDeUsoAgenda.ScheduleVisitCommand(
                        "evento-3", "cliente-1", "", "", "", VisitaAgendada.VisitType.ONE_OFF, VisitaAgendada.Recurrence.NONE,
                        LocalDate.of(2026, 2, 2), "Evento", "servico_avulso", "", LocalDateTime.of(2026, 2, 2, 8, 0),
                        LocalDateTime.of(2026, 2, 2, 9, 0), false, VisitaAgendada.VisitStatus.SCHEDULED,
                        VisitaAgendada.VisitPriority.NORMAL, "resp-2", true, -1, "")));
    }

    @Test
    void permiteReagendarCancelarEConcluirComAcoesControladas() {
        FakeRepositorioAgenda repositorio = new FakeRepositorioAgenda();
        CasoDeUsoGerenciarAgenda casoDeUso = new CasoDeUsoGerenciarAgenda(repositorio);
        casoDeUso.schedule(evento("evento-1", VisitaAgendada.VisitType.ONE_OFF, VisitaAgendada.Recurrence.NONE, LocalDate.of(2026, 3, 1), "resp-1"));

        casoDeUso.reschedule(new CasoDeUsoAgenda.RescheduleVisitCommand("evento-1", LocalDateTime.of(2026, 3, 2, 10, 0), LocalDateTime.of(2026, 3, 2, 11, 0)));
        assertEquals(LocalDate.of(2026, 3, 2), repositorio.findById("evento-1").orElseThrow().scheduledDate());

        casoDeUso.complete(new CasoDeUsoAgenda.ChangeVisitStatusCommand("evento-1", ""));
        assertEquals(VisitaAgendada.VisitStatus.COMPLETED, repositorio.findById("evento-1").orElseThrow().status());

        casoDeUso.schedule(evento("evento-2", VisitaAgendada.VisitType.ONE_OFF, VisitaAgendada.Recurrence.NONE, LocalDate.of(2026, 3, 3), "resp-1"));
        casoDeUso.cancel(new CasoDeUsoAgenda.ChangeVisitStatusCommand("evento-2", "Cliente pediu."));
        assertEquals(VisitaAgendada.VisitStatus.CANCELLED, repositorio.findById("evento-2").orElseThrow().status());
        assertThrows(IllegalArgumentException.class, () -> casoDeUso.complete(new CasoDeUsoAgenda.ChangeVisitStatusCommand("evento-2", "")));
    }

    private CasoDeUsoAgenda.ScheduleVisitCommand evento(
            String id,
            VisitaAgendada.VisitType type,
            VisitaAgendada.Recurrence recurrence,
            LocalDate date,
            String responsibleId
    ) {
        return new CasoDeUsoAgenda.ScheduleVisitCommand(
                id,
                "cliente-1",
                "",
                "",
                "",
                type,
                recurrence,
                date,
                "Evento " + id,
                "servico_avulso",
                "",
                date.atTime(8, 0),
                date.atTime(9, 0),
                false,
                VisitaAgendada.VisitStatus.SCHEDULED,
                VisitaAgendada.VisitPriority.NORMAL,
                responsibleId,
                true,
                1,
                ""
        );
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
    }
}
