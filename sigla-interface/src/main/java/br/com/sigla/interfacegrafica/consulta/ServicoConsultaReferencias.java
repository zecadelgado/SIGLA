package br.com.sigla.interfacegrafica.consulta;

import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.contratos.porta.entrada.CasoDeUsoContrato;
import br.com.sigla.aplicacao.estoque.porta.entrada.CasoDeUsoEstoque;
import br.com.sigla.aplicacao.financeiro.porta.entrada.CasoDeUsoFinanceiro;
import br.com.sigla.aplicacao.funcionarios.porta.entrada.CasoDeUsoFuncionario;
import br.com.sigla.dominio.contratos.Contrato;
import br.com.sigla.interfacegrafica.modelo.OpcaoId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                .map(customer -> new OpcaoId(customer.id(), customer.name()))
                .toList();
    }

    public List<OpcaoId> funcionarios() {
        return casoDeUsoFuncionario.listAll().stream()
                .map(employee -> new OpcaoId(employee.id(), employee.name() + " - " + employee.role()))
                .toList();
    }

    public List<OpcaoId> produtos() {
        return casoDeUsoEstoque.listAll().stream()
                .filter(item -> item.ativo())
                .map(item -> new OpcaoId(item.id(), item.name()))
                .toList();
    }

    public List<OpcaoId> ordensServico() {
        return servicoConsultaOrdemServico.listAll().stream()
                .map(order -> new OpcaoId(order.id(), order.numero() + " - " + order.customerName()))
                .toList();
    }

    public List<OpcaoId> contratos() {
        Map<String, String> clientes = casoDeUsoCliente.listAll().stream()
                .collect(Collectors.toMap(customer -> customer.id(), customer -> customer.name(), (left, right) -> left));
        return casoDeUsoContrato.listAll().stream()
                .filter(contrato -> contrato.status() == Contrato.ContratoStatus.ACTIVE || contrato.status() == Contrato.ContratoStatus.DRAFT)
                .map(contrato -> new OpcaoId(contrato.id(), contratoLabel(contrato, clientes)))
                .toList();
    }

    public List<OpcaoId> contratosDoCliente(String clienteId) {
        if (clienteId == null || clienteId.isBlank()) {
            return contratos();
        }
        return contratos().stream()
                .filter(opcao -> casoDeUsoContrato.listAll().stream()
                        .anyMatch(contrato -> contrato.id().equals(opcao.id()) && clienteId.equals(contrato.customerId())))
                .toList();
    }

    public List<OpcaoId> ordensServicoDoCliente(String clienteId) {
        if (clienteId == null || clienteId.isBlank()) {
            return ordensServico();
        }
        return servicoConsultaOrdemServico.listAll().stream()
                .filter(order -> clienteId.equals(order.customerId()))
                .map(order -> new OpcaoId(order.id(), order.numero() + " - " + order.customerName()))
                .toList();
    }

    public List<OpcaoId> categoriasFinanceiras() {
        return casoDeUsoFinanceiro.listCategoriasAtivas().stream()
                .map(categoria -> new OpcaoId(categoria.id(), categoria.nome()))
                .toList();
    }

    public List<OpcaoId> formasPagamento() {
        return casoDeUsoFinanceiro.listFormasPagamentoAtivas().stream()
                .map(forma -> new OpcaoId(forma.id(), forma.nome()))
                .toList();
    }

    private String contratoLabel(Contrato contrato, Map<String, String> clientes) {
        String descricao = contrato.description() == null || contrato.description().isBlank()
                ? contrato.type().name()
                : contrato.description();
        return clientes.getOrDefault(contrato.customerId(), contrato.customerId()) + " - " + descricao;
    }
}
