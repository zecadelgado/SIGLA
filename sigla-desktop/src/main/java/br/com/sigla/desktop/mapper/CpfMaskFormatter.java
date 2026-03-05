package br.com.sigla.desktop.mapper;

import javafx.scene.control.TextFormatter;
import org.springframework.stereotype.Component;

@Component
public class CpfMaskFormatter {

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
