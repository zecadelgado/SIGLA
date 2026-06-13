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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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

    @Test
    void renovarMarcaAntigoComoSubstituidoEEmiteNovoAtivo() {
        FakeRepositorioCertificado certificados = new FakeRepositorioCertificado();
        CasoDeUsoGerenciarCertificado casoDeUso = new CasoDeUsoGerenciarCertificado(certificados, new FakeRepositorioAgenda());
        casoDeUso.issue(new CasoDeUsoCertificado.IssueCertificadoCommand(
                "cert-1", "cliente-1", "", "", "Higiene",
                LocalDate.of(2026, 1, 1), null, 6, true, Certificado.CertificadoStatus.ACTIVE, 15, ""));

        String novoId = casoDeUso.renovar(new CasoDeUsoCertificado.RenovarCertificadoCommand(
                "cert-1", LocalDate.of(2026, 7, 1), 6));

        assertNotEquals("cert-1", novoId);
        assertEquals(2, certificados.findAll().size());
        assertEquals(Certificado.CertificadoStatus.REPLACED, certificados.findById("cert-1").orElseThrow().status());
        Certificado novo = certificados.findById(novoId).orElseThrow();
        assertEquals(Certificado.CertificadoStatus.ACTIVE, novo.status());
        assertEquals(LocalDate.of(2027, 1, 1), novo.validUntil());
        assertEquals("cliente-1", novo.customerId());
    }

    @Test
    void marcarVencidosMudaAtivoVencidoParaExpired() {
        FakeRepositorioCertificado certificados = new FakeRepositorioCertificado();
        CasoDeUsoGerenciarCertificado casoDeUso = new CasoDeUsoGerenciarCertificado(certificados, new FakeRepositorioAgenda());
        casoDeUso.issue(new CasoDeUsoCertificado.IssueCertificadoCommand(
                "cert-1", "cliente-1", "", "", "Higiene",
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 7, 1), 6, true, Certificado.CertificadoStatus.ACTIVE, 15, ""));

        casoDeUso.marcarVencidos(LocalDate.of(2026, 6, 8));
        assertEquals(Certificado.CertificadoStatus.EXPIRED, certificados.findById("cert-1").orElseThrow().status());
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
