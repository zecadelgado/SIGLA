package br.com.sigla.relatorios.formulario;

/** Dados de marca/cabecalho usados nos formularios impressos. */
public record DadosEmpresa(
        String nome,
        String subtitulo,
        String razaoSocial,
        String cnpj,
        String telefones,
        String endereco
) {
}
