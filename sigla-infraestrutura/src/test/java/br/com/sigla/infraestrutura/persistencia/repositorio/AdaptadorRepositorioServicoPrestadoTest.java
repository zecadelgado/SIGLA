package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.servicos.porta.saida.RepositorioOrdemServico;
import br.com.sigla.dominio.servicos.OrdemServico;
import br.com.sigla.dominio.servicos.ServicoPrestado;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica a conversao ServicoPrestado &lt;-&gt; OrdemServico do adaptador que
 * agora reaproveita as tabelas de ordem de servico. Usa um
 * {@link RepositorioOrdemServico} em memoria como backing store (o mesmo
 * contrato que, em producao, grava em {@code ordens_servico}).
 */
class AdaptadorRepositorioServicoPrestadoTest {

    private final RepositorioOrdemServico ordensFake = new RepositorioOrdemServicoFake();
    private final AdaptadorRepositorioServicoPrestado adaptador = new AdaptadorRepositorioServicoPrestado(ordensFake);

    @Test
    void deveGravarComoOrdemServicoMarcadaEPersistirAssinaturaEAnexos() {
        ServicoPrestado servico = new ServicoPrestado(
                "sp-1", "cli-1", "ctr-1", null, "func-1",
                LocalDate.of(2026, 6, 1), "Dedetizacao mensal", new BigDecimal("250.00"),
                ServicoPrestado.PaymentStatus.PARTIALLY_PAID, ServicoPrestado.SignatureType.DIGITAL,
                "var/attachments/sp-1/signature/assinatura.png",
                List.of(new ServicoPrestado.Attachment("foto.jpg", "var/attachments/sp-1/foto.jpg", "image/jpeg")),
                ServicoPrestado.ServiceStatus.COMPLETED, ServicoPrestado.ServicePriority.HIGH, "ok");

        adaptador.save(servico);

        OrdemServico gravada = ordensFake.findById("sp-1").orElseThrow();
        assertEquals(AdaptadorRepositorioServicoPrestado.TIPO_SERVICO_PRESTADO, gravada.tipoServico());
        assertTrue(gravada.assinaturaCliente(), "assinatura DIGITAL deve marcar assinaturaCliente");
        assertFalse(gravada.pago(), "PARTIALLY_PAID nao e PAID");
        assertEquals(OrdemServico.OrdemServicoStatus.CONCLUIDA, gravada.status());
        assertEquals(new BigDecimal("250.00"), gravada.valorServico());
        assertEquals(2, gravada.anexos().size(), "assinatura + 1 anexo");
        assertTrue(gravada.anexos().stream().anyMatch(a -> a.tipo() == OrdemServico.TipoAnexo.ASSINATURA));
    }

    @Test
    void deveFazerRoundTripDosCamposComCorrespondencia() {
        ServicoPrestado original = new ServicoPrestado(
                "sp-2", "cli-9", null, null, "func-9",
                LocalDate.of(2026, 5, 20), "Visita avulsa", new BigDecimal("100.00"),
                ServicoPrestado.PaymentStatus.PAID, ServicoPrestado.SignatureType.MANUAL,
                "var/attachments/sp-2/signature/sig.png",
                List.of(new ServicoPrestado.Attachment("comprovante.pdf", "var/attachments/sp-2/c.pdf", "application/pdf")),
                ServicoPrestado.ServiceStatus.COMPLETED, ServicoPrestado.ServicePriority.NORMAL, "concluido");
        adaptador.save(original);

        ServicoPrestado lido = adaptador.findById("sp-2").orElseThrow();
        assertEquals("cli-9", lido.customerId());
        assertEquals("func-9", lido.employeeId());
        assertEquals("Visita avulsa", lido.description());
        assertEquals(new BigDecimal("100.00"), lido.amountCharged());
        assertEquals(LocalDate.of(2026, 5, 20), lido.executionDate());
        assertEquals(ServicoPrestado.PaymentStatus.PAID, lido.paymentStatus());
        assertEquals(ServicoPrestado.ServiceStatus.COMPLETED, lido.serviceStatus());
        assertEquals(ServicoPrestado.SignatureType.MANUAL, lido.signatureType());
        assertEquals("var/attachments/sp-2/signature/sig.png", lido.signaturePath());
        assertEquals(1, lido.attachments().size());
        assertEquals("application/pdf", lido.attachments().get(0).contentType());
    }

    @Test
    void naoDeveTratarOrdemDeServicoComumComoServicoPrestado() {
        // Uma OS comum (sem o marcador) gravada direto no repositorio de ordens
        ordensFake.save(new OrdemServico(
                "os-comum", null, "cli-1", "", "Dedetizacao", "Dedetizacao", "DEDETIZACAO",
                OrdemServico.OrdemServicoStatus.ABERTA, null, null, null, null, null,
                false, false, BigDecimal.ZERO, false, List.of(), List.of(), ""));

        assertTrue(adaptador.findById("os-comum").isEmpty(), "OS comum nao deve virar ServicoPrestado");
        assertTrue(adaptador.findAll().isEmpty(), "findAll deve filtrar pelo marcador");
    }

    private static final class RepositorioOrdemServicoFake implements RepositorioOrdemServico {
        private final Map<String, OrdemServico> dados = new LinkedHashMap<>();

        @Override
        public OrdemServico save(OrdemServico ordemServico) {
            dados.put(ordemServico.id(), ordemServico);
            return ordemServico;
        }

        @Override
        public List<OrdemServico> findAll() {
            return List.copyOf(dados.values());
        }

        @Override
        public Optional<OrdemServico> findById(String id) {
            return Optional.ofNullable(dados.get(id));
        }
    }
}
