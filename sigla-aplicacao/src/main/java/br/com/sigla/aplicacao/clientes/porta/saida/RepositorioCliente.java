package br.com.sigla.aplicacao.clientes.porta.saida;

import br.com.sigla.dominio.clientes.Cliente;

import java.util.List;
import java.util.Optional;

public interface RepositorioCliente {

    void save(Cliente customer);

    void deleteById(String id);

    List<Cliente> findAll();

    Optional<Cliente> findById(String id);

    boolean existsActiveCpf(String cpf, String exceptId);

    boolean existsActiveCnpj(String cnpj, String exceptId);

    boolean existsActiveEmail(String email, String exceptId);

    boolean hasLinkedRecords(String id);
}
