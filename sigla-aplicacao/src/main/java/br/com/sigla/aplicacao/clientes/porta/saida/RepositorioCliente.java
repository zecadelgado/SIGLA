package br.com.sigla.aplicacao.clientes.porta.saida;

import br.com.sigla.dominio.clientes.Cliente;

import java.util.List;
import java.util.Optional;

public interface RepositorioCliente {

    void save(Cliente customer);

    List<Cliente> findAll();

    Optional<Cliente> findById(String id);
}

