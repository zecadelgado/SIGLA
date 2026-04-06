package br.com.sigla.aplicacao.funcionarios.porta.saida;

import br.com.sigla.dominio.funcionarios.Funcionario;

import java.util.List;
import java.util.Optional;

public interface RepositorioFuncionario {

    void save(Funcionario employee);

    List<Funcionario> findAll();

    Optional<Funcionario> findById(String id);
}

