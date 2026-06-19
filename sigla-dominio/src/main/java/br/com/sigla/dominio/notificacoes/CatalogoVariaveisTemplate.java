package br.com.sigla.dominio.notificacoes;

import java.util.List;

/**
 * Variaveis disponiveis para uso nos templates de mensagem.
 * Exibidas na tela de configuracao e usadas como chaves do mapa de metadata.
 */
public enum CatalogoVariaveisTemplate {
    CLIENTE_NOME("cliente_nome", "Nome do cliente"),
    CLIENTE_TELEFONE("cliente_telefone", "Telefone do cliente"),
    FUNCIONARIO_NOME("funcionario_nome", "Nome do funcionario"),
    FUNCIONARIO_TELEFONE("funcionario_telefone", "Telefone do funcionario"),
    DATA_VISITA("data_visita", "Data da visita"),
    HORA_VISITA("hora_visita", "Hora da visita"),
    ENDERECO("endereco", "Endereco completo"),
    TIPO_SERVICO("tipo_servico", "Tipo de servico"),
    CONTRATO_VENCIMENTO("contrato_vencimento", "Data de vencimento do contrato"),
    CERTIFICADO_VENCIMENTO("certificado_vencimento", "Data de validade do certificado"),
    OBSERVACOES("observacoes", "Observacoes");

    private final String chave;
    private final String descricao;

    CatalogoVariaveisTemplate(String chave, String descricao) {
        this.chave = chave;
        this.descricao = descricao;
    }

    public String chave() {
        return chave;
    }

    public String descricao() {
        return descricao;
    }

    public String marcador() {
        return "{{" + chave + "}}";
    }

    public static List<CatalogoVariaveisTemplate> todas() {
        return List.of(values());
    }
}
