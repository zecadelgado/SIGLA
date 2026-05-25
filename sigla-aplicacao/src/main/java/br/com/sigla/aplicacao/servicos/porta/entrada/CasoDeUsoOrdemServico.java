package br.com.sigla.aplicacao.servicos.porta.entrada;

import br.com.sigla.dominio.servicos.OrdemServico;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface CasoDeUsoOrdemServico {

    OrdemServico create(CreateOrdemServicoCommand command);

    OrdemServico update(UpdateOrdemServicoCommand command);

    OrdemServico start(String id);

    OrdemServico conclude(ConcluirOrdemServicoCommand command);

    default OrdemServico conclude(String id) {
        return conclude(new ConcluirOrdemServicoCommand(id, "", null, false));
    }

    OrdemServico cancel(CancelarOrdemServicoCommand command);

    default OrdemServico cancel(String id) {
        return cancel(new CancelarOrdemServicoCommand(id, ""));
    }

    OrdemServico marcarPago(String id, boolean pago);

    OrdemServico adicionarProduto(AdicionarProdutoOrdemCommand command);

    OrdemServico anexar(AnexarOrdemServicoCommand command);

    List<OrdemServico> listAll();

    List<OrdemServico> filtrar(FiltroOrdemServico filtro);

    record CreateOrdemServicoCommand(
            String id,
            String clienteId,
            String contratoId,
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
        public CreateOrdemServicoCommand(
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
            this(id, clienteId, "", titulo, descricao, tipoServico, status, dataAgendada, dataInicio, dataFim, responsavelInternoId, executadoPorId, valorServico, observacoes);
        }
    }

    record UpdateOrdemServicoCommand(
            String id,
            String clienteId,
            String contratoId,
            String titulo,
            String descricao,
            String tipoServico,
            LocalDateTime dataAgendada,
            String responsavelInternoId,
            BigDecimal valorServico,
            String observacoes
    ) {
    }

    record ConcluirOrdemServicoCommand(
            String id,
            String executadoPorId,
            LocalDateTime dataFim,
            boolean assinaturaCliente
    ) {
    }

    record CancelarOrdemServicoCommand(
            String id,
            String motivo
    ) {
    }

    record AdicionarProdutoOrdemCommand(
            String ordemId,
            String id,
            String produtoId,
            int quantidade,
            BigDecimal valorUnitario
    ) {
    }

    record AnexarOrdemServicoCommand(
            String ordemId,
            String id,
            OrdemServico.TipoAnexo tipo,
            String nomeArquivo,
            String caminhoStorage,
            String mimeType,
            long tamanhoBytes,
            String descricao,
            String uploadedBy
    ) {
    }

    record FiltroOrdemServico(
            String texto,
            OrdemServico.OrdemServicoStatus status,
            LocalDateTime inicio,
            LocalDateTime fim,
            String clienteId,
            String responsavelId
    ) {
    }
}
