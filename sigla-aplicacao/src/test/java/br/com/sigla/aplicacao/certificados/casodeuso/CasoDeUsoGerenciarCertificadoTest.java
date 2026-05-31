package br.com.sigla.aplicacao.certificados.casodeuso;

import br.com.sigla.aplicacao.agenda.porta.saida.RepositorioAgenda;
import br.com.sigla.aplicacao.certificados.porta.entrada.CasoDeUsoCertificado;
import br.com.sigla.aplicacao.certificados.porta.saida.RepositorioCertificado;
import br.com.sigla.dominio.agenda.VisitaAgendada;
import br.com.sigla.dominio.certificados.Certificado;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CasoDeUsoGerenciarCertificadoTest {

    @Test
    void calculaValidadePadraoERegistraVencimentoNoCalendario() {
        FakeRepositorioCertificado certificados = new FakeRepositorioCertificado();
        FakeRepositorioAgenda agenda = new FakeRepositorioAgenda();
        CasoDeUsoGerenciarCertificado casoDeUso = new CasoDeUsoGerenciarCertificado(certificados, agenda);

        casoDeUso.issue(new CasoDeUsoCertificado.IssueCertificadoCommand(
                "cert-1",
                "cliente-1",
                "",
                "",
                "Higiene",
                LocalDate.of(2026, 1, 1),
                null,
                0,
                true,
                Certificado.CertificadoStatus.ACTIVE,
                0,
                ""
        ));

        Certificado certificado = certificados.findAll().getFirst();
        assertEquals(LocalDate.of(2026, 7, 1), certificado.validUntil());
        assertEquals(6, certificado.intervalMonths());
        assertEquals(15, certificado.renewalAlertDays());

        VisitaAgendada evento = agenda.findAll().getFirst();
        assertEquals("certificado-vencimento-cert-1", evento.id());
        assertEquals("cert-1", evento.certificateId());
        assertEquals("certificado_vencimento", evento.serviceType());
        assertEquals(LocalDate.of(2026, 7, 1), evento.scheduledDate());
    }

    private static final class FakeRepositorioCertificado implements RepositorioCertificado {
        private final Map<String, Certificado> storage = new ConcurrentHashMap<>();

        @Override
        public void save(Certificado certificate) {
            storage.put(certificate.id(), certificate);
        }

        @Override
        public List<Certificado> findAll() {
            return storage.values().stream().toList();
        }

        @Override
        public Optional<Certificado> findById(String id) {
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
    }
}
