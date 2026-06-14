package br.com.sigla.aplicacao.usuarios.porta.saida;

public interface ServicoSenhaUsuario {

    String hash(String senha);

    boolean matches(String senha, String senhaHash);
}
