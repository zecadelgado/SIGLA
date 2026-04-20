package br.com.sigla.interfacegrafica.consulta;

import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.estoque.porta.entrada.CasoDeUsoEstoque;
import br.com.sigla.aplicacao.funcionarios.porta.entrada.CasoDeUsoFuncionario;
import br.com.sigla.interfacegrafica.modelo.OpcaoId;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ServicoConsultaReferencias {

    private final CasoDeUsoCliente casoDeUsoCliente;
    private final CasoDeUsoFuncionario casoDeUsoFuncionario;
    private final CasoDeUsoEstoque casoDeUsoEstoque;
    private final ServicoConsultaOrdemServico servicoConsultaOrdemServico;

    public ServicoConsultaReferencias(
            CasoDeUsoCliente casoDeUsoCliente,
            CasoDeUsoFuncionario casoDeUsoFuncionario,
            CasoDeUsoEstoque casoDeUsoEstoque,
            ServicoConsultaOrdemServico servicoConsultaOrdemServico
    ) {
        this.casoDeUsoCliente = casoDeUsoCliente;
        this.casoDeUsoFuncionario = casoDeUsoFuncionario;
        this.casoDeUsoEstoque = casoDeUsoEstoque;
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
                .map(item -> new OpcaoId(item.id(), item.name()))
                .toList();
    }

    public List<OpcaoId> ordensServico() {
        return servicoConsultaOrdemServico.listAll().stream()
                .map(order -> new OpcaoId(order.id(), order.id() + " - " + order.customerName()))
                .toList();
    }
}
