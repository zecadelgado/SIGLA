package br.com.sigla.interfacegrafica.formatador;

import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Mascara de moeda (estilo centavos) para campos de valor: conforme o usuario
 * digita, o conteudo e reformatado para {@code R$ 1.234,56}. Os digitos sao
 * interpretados como centavos, entao "5000" vira {@code R$ 50,00}.
 *
 * <p>Segue o mesmo padrao de {@link FormatadorMascaraCpf}: um listener em
 * {@code textProperty} reformata a cada alteracao. {@link #valor(TextField)} le
 * o valor digitado como {@link BigDecimal} em reais, tolerando campo vazio,
 * simbolo de moeda e separadores.
 */
@Component
public class FormatadorMascaraMoeda {

    private static final Locale PT_BR = new Locale("pt", "BR");
    private static final int MAX_DIGITOS = 13;

    /** Instala a mascara de moeda no campo. */
    public void aplicar(TextField field) {
        if (field == null) {
            return;
        }
        boolean[] updating = {false};
        field.textProperty().addListener((observable, oldValue, newValue) -> {
            if (updating[0]) {
                return;
            }
            String formatted = moeda(newValue);
            if (!formatted.equals(newValue)) {
                updating[0] = true;
                field.setText(formatted);
                field.positionCaret(formatted.length());
                updating[0] = false;
            }
        });
        field.setText(moeda(field.getText()));
        field.positionCaret(field.getText().length());
    }

    /** Preenche o campo com um valor inicial ja formatado. */
    public void definir(TextField field, BigDecimal valor) {
        if (field == null) {
            return;
        }
        field.setText(valor == null || valor.signum() == 0 ? "" : formatar(valor));
        field.positionCaret(field.getText().length());
    }

    /** Le o valor digitado no campo como BigDecimal (em reais). */
    public BigDecimal valor(TextField field) {
        return field == null ? BigDecimal.ZERO : valor(field.getText());
    }

    BigDecimal valor(String texto) {
        String digits = digits(texto);
        if (digits.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(digits).movePointLeft(2);
    }

    String moeda(String value) {
        String digits = digits(value);
        if (digits.isEmpty()) {
            return "";
        }
        return formatar(new BigDecimal(digits).movePointLeft(2));
    }

    private String formatar(BigDecimal valor) {
        BigDecimal seguro = (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
        DecimalFormat format = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(PT_BR));
        return "R$ " + format.format(seguro);
    }

    private String digits(String value) {
        String digits = value == null ? "" : value.replaceAll("\\D", "");
        return digits.length() <= MAX_DIGITOS ? digits : digits.substring(0, MAX_DIGITOS);
    }
}
