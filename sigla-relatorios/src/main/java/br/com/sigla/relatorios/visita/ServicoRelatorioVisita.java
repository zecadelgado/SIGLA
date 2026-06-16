package br.com.sigla.relatorios.visita;

import br.com.sigla.relatorios.formulario.DadosEmpresa;
import br.com.sigla.relatorios.formulario.FormularioVisita;
import br.com.sigla.relatorios.impressao.DespachanteImpressao;
import br.com.sigla.relatorios.modelo.ProvedorModeloRelatorio;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

/**
 * Gera o Relatorio de Visita no formulario LIDER (PDF) e o envia para impressao.
 */
@Service
public class ServicoRelatorioVisita {

    private final ProvedorModeloRelatorio modelo;
    private final DespachanteImpressao despachante;

    public ServicoRelatorioVisita(ProvedorModeloRelatorio modelo, DespachanteImpressao despachante) {
        this.modelo = modelo;
        this.despachante = despachante;
    }

    public byte[] gerar(FormularioVisita.Dados dados) {
        return FormularioVisita.gerar(dados, empresa());
    }

    public Path imprimir(FormularioVisita.Dados dados) {
        return despachante.imprimir("relatorio-visita", gerar(dados));
    }

    private DadosEmpresa empresa() {
        return new DadosEmpresa(
                modelo.nomeEmpresa(),
                modelo.subtituloEmpresa(),
                modelo.razaoSocial(),
                modelo.cnpjEmpresa(),
                modelo.telefonesEmpresa(),
                modelo.enderecoEmpresa());
    }
}
