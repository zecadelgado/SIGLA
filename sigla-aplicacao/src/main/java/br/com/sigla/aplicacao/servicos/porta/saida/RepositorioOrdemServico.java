package br.com.sigla.aplicacao.servicos.porta.saida;

import br.com.sigla.dominio.servicos.OrdemServico;

import java.util.List;
import java.util.Optional;

public interface RepositorioOrdemServico {

    OrdemServico save(OrdemServico ordemServico);

    List<OrdemServico> findAll();

    Optional<OrdemServico> findById(String id);
}
