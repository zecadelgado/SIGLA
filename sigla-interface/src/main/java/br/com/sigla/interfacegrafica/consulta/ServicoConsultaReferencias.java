package br.com.sigla.interfacegrafica.consulta;

import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.contratos.porta.entrada.CasoDeUsoContrato;
import br.com.sigla.aplicacao.estoque.porta.entrada.CasoDeUsoEstoque;
import br.com.sigla.aplicacao.financeiro.porta.entrada.CasoDeUsoFinanceiro;
import br.com.sigla.aplicacao.funcionarios.porta.entrada.CasoDeUsoFuncionario;
import br.com.sigla.dominio.contratos.Contrato;
import br.com.sigla.dominio.funcionarios.Funcionario;
import br.com.sigla.interfacegrafica.modelo.OpcaoId;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Component
public class ServicoConsultaReferencias {

    private final CasoDeUsoCliente casoDeUsoCliente;
    private final CasoDeUsoFuncionario casoDeUsoFuncionario;
    private final CasoDeUsoEstoque casoDeUsoEstoque;
    private final CasoDeUsoContrato casoDeUsoContrato;
    private final CasoDeUsoFinanceiro casoDeUsoFinanceiro;
    private final ServicoConsultaOrdemServico servicoConsultaOrdemServico;

    public ServicoConsultaReferencias(
            CasoDeUsoCliente casoDeUsoCliente,
            CasoDeUsoFuncionario casoDeUsoFuncionario,
            CasoDeUsoEstoque casoDeUsoEstoque,
            CasoDeUsoContrato casoDeUsoContrato,
            CasoDeUsoFinanceiro casoDeUsoFinanceiro,
            ServicoConsultaOrdemServico servicoConsultaOrdemServico
    ) {
        this.casoDeUsoCliente = casoDeUsoCliente;
        this.casoDeUsoFuncionario = casoDeUsoFuncionario;
        this.casoDeUsoEstoque = casoDeUsoEstoque;
        this.casoDeUsoContrato = casoDeUsoContrato;
        this.casoDeUsoFinanceiro = casoDeUsoFinanceiro;
        this.servicoConsultaOrdemServico = servicoConsultaOrdemServico;
    }

    public List<OpcaoId> clientes() {
        return casoDeUsoCliente.listAll().stream()
                .filter(customer -> customer.ativo())
                .map(customer -> new OpcaoId(customer.id(), customer.name()))
                .sorted(Comparator.comparing(OpcaoId::label))
                .toList();
    }

    public List<OpcaoId> funcionarios() {
        return casoDeUsoFuncionario.listAll().stream()
                .filter(employee -> employee.status() == Funcionario.FuncionarioStatus.ACTIVE)
                .map(employee -> new OpcaoId(employee.id(), employee.name() + " - " + employee.role()))
                .sorted(Comparator.comparing(OpcaoId::label))
                .toList();
    }

    public List<OpcaoId> produtos() {
        return casoDeUsoEstoque.listAll().stream()
                .map(item -> new OpcaoId(item.id(), item.name()))
                .sorted(Comparator.comparing(OpcaoId::label))
                .toList();
    }

    public List<OpcaoId> contratos() {
        return casoDeUsoContrato.listAll().stream()
                .map(contract -> new OpcaoId(contract.id(), contratoLabel(contract)))
                .sorted(Comparator.comparing(OpcaoId::label))
                .toList();
    }

    public List<OpcaoId> contratosPorCliente(String clienteId) {
        return casoDeUsoContrato.listAll().stream()
                .filter(contract -> isSame(contract.customerId(), clienteId))
                .map(contract -> new OpcaoId(contract.id(), contratoLabel(contract)))
                .sorted(Comparator.comparing(OpcaoId::label))
                .toList();
    }

    public List<OpcaoId> ordensServico() {
        return servicoConsultaOrdemServico.listAll().stream()
                .map(order -> new OpcaoId(order.id(), order.numero() + " - " + order.customerName()))
                .sorted(Comparator.comparing(OpcaoId::label))
                .toList();
    }

    public List<OpcaoId> ordensServicoPorCliente(String clienteId) {
        return servicoConsultaOrdemServico.listAll().stream()
                .filter(order -> isSame(order.customerId(), clienteId))
                .map(order -> new OpcaoId(order.id(), order.numero() + " - " + order.customerName()))
                .sorted(Comparator.comparing(OpcaoId::label))
                .toList();
    }

    public List<OpcaoId> ordensServicoPorContrato(String contratoId) {
        return servicoConsultaOrdemServico.listAll().stream()
                .filter(order -> isSame(order.contractId(), contratoId))
                .map(order -> new OpcaoId(order.id(), order.numero() + " - " + order.customerName()))
                .sorted(Comparator.comparing(OpcaoId::label))
                .toList();
    }

    public List<OpcaoId> categoriasFinanceiras(CasoDeUsoFinanceiro.TransactionType tipo) {
        return casoDeUsoFinanceiro.listCategorias(tipo).stream()
                .map(reference -> new OpcaoId(reference.id(), reference.nome()))
                .sorted(Comparator.comparing(OpcaoId::label))
                .toList();
    }

    public List<OpcaoId> formasPagamento() {
        return casoDeUsoFinanceiro.listFormasPagamento().stream()
                .map(reference -> new OpcaoId(reference.id(), reference.nome()))
                .sorted(Comparator.comparing(OpcaoId::label))
                .toList();
    }

    public String clienteIdPorContrato(String contratoId) {
        return casoDeUsoContrato.listAll().stream()
                .filter(contract -> isSame(contract.id(), contratoId))
                .map(Contrato::customerId)
                .findFirst()
                .orElse("");
    }

    public String clienteIdPorOrdem(String ordemId) {
        return servicoConsultaOrdemServico.listAll().stream()
                .filter(order -> isSame(order.id(), ordemId))
                .map(ServicoConsultaOrdemServico.OrdemServicoView::customerId)
                .findFirst()
                .orElse("");
    }

    public String contratoIdPorOrdem(String ordemId) {
        return servicoConsultaOrdemServico.listAll().stream()
                .filter(order -> isSame(order.id(), ordemId))
                .map(ServicoConsultaOrdemServico.OrdemServicoView::contractId)
                .findFirst()
                .orElse("");
    }

    private String contratoLabel(Contrato contract) {
        String descricao = contract.descricao() == null || contract.descricao().isBlank() ? contract.type().name() : contract.descricao();
        String cliente = clientes().stream()
                .filter(option -> option.id().equals(contract.customerId()))
                .map(OpcaoId::label)
                .findFirst()
                .orElse(contract.customerId());
        return cliente + " - " + descricao + " - fim " + contract.endDate();
    }

    private boolean isSame(String left, String right) {
        return left != null && right != null && !right.isBlank() && Objects.equals(left, right);
    }
}
