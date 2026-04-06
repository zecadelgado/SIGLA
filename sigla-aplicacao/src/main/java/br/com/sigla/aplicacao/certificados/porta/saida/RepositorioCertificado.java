package br.com.sigla.aplicacao.certificados.porta.saida;

import br.com.sigla.dominio.certificados.Certificado;

import java.util.List;
import java.util.Optional;

public interface RepositorioCertificado {

    void save(Certificado certificate);

    List<Certificado> findAll();

    Optional<Certificado> findById(String id);
}

