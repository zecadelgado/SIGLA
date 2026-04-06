package br.com.sigla.interfacegrafica.formatador;

import javafx.scene.control.TextFormatter;
import org.springframework.stereotype.Component;

@Component
public class FormatadorMascaraCpf {

    public TextFormatter<String> formatter() {
        return new TextFormatter<>(change -> {
            String text = change.getControlNewText().replaceAll("\\D", "");
            if (text.length() > 11) {
                return null;
            }
            return change;
        });
    }
}

