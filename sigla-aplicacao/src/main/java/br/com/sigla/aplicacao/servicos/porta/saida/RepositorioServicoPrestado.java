package br.com.sigla.aplicacao.servicos.porta.saida;

import br.com.sigla.dominio.servicos.ServicoPrestado;

import java.util.List;
import java.util.Optional;

public interface RepositorioServicoPrestado {

    void save(ServicoPrestado serviceProvided);

    List<ServicoPrestado> findAll();

    Optional<ServicoPrestado> findById(String id);
}

