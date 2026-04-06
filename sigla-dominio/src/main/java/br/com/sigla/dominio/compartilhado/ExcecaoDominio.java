package br.com.sigla.dominio.compartilhado;

public class ExcecaoDominio extends RuntimeException {

    public ExcecaoDominio(String message) {
        super(message);
    }
}

