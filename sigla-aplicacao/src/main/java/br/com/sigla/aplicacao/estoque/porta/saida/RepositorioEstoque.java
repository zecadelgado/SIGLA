package br.com.sigla.aplicacao.estoque.porta.saida;

import br.com.sigla.dominio.estoque.ItemEstoque;

import java.util.List;
import java.util.Optional;

public interface RepositorioEstoque {

    void save(ItemEstoque item);

    /**
     * Anexa um movimento a razao imutavel de estoque. O saldo materializado do
     * produto e atualizado de forma atomica pela persistencia (trigger no
     * PostgreSQL), nunca por releitura/regravacao do agregado.
     */
    void registrarMovimento(ItemEstoque item, ItemEstoque.InventoryMovement movimento);

    List<ItemEstoque> findAll();

    Optional<ItemEstoque> findById(String id);

    boolean existsActiveSku(String sku, String exceptId);

    boolean existsMovementForOrder(String orderId);
}

