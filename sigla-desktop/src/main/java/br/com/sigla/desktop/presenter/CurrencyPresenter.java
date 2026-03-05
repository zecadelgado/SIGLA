package br.com.sigla.desktop.presenter;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Component
public class CurrencyPresenter {

    private final NumberFormat numberFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public String format(BigDecimal value) {
        return numberFormat.format(value);
    }
}
