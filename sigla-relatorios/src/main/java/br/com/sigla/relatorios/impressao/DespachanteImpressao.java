package br.com.sigla.relatorios.impressao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DespachanteImpressao {

    private static final Logger LOGGER = LoggerFactory.getLogger(DespachanteImpressao.class);

    public void print(String documentName, byte[] payload) {
        LOGGER.info("Print queued: {} ({} bytes)", documentName, payload.length);
    }
}

