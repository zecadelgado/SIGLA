package br.com.sigla.aplicacao.servicos.porta.entrada;

import br.com.sigla.dominio.servicos.OrdemServico;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface CasoDeUsoOrdemServico {

    OrdemServico create(CreateOrdemServicoCommand command);

    OrdemServico conclude(String id);

    OrdemServico cancel(String id);

    List<OrdemServico> listAll();

    record CreateOrdemServicoCommand(
            String id,
            String clienteId,
            String titulo,
            String descricao,
            String tipoServico,
            OrdemServico.OrdemServicoStatus status,
            LocalDateTime dataAgendada,
            LocalDateTime dataInicio,
            LocalDateTime dataFim,
            String responsavelInternoId,
            String executadoPorId,
            BigDecimal valorServico,
            String observacoes
    ) {
    }
}
