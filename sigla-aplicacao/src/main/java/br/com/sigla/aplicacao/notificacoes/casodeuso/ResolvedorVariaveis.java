package br.com.sigla.aplicacao.notificacoes.casodeuso;

import br.com.sigla.dominio.agenda.VisitaAgendada;
import br.com.sigla.dominio.certificados.Certificado;
import br.com.sigla.dominio.clientes.Cliente;
import br.com.sigla.dominio.contratos.Contrato;
import br.com.sigla.dominio.funcionarios.Funcionario;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/** Monta o mapa de variaveis ({@code {{chave}}}) de cada tipo de evento para os templates. */
final class ResolvedorVariaveis {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private ResolvedorVariaveis() {
    }

    static Map<String, String> visita(VisitaAgendada visita, Cliente cliente, Funcionario funcionario) {
        Map<String, String> variaveis = base(cliente, funcionario);
        variaveis.put("data_visita", visita.scheduledDate().format(DATA));
        if (visita.startAt() != null && !visita.allDay()) {
            variaveis.put("hora_visita", visita.startAt().format(HORA));
        }
        variaveis.put("tipo_servico", !visita.serviceType().isBlank() ? visita.serviceType() : visita.title());
        variaveis.put("observacoes", visita.notes());
        return variaveis;
    }

    static Map<String, String> contrato(Contrato contrato, Cliente cliente) {
        Map<String, String> variaveis = base(cliente, null);
        variaveis.put("contrato_vencimento", contrato.endDate().format(DATA));
        variaveis.put("tipo_servico", !contrato.description().isBlank() ? contrato.description() : contrato.type().name());
        variaveis.put("observacoes", contrato.notes());
        return variaveis;
    }

    static Map<String, String> certificado(Certificado certificado, Cliente cliente) {
        Map<String, String> variaveis = base(cliente, null);
        variaveis.put("certificado_vencimento", certificado.validUntil().format(DATA));
        variaveis.put("tipo_servico", certificado.description());
        variaveis.put("observacoes", certificado.notes());
        return variaveis;
    }

    private static Map<String, String> base(Cliente cliente, Funcionario funcionario) {
        Map<String, String> variaveis = new LinkedHashMap<>();
        if (cliente != null) {
            variaveis.put("cliente_nome", cliente.name());
            variaveis.put("cliente_telefone", cliente.phone());
            variaveis.put("cliente_email", cliente.email());
            variaveis.put("endereco", cliente.location());
        }
        if (funcionario != null) {
            variaveis.put("funcionario_nome", funcionario.name());
            variaveis.put("funcionario_telefone", funcionario.telefone());
        }
        return variaveis;
    }
}
