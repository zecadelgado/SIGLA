package br.com.sigla.relatorios.recibo;

import br.com.sigla.relatorios.impressao.DespachanteImpressao;
import br.com.sigla.relatorios.modelo.ProvedorModeloRelatorio;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class ServicoRelatorioRecibo {

    private final ProvedorModeloRelatorio templateProvider;
    private final DespachanteImpressao printDispatcher;

    public ServicoRelatorioRecibo(ProvedorModeloRelatorio templateProvider, DespachanteImpressao printDispatcher) {
        this.templateProvider = templateProvider;
        this.printDispatcher = printDispatcher;
    }

    public byte[] generate(String receiptNumber) {
        String content = "RECEIPT " + receiptNumber + " template=" + templateProvider.modeloRecibo();
        return content.getBytes(StandardCharsets.UTF_8);
    }

    public void print(String receiptNumber) {
        printDispatcher.print("RECEIPT-" + receiptNumber, generate(receiptNumber));
    }
}

