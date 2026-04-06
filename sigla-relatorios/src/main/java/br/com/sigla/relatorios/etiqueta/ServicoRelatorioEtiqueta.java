package br.com.sigla.relatorios.etiqueta;

import br.com.sigla.relatorios.impressao.DespachanteImpressao;
import br.com.sigla.relatorios.modelo.ProvedorModeloRelatorio;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class ServicoRelatorioEtiqueta {

    private final ProvedorModeloRelatorio templateProvider;
    private final DespachanteImpressao printDispatcher;

    public ServicoRelatorioEtiqueta(ProvedorModeloRelatorio templateProvider, DespachanteImpressao printDispatcher) {
        this.templateProvider = templateProvider;
        this.printDispatcher = printDispatcher;
    }

    public byte[] generate(String sku) {
        String content = "LABEL " + sku + " template=" + templateProvider.modeloEtiqueta();
        return content.getBytes(StandardCharsets.UTF_8);
    }

    public void print(String sku) {
        printDispatcher.print("LABEL-" + sku, generate(sku));
    }
}

