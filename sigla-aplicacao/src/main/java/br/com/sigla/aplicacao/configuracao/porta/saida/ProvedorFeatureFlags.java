package br.com.sigla.aplicacao.configuracao.porta.saida;

public interface ProvedorFeatureFlags {

    String MATERIALIZAR_OCORRENCIAS_CONTRATUAIS = "materializar_ocorrencias_contratuais";

    boolean habilitada(String chave);
}
