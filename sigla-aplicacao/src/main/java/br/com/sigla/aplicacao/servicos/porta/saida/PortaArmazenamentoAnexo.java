package br.com.sigla.aplicacao.servicos.porta.saida;

public interface PortaArmazenamentoAnexo {

    String store(String folder, String fileName, byte[] payload);
}

