package br.com.sigla.infraestrutura.persistencia.conversor;

import br.com.sigla.dominio.servicos.OrdemServico;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class OrdemServicoStatusConverter implements AttributeConverter<OrdemServico.OrdemServicoStatus, String> {

    @Override
    public String convertToDatabaseColumn(OrdemServico.OrdemServicoStatus status) {
        return status == null ? OrdemServico.OrdemServicoStatus.AGENDADA.name() : status.name();
    }

    @Override
    public OrdemServico.OrdemServicoStatus convertToEntityAttribute(String valor) {
        if (valor == null || valor.isBlank()) {
            return OrdemServico.OrdemServicoStatus.AGENDADA;
        }
        return switch (valor.trim().toUpperCase()) {
            case "SCHEDULED", "AGENDADO", "AGENDADA" -> OrdemServico.OrdemServicoStatus.AGENDADA;
            case "OPEN", "ABERTO", "ABERTA" -> OrdemServico.OrdemServicoStatus.ABERTA;
            case "IN_PROGRESS", "EM_ANDAMENTO" -> OrdemServico.OrdemServicoStatus.EM_ANDAMENTO;
            case "COMPLETED", "CONCLUIDO", "CONCLUIDA" -> OrdemServico.OrdemServicoStatus.CONCLUIDA;
            case "CANCELLED", "CANCELADO", "CANCELADA" -> OrdemServico.OrdemServicoStatus.CANCELADA;
            case "OVERDUE", "ATRASADO", "ATRASADA" -> OrdemServico.OrdemServicoStatus.ATRASADA;
            default -> OrdemServico.OrdemServicoStatus.valueOf(valor.trim().toUpperCase());
        };
    }
}
