package br.com.sigla.aplicacao.servicos.porta.saida;

import br.com.sigla.dominio.servicos.OrdemServico;

import java.util.List;
import java.util.Optional;

public interface RepositorioOrdemServico {

    OrdemServico save(OrdemServico ordemServico);

    /**
     * Desvincula uma OS contratual e grava a auditoria da operacao de forma
     * atomica. A implementacao PostgreSQL deve usar exclusivamente a RPC
     * administrativa protegida pelo trigger do banco.
     */
    default OrdemServico desvincularContratoAdministrativamente(String id, String motivo, String usuarioId) {
        throw new UnsupportedOperationException(
                "Desvinculacao administrativa exige persistencia com auditoria atomica.");
    }

    List<OrdemServico> findAll();

    Optional<OrdemServico> findById(String id);
}
