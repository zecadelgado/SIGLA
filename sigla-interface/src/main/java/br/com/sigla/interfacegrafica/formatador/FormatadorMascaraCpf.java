package br.com.sigla.interfacegrafica.formatador;

import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

@Component
public class FormatadorMascaraCpf {

    public void aplicarCpf(TextField field) {
        aplicar(field, 11, this::cpf);
    }

    public void aplicarCnpj(TextField field) {
        aplicar(field, 14, this::cnpj);
    }

    public void aplicarTelefone(TextField field) {
        aplicar(field, 11, this::telefone);
    }

    String cpf(String value) {
        String digits = digits(value, 11);
        if (digits.length() <= 3) {
            return digits;
        }
        if (digits.length() <= 6) {
            return digits.substring(0, 3) + "." + digits.substring(3);
        }
        if (digits.length() <= 9) {
            return digits.substring(0, 3) + "." + digits.substring(3, 6) + "." + digits.substring(6);
        }
        return digits.substring(0, 3) + "." + digits.substring(3, 6) + "." + digits.substring(6, 9) + "-" + digits.substring(9);
    }

    String cnpj(String value) {
        String digits = digits(value, 14);
        if (digits.length() <= 2) {
            return digits;
        }
        if (digits.length() <= 5) {
            return digits.substring(0, 2) + "." + digits.substring(2);
        }
        if (digits.length() <= 8) {
            return digits.substring(0, 2) + "." + digits.substring(2, 5) + "." + digits.substring(5);
        }
        if (digits.length() <= 12) {
            return digits.substring(0, 2) + "." + digits.substring(2, 5) + "." + digits.substring(5, 8) + "/" + digits.substring(8);
        }
        return digits.substring(0, 2) + "." + digits.substring(2, 5) + "." + digits.substring(5, 8) + "/" + digits.substring(8, 12) + "-" + digits.substring(12);
    }

    String telefone(String value) {
        String digits = digits(value, 11);
        if (digits.length() <= 2) {
            return digits;
        }
        String ddd = digits.substring(0, 2);
        String numero = digits.substring(2);
        if (numero.length() <= 4) {
            return "(" + ddd + ") " + numero;
        }
        int prefixLength = digits.length() == 11 ? 5 : 4;
        prefixLength = Math.min(prefixLength, numero.length());
        String prefix = numero.substring(0, prefixLength);
        String suffix = numero.substring(prefixLength);
        return "(" + ddd + ") " + prefix + (suffix.isBlank() ? "" : "-" + suffix);
    }

    private void aplicar(TextField field, int maxDigits, java.util.function.Function<String, String> formatter) {
        if (field == null) {
            return;
        }
        boolean[] updating = {false};
        field.textProperty().addListener((observable, oldValue, newValue) -> {
            if (updating[0]) {
                return;
            }
            String formatted = formatter.apply(digits(newValue, maxDigits));
            if (!formatted.equals(newValue)) {
                updating[0] = true;
                field.setText(formatted);
                field.positionCaret(formatted.length());
                updating[0] = false;
            }
        });
        field.setText(formatter.apply(field.getText()));
        field.positionCaret(field.getText().length());
    }

    private String digits(String value, int maxDigits) {
        String digits = value == null ? "" : value.replaceAll("\\D", "");
        return digits.length() <= maxDigits ? digits : digits.substring(0, maxDigits);
    }
}

