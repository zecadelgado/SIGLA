package br.com.sigla.interfacegrafica.consulta;

import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.servicos.porta.entrada.CasoDeUsoOrdemServico;
import br.com.sigla.dominio.servicos.OrdemServico;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ServicoConsultaOrdemServico {

    private final CasoDeUsoOrdemServico casoDeUsoOrdemServico;
    private final CasoDeUsoCliente casoDeUsoCliente;

    public ServicoConsultaOrdemServico(
            CasoDeUsoOrdemServico casoDeUsoOrdemServico,
            CasoDeUsoCliente casoDeUsoCliente
    ) {
        this.casoDeUsoOrdemServico = casoDeUsoOrdemServico;
        this.casoDeUsoCliente = casoDeUsoCliente;
    }

    public List<OrdemServicoView> listAll() {
        Map<String, String> clientes = casoDeUsoCliente.listAll().stream()
                .collect(Collectors.toMap(customer -> customer.id(), customer -> customer.name()));
        return casoDeUsoOrdemServico.listAll().stream()
                .map(order -> toView(order, clientes))
                .sorted((left, right) -> right.emissionDate().compareTo(left.emissionDate()))
                .toList();
    }

    public OrdemServicoResumo summary() {
        List<OrdemServicoView> orders = listAll();
        long abertas = orders.stream().filter(order -> "ABERTA".equals(order.status()) || "AGENDADA".equals(order.status())).count();
        long emAndamento = orders.stream().filter(order -> "EM_ANDAMENTO".equals(order.status())).count();
        long concluidas = orders.stream().filter(order -> "CONCLUIDA".equals(order.status())).count();
        BigDecimal faturamento = orders.stream()
                .map(OrdemServicoView::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OrdemServicoResumo((int) abertas, (int) emAndamento, (int) concluidas, faturamento);
    }

    private OrdemServicoView toView(OrdemServico order, Map<String, String> clientes) {
        return new OrdemServicoView(
                order.id(),
                order.numeroOs() == null ? order.id() : String.valueOf(order.numeroOs()),
                order.titulo(),
                order.descricao(),
                clientes.getOrDefault(order.clienteId(), order.clienteId()),
                blankAsDash(order.responsavelInternoId()),
                order.dataAgendada() == null ? LocalDate.now() : order.dataAgendada().toLocalDate(),
                order.status().name(),
                order.totalGeral(),
                order.clienteId(),
                order.contratoId(),
                order.tipoServico(),
                order.dataInicio(),
                order.dataFim(),
                blankAsDash(order.observacoes()),
                order.foiFeito(),
                order.pago(),
                order.assinaturaCliente(),
                order.totalProdutos(),
                order.produtos().size(),
                order.anexos().size()
        );
    }

    public record OrdemServicoView(
            String id,
            String numero,
            String title,
            String description,
            String customerName,
            String responsible,
            LocalDate emissionDate,
            String status,
            BigDecimal amount,
            String customerId,
            String contractId,
            String serviceType,
            java.time.LocalDateTime startAt,
            java.time.LocalDateTime endAt,
            String notes,
            boolean done,
            boolean paid,
            boolean signed,
            BigDecimal productTotal,
            int productCount,
            int attachmentCount
    ) {
    }

    public record OrdemServicoResumo(
            int abertas,
            int emAndamento,
            int concluidas,
            BigDecimal faturamento
    ) {
    }

    private String blankAsDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
