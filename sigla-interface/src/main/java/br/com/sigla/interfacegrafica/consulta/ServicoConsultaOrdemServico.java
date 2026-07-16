package br.com.sigla.interfacegrafica.consulta;

import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.funcionarios.porta.entrada.CasoDeUsoFuncionario;
import br.com.sigla.aplicacao.financeiro.porta.entrada.CasoDeUsoFinanceiro;
import br.com.sigla.dominio.financeiro.LancamentoFinanceiro;
import br.com.sigla.aplicacao.servicos.porta.entrada.CasoDeUsoOrdemServico;
import br.com.sigla.dominio.funcionarios.Funcionario;
import br.com.sigla.dominio.servicos.OrdemServico;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ServicoConsultaOrdemServico {

    private final CasoDeUsoOrdemServico casoDeUsoOrdemServico;
    private final CasoDeUsoCliente casoDeUsoCliente;
    private final CasoDeUsoFuncionario casoDeUsoFuncionario;
    private final CasoDeUsoFinanceiro casoDeUsoFinanceiro;

    @Autowired
    public ServicoConsultaOrdemServico(
            CasoDeUsoOrdemServico casoDeUsoOrdemServico,
            CasoDeUsoCliente casoDeUsoCliente,
            CasoDeUsoFuncionario casoDeUsoFuncionario,
            CasoDeUsoFinanceiro casoDeUsoFinanceiro
    ) {
        this.casoDeUsoOrdemServico = casoDeUsoOrdemServico;
        this.casoDeUsoCliente = casoDeUsoCliente;
        this.casoDeUsoFuncionario = casoDeUsoFuncionario;
        this.casoDeUsoFinanceiro = casoDeUsoFinanceiro;
    }

    /** Compatibilidade para consultas sem o modulo financeiro (testes e telas isoladas). */
    public ServicoConsultaOrdemServico(
            CasoDeUsoOrdemServico casoDeUsoOrdemServico,
            CasoDeUsoCliente casoDeUsoCliente,
            CasoDeUsoFuncionario casoDeUsoFuncionario
    ) {
        this(casoDeUsoOrdemServico, casoDeUsoCliente, casoDeUsoFuncionario, null);
    }

    public List<OrdemServicoView> listAll() {
        Map<String, String> clientes = casoDeUsoCliente.listAll().stream()
                .collect(Collectors.toMap(customer -> customer.id(), customer -> customer.name(), (left, right) -> left));
        Map<String, String> funcionarios = casoDeUsoFuncionario.listAll().stream()
                .collect(Collectors.toMap(Funcionario::id, Funcionario::name, (left, right) -> left));
        return casoDeUsoOrdemServico.listAll().stream()
                .map(order -> toView(order, clientes, funcionarios))
                .sorted(Comparator.comparing(
                        OrdemServicoView::emissionDate,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();
    }

    public List<OrdemServicoView> listByDate(LocalDate date) {
        if (date == null) {
            return List.of();
        }
        return listAll().stream()
                .filter(order -> date.equals(order.emissionDate()))
                .toList();
    }

    public Optional<OrdemServicoView> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return listAll().stream()
                .filter(order -> order.id().equals(id))
                .findFirst();
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

    private OrdemServicoView toView(OrdemServico order, Map<String, String> clientes, Map<String, String> funcionarios) {
        String responsavelId = order.responsavelInternoId() == null ? "" : order.responsavelInternoId();
        String responsavelNome = responsavelId.isBlank() ? "-" : funcionarios.getOrDefault(responsavelId, "-");
        String statusFinanceiro = casoDeUsoFinanceiro == null ? (order.pago() ? "Pago" : "Nao faturada")
                : casoDeUsoFinanceiro.buscarLancamentoPorOrdemServico(order.id())
                .map(lancamento -> statusFinanceiro(lancamento))
                .orElse("Nao faturada");
        return new OrdemServicoView(
                order.id(),
                order.numeroOs() == null ? order.id() : String.valueOf(order.numeroOs()),
                order.titulo(),
                order.descricao(),
                clientes.getOrDefault(order.clienteId(), "-"),
                responsavelNome,
                responsavelId,
                order.dataAgendada() == null ? null : order.dataAgendada().toLocalDate(),
                order.status().name(),
                order.totalGeral(),
                order.clienteId(),
                order.contratoId(),
                order.tipoServico(),
                order.dataInicio(),
                order.dataFim(),
                blankAsDash(order.observacoes()),
                order.foiFeito(),
                "Pago".equals(statusFinanceiro),
                order.assinaturaCliente(),
                order.totalProdutos(),
                order.produtos().size(),
                order.anexos().size(),
                statusFinanceiro
        );
    }

    public record OrdemServicoView(
            String id,
            String numero,
            String title,
            String description,
            String customerName,
            String responsible,
            String responsibleId,
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
            int attachmentCount,
            String financialStatus
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

    private String statusFinanceiro(LancamentoFinanceiro lancamento) {
        if (lancamento.status() != LancamentoFinanceiro.Status.PAID
                && lancamento.status() != LancamentoFinanceiro.Status.CANCELLED
                && (lancamento.vencido(LocalDate.now())
                || lancamento.parcelas().stream().anyMatch(parcela -> parcela.vencida(LocalDate.now())))) {
            return "Vencido";
        }
        return switch (lancamento.status()) {
            case PAID -> "Pago";
            case PARTIAL -> "Parcial";
            case CANCELLED -> "Cancelado";
            case OVERDUE -> "Vencido";
            case PENDING -> "Pendente";
        };
    }
}
