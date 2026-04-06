package br.com.sigla.interfacegrafica.apresentacao;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ApresentadorTexto {

    public String render(List<String> lines, String emptyState) {
        if (lines == null || lines.isEmpty()) {
            return emptyState;
        }
        return String.join("\n\n", lines);
    }
}

