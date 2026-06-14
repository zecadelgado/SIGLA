package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.servicos.porta.saida.RepositorioOrdemServico;
import br.com.sigla.aplicacao.servicos.porta.saida.RepositorioServicoPrestado;
import br.com.sigla.dominio.servicos.OrdemServico;
import br.com.sigla.dominio.servicos.ServicoPrestado;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Persiste {@link ServicoPrestado} reaproveitando as tabelas que ja existem e ja
 * gravam ({@code ordens_servico} + {@code ordem_servico_anexos}), via a porta
 * {@link RepositorioOrdemServico}. Antes este adaptador caia num mapa em memoria
 * (a interface Spring Data estava {@code @NoRepositoryBean} e apontava para a
 * tabela inexistente {@code provided_servicos}), perdendo os dados ao reiniciar.
 *
 * <p>Os registros de servico prestado sao marcados com {@code tipoServico =}
 * {@value #TIPO_SERVICO_PRESTADO} para serem reconhecidos na leitura sem se
 * confundir com ordens de servico comuns. Alguns campos do agregado original
 * (prioridade, agendamento e granularidade de status de pagamento) nao tem
 * coluna correspondente e nao sao preservados no retorno.
 */
@Repository
public class AdaptadorRepositorioServicoPrestado implements RepositorioServicoPrestado {

    static final String TIPO_SERVICO_PRESTADO = "SERVICO_PRESTADO";

    private final RepositorioOrdemServico repositorioOrdemServico;

    public AdaptadorRepositorioServicoPrestado(RepositorioOrdemServico repositorioOrdemServico) {
        this.repositorioOrdemServico = repositorioOrdemServico;
    }

    @Override
    public void save(ServicoPrestado serviceProvided) {
        repositorioOrdemServico.save(toOrdemServico(serviceProvided));
    }

    @Override
    public List<ServicoPrestado> findAll() {
        return repositorioOrdemServico.findAll().stream()
                .filter(this::isServicoPrestado)
                .map(this::toServicoPrestado)
                .toList();
    }

    @Override
    public Optional<ServicoPrestado> findById(String id) {
        return repositorioOrdemServico.findById(id)
                .filter(this::isServicoPrestado)
                .map(this::toServicoPrestado);
    }

    private boolean isServicoPrestado(OrdemServico ordem) {
        return TIPO_SERVICO_PRESTADO.equalsIgnoreCase(ordem.tipoServico());
    }

    private OrdemServico toOrdemServico(ServicoPrestado servico) {
        LocalDateTime momento = servico.executionDate().atStartOfDay();
        List<OrdemServico.Anexo> anexos = new ArrayList<>();
        if (servico.signaturePath() != null && !servico.signaturePath().isBlank()) {
            anexos.add(new OrdemServico.Anexo(
                    null,
                    OrdemServico.TipoAnexo.ASSINATURA,
                    "assinatura",
                    servico.signaturePath(),
                    null,
                    0L,
                    "Assinatura do servico prestado",
                    servico.employeeId()
            ));
        }
        for (ServicoPrestado.Attachment anexo : servico.attachments()) {
            anexos.add(new OrdemServico.Anexo(
                    null,
                    OrdemServico.TipoAnexo.OUTRO,
                    anexo.name(),
                    anexo.storagePath(),
                    anexo.contentType(),
                    0L,
                    null,
                    servico.employeeId()
            ));
        }
        return new OrdemServico(
                servico.id(),
                null,
                servico.customerId(),
                servico.contractId() == null ? "" : servico.contractId(),
                servico.description(),
                servico.description(),
                TIPO_SERVICO_PRESTADO,
                statusOrdem(servico.serviceStatus()),
                momento,
                null,
                momento,
                servico.employeeId(),
                servico.employeeId(),
                servico.serviceStatus() == ServicoPrestado.ServiceStatus.COMPLETED,
                servico.paymentStatus() == ServicoPrestado.PaymentStatus.PAID,
                servico.amountCharged(),
                servico.signatureType() != ServicoPrestado.SignatureType.NONE,
                List.of(),
                anexos,
                servico.notes()
        );
    }

    private ServicoPrestado toServicoPrestado(OrdemServico ordem) {
        String assinaturaPath = null;
        List<ServicoPrestado.Attachment> anexos = new ArrayList<>();
        for (OrdemServico.Anexo anexo : ordem.anexos()) {
            if (anexo.tipo() == OrdemServico.TipoAnexo.ASSINATURA) {
                assinaturaPath = anexo.caminhoStorage();
            } else {
                anexos.add(new ServicoPrestado.Attachment(
                        anexo.nomeArquivo(),
                        anexo.caminhoStorage(),
                        anexo.mimeType() == null || anexo.mimeType().isBlank() ? "application/octet-stream" : anexo.mimeType()
                ));
            }
        }
        LocalDate dataExecucao = ordem.dataFim() != null ? ordem.dataFim().toLocalDate()
                : ordem.dataAgendada() != null ? ordem.dataAgendada().toLocalDate()
                : LocalDate.now();
        return new ServicoPrestado(
                ordem.id(),
                ordem.clienteId(),
                ordem.contratoId() == null || ordem.contratoId().isBlank() ? null : ordem.contratoId(),
                null,
                ordem.executadoPorId() == null || ordem.executadoPorId().isBlank() ? "-" : ordem.executadoPorId(),
                dataExecucao,
                ordem.titulo(),
                ordem.valorServico() == null ? BigDecimal.ZERO : ordem.valorServico(),
                ordem.pago() ? ServicoPrestado.PaymentStatus.PAID : ServicoPrestado.PaymentStatus.PENDING,
                ordem.assinaturaCliente() ? ServicoPrestado.SignatureType.MANUAL : ServicoPrestado.SignatureType.NONE,
                assinaturaPath,
                anexos,
                statusServico(ordem.status()),
                ServicoPrestado.ServicePriority.NORMAL,
                ordem.observacoes()
        );
    }

    private OrdemServico.OrdemServicoStatus statusOrdem(ServicoPrestado.ServiceStatus status) {
        return switch (status) {
            case IN_PROGRESS -> OrdemServico.OrdemServicoStatus.EM_ANDAMENTO;
            case COMPLETED -> OrdemServico.OrdemServicoStatus.CONCLUIDA;
            case CANCELLED -> OrdemServico.OrdemServicoStatus.CANCELADA;
            default -> OrdemServico.OrdemServicoStatus.AGENDADA;
        };
    }

    private ServicoPrestado.ServiceStatus statusServico(OrdemServico.OrdemServicoStatus status) {
        return switch (status) {
            case EM_ANDAMENTO -> ServicoPrestado.ServiceStatus.IN_PROGRESS;
            case CONCLUIDA -> ServicoPrestado.ServiceStatus.COMPLETED;
            case CANCELADA -> ServicoPrestado.ServiceStatus.CANCELLED;
            default -> ServicoPrestado.ServiceStatus.SCHEDULED;
        };
    }
}
