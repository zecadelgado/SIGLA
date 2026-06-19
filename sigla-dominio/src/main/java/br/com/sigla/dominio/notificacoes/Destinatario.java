package br.com.sigla.dominio.notificacoes;

/**
 * Para quem uma configuracao de notificacao se destina.
 * AMBOS gera um registro para o cliente e outro para o funcionario.
 */
public enum Destinatario {
    CLIENTE,
    FUNCIONARIO,
    AMBOS
}
