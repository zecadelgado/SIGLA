package br.com.sigla.interfacegrafica.consulta;

import br.com.sigla.aplicacao.agenda.porta.entrada.CasoDeUsoAgenda;
import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.financeiro.porta.entrada.CasoDeUsoFinanceiro;
import br.com.sigla.aplicacao.funcionarios.porta.entrada.CasoDeUsoFuncionario;
import br.com.sigla.aplicacao.servicos.porta.entrada.CasoDeUsoServicoPrestado;
import br.com.sigla.dominio.agenda.VisitaAgendada;
import br.com.sigla.dominio.servicos.ServicoPrestado;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ServicoConsultaOrdemServico {

    private final CasoDeUsoAgenda casoDeUsoAgenda;
    private final CasoDeUsoCliente casoDeUsoCliente;
    private final CasoDeUsoServicoPrestado casoDeUsoServicoPrestado;
    private final CasoDeUsoFinanceiro casoDeUsoFinanceiro;
    private final CasoDeUsoFuncionario casoDeUsoFuncionario;

    public ServicoConsultaOrdemServico(
            CasoDeUsoAgenda casoDeUsoAgenda,
            CasoDeUsoCliente casoDeUsoCliente,
            CasoDeUsoServicoPrestado casoDeUsoServicoPrestado,
            CasoDeUsoFinanceiro casoDeUsoFinanceiro,
            CasoDeUsoFuncionario casoDeUsoFuncionario
    ) {
        this.casoDeUsoAgenda = casoDeUsoAgenda;
        this.casoDeUsoCliente = casoDeUsoCliente;
        this.casoDeUsoServicoPrestado = casoDeUsoServicoPrestado;
        this.casoDeUsoFinanceiro = casoDeUsoFinanceiro;
        this.casoDeUsoFuncionario = casoDeUsoFuncionario;
    }

    public List<OrdemServicoView> listAll() {
        Map<String, String> clientes = casoDeUsoCliente.listAll().stream()
                .collect(Collectors.toMap(customer -> customer.id(), customer -> customer.name()));
        Map<String, String> funcionarios = casoDeUsoFuncionario.listAll().stream()
                .collect(Collectors.toMap(employee -> employee.id(), employee -> employee.name()));
        Map<String, List<ServicoPrestado>> servicosPorAgenda = casoDeUsoServicoPrestado.listAll().stream()
                .filter(service -> service.scheduleId() != null && !service.scheduleId().isBlank())
                .collect(Collectors.groupingBy(ServicoPrestado::scheduleId));
        Map<String, BigDecimal> faturamentoPorOrdem = casoDeUsoFinanceiro.listTransactions().stream()
                .filter(transaction -> transaction.orderReference() != null && !transaction.orderReference().isBlank())
                .filter(transaction -> transaction.type() == CasoDeUsoFinanceiro.TransactionType.ENTRY)
                .collect(Collectors.groupingBy(
                        CasoDeUsoFinanceiro.TransacaoFinanceiraView::orderReference,
                        Collectors.reducing(BigDecimal.ZERO, CasoDeUsoFinanceiro.TransacaoFinanceiraView::amount, BigDecimal::add)
                ));

        return casoDeUsoAgenda.listAll().stream()
                .map(schedule -> toView(schedule, clientes, funcionarios, servicosPorAgenda.getOrDefault(schedule.id(), List.of()), faturamentoPorOrdem))
                .sorted((left, right) -> right.emissionDate().compareTo(left.emissionDate()))
                .toList();
    }

    public OrdemServicoResumo summary() {
        List<OrdemServicoView> orders = listAll();
        long abertas = orders.stream().filter(order -> "ABERTA".equals(order.status())).count();
        long emAndamento = orders.stream().filter(order -> "EM_ANDAMENTO".equals(order.status())).count();
        long concluidas = orders.stream().filter(order -> "CONCLUIDA".equals(order.status())).count();
        BigDecimal faturamento = orders.stream()
                .map(OrdemServicoView::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OrdemServicoResumo((int) abertas, (int) emAndamento, (int) concluidas, faturamento);
    }

    private OrdemServicoView toView(
            VisitaAgendada schedule,
            Map<String, String> clientes,
            Map<String, String> funcionarios,
            List<ServicoPrestado> servicos,
            Map<String, BigDecimal> faturamentoPorOrdem
    ) {
        ServicoPrestado primeiroServico = servicos.stream().findFirst().orElse(null);
        String responsible = schedule.internalResponsible();
        if ((responsible == null || responsible.isBlank()) && primeiroServico != null) {
            responsible = funcionarios.getOrDefault(primeiroServico.employeeId(), primeiroServico.employeeId());
        }

        BigDecimal amount = faturamentoPorOrdem.getOrDefault(
                schedule.id(),
                servicos.stream().map(ServicoPrestado::amountCharged).reduce(BigDecimal.ZERO, BigDecimal::add)
        );
        String title = safeText(schedule.title(), schedule.id());
        return new OrdemServicoView(
                schedule.id(),
                title,
                clientes.getOrDefault(schedule.customerId(), schedule.customerId()),
                responsible == null || responsible.isBlank() ? "-" : responsible,
                schedule.scheduledDate(),
                mapStatus(schedule, primeiroServico),
                amount,
                schedule.customerId(),
                schedule.contractId(),
                safeText(schedule.serviceType(), "-"),
                schedule.startAt(),
                schedule.endAt(),
                safeText(schedule.notes(), "")
        );
    }

    private String mapStatus(VisitaAgendada schedule, ServicoPrestado service) {
        if (service != null) {
            return switch (service.serviceStatus()) {
                case IN_PROGRESS -> "EM_ANDAMENTO";
                case COMPLETED -> "CONCLUIDA";
                case CANCELLED -> "CANCELADA";
                default -> "ABERTA";
            };
        }

        return switch (schedule.status()) {
            case IN_PROGRESS -> "EM_ANDAMENTO";
            case COMPLETED -> "CONCLUIDA";
            case CANCELLED -> "CANCELADA";
            case MISSED -> "ATRASADA";
            default -> "ABERTA";
        };
    }

    public record OrdemServicoView(
            String id,
            String title,
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
            String notes
    ) {
    }

    public record OrdemServicoResumo(
            int abertas,
            int emAndamento,
            int concluidas,
            BigDecimal faturamento
    ) {
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
