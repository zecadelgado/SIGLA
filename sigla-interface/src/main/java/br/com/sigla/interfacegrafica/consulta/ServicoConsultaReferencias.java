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
<<<<<<< Updated upstream
=======
                .sorted(Comparator.comparing(customer -> customer.name().toLowerCase()))
>>>>>>> Stashed changes
                .map(customer -> new OpcaoId(customer.id(), customer.name()))
                .sorted(Comparator.comparing(OpcaoId::label))
                .toList();
    }

    public List<OpcaoId> funcionarios() {
        return casoDeUsoFuncionario.listAll().stream()
                .filter(employee -> employee.status() == Funcionario.FuncionarioStatus.ACTIVE)
<<<<<<< Updated upstream
=======
                .sorted(Comparator.comparing(employee -> employee.name().toLowerCase()))
>>>>>>> Stashed changes
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
<<<<<<< Updated upstream
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
=======
                .filter(order -> mesmoId(order.customerId(), clienteId))
                .map(order -> new OpcaoId(order.id(), order.numero() + " - " + order.customerName()))
                .toList();
    }

    public List<OpcaoId> contratos() {
        return casoDeUsoContrato.listAll().stream()
                .map(contrato -> new OpcaoId(contrato.id(), labelContrato(contrato)))
                .toList();
    }

    public List<OpcaoId> contratosPorCliente(String clienteId) {
        return casoDeUsoContrato.listAll().stream()
                .filter(contrato -> mesmoId(contrato.customerId(), clienteId))
                .map(contrato -> new OpcaoId(contrato.id(), labelContrato(contrato)))
                .toList();
    }

    public String clienteIdPorOrdem(String ordemId) {
        return servicoConsultaOrdemServico.findById(ordemId)
                .map(ServicoConsultaOrdemServico.OrdemServicoView::customerId)
                .orElse("");
    }

    public String contratoIdPorOrdem(String ordemId) {
        return servicoConsultaOrdemServico.findById(ordemId)
                .map(ServicoConsultaOrdemServico.OrdemServicoView::contractId)
                .orElse("");
>>>>>>> Stashed changes
    }

    public String clienteIdPorContrato(String contratoId) {
        return casoDeUsoContrato.listAll().stream()
<<<<<<< Updated upstream
                .filter(contract -> isSame(contract.id(), contratoId))
=======
                .filter(contrato -> mesmoId(contrato.id(), contratoId))
>>>>>>> Stashed changes
                .map(Contrato::customerId)
                .findFirst()
                .orElse("");
    }

<<<<<<< Updated upstream
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
=======
    public List<OpcaoId> categoriasFinanceiras(CasoDeUsoFinanceiro.TransactionType tipo) {
        String tipoBanco = tipo == CasoDeUsoFinanceiro.TransactionType.EXPENSE ? "EXPENSE" : "ENTRY";
        return casoDeUsoFinanceiro.listCategoriasAtivas().stream()
                .filter(categoria -> categoria.tipo().equalsIgnoreCase(tipoBanco))
                .map(categoria -> new OpcaoId(categoria.id(), categoria.nome()))
                .toList();
    }

    public List<OpcaoId> formasPagamento() {
        return casoDeUsoFinanceiro.listFormasPagamentoAtivas().stream()
                .map(forma -> new OpcaoId(forma.id(), forma.nome()))
                .toList();
    }

    private String labelContrato(Contrato contrato) {
        String descricao = contrato.description() == null || contrato.description().isBlank()
                ? contrato.type().name()
                : contrato.description();
        return descricao + " - " + contrato.status().name();
    }

    private boolean mesmoId(String left, String right) {
        return left != null && right != null && !right.isBlank() && left.equals(right);
>>>>>>> Stashed changes
    }
}
