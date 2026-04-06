package br.com.sigla.aplicacao.contratos.porta.saida;

import br.com.sigla.dominio.contratos.Contrato;

import java.util.List;
import java.util.Optional;

public interface RepositorioContrato {

    void save(Contrato contract);

    List<Contrato> findAll();

    Optional<Contrato> findById(String id);
}

