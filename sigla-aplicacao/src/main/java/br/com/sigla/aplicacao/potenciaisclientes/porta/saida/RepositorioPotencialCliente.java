package br.com.sigla.aplicacao.potenciaisclientes.porta.saida;

import br.com.sigla.dominio.potenciaisclientes.PotencialCliente;

import java.util.List;
import java.util.Optional;

public interface RepositorioPotencialCliente {

    void save(PotencialCliente lead);

    List<PotencialCliente> findAll();

    Optional<PotencialCliente> findById(String id);

    Optional<PotencialCliente> findConvertedByClienteId(String clienteId);
}
