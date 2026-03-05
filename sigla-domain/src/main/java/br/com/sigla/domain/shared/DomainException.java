package br.com.sigla.domain.shared;

public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
