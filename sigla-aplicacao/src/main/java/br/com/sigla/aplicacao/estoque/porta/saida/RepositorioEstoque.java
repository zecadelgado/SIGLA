package br.com.sigla.aplicacao.estoque.porta.saida;

import br.com.sigla.dominio.estoque.ItemEstoque;

import java.util.List;
import java.util.Optional;

public interface RepositorioEstoque {

    void save(ItemEstoque item);

    List<ItemEstoque> findAll();

    Optional<ItemEstoque> findById(String id);
}

