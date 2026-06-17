package br.com.sigla.interfacegrafica.apresentacao;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Component
public class ApresentadorMoeda {

    private final NumberFormat numberFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public String format(BigDecimal value) {
        // NumberFormat.format(null) lanca excecao; trata valor ausente como zero
        // para nunca quebrar a renderizacao de tabelas/labels.
        return numberFormat.format(value == null ? BigDecimal.ZERO : value);
    }
}

