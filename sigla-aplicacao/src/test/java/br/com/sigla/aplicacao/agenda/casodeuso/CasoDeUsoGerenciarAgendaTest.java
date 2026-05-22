package br.com.sigla.aplicacao.agenda.casodeuso;

import br.com.sigla.aplicacao.agenda.porta.entrada.CasoDeUsoAgenda;
import br.com.sigla.aplicacao.agenda.porta.saida.RepositorioAgenda;
import br.com.sigla.dominio.agenda.VisitaAgendada;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CasoDeUsoGerenciarAgendaTest {

    @Test
    void shouldPersistOrderAndContractReferencesSeparately() {
        InMemoryRepositorioAgenda repository = new InMemoryRepositorioAgenda();
        CasoDeUsoGerenciarAgenda useCase = new CasoDeUsoGerenciarAgenda(repository);
        LocalDate date = LocalDate.of(2026, 5, 21);

        useCase.schedule(new CasoDeUsoAgenda.ScheduleVisitCommand(
                "AGE-100",
                "CLI-100",
                "OS-100",
                "CON-100",
                VisitaAgendada.VisitType.MONTHLY,
                VisitaAgendada.Recurrence.MONTHLY,
                date,
                "Visita mensal",
                "SERVICO",
                "",
                LocalDateTime.of(2026, 5, 21, 8, 0),
                LocalDateTime.of(2026, 5, 21, 9, 0),
                false,
                VisitaAgendada.VisitStatus.SCHEDULED,
                VisitaAgendada.VisitPriority.HIGH,
                "FUNC-100",
                true,
                2,
                "Lembrar responsavel"
        ));

        VisitaAgendada saved = repository.findById("AGE-100").orElseThrow();
        assertEquals("OS-100", saved.orderId());
        assertEquals("CON-100", saved.contractId());
        assertEquals(VisitaAgendada.Recurrence.MONTHLY, saved.recurrence());
        assertTrue(saved.reminderActive());
        assertEquals(2, saved.daysBeforeReminder());
    }

    private static final class InMemoryRepositorioAgenda implements RepositorioAgenda {
        private final Map<String, VisitaAgendada> agenda = new HashMap<>();

        @Override
        public void save(VisitaAgendada schedule) {
            agenda.put(schedule.id(), schedule);
        }

        @Override
        public List<VisitaAgendada> findAll() {
            return agenda.values().stream().toList();
        }

        @Override
        public Optional<VisitaAgendada> findById(String id) {
            return Optional.ofNullable(agenda.get(id));
        }
    }
}
