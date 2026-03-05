package br.com.sigla.reporting.printer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PrintDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrintDispatcher.class);

    public void print(String documentName, byte[] payload) {
        LOGGER.info("Print queued: {} ({} bytes)", documentName, payload.length);
    }
}
