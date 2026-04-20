package br.com.sigla.interfacegrafica.modelo;

public record OpcaoId(String id, String label) {

    @Override
    public String toString() {
        return label;
    }
}
