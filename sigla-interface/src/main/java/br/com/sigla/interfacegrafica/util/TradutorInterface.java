package br.com.sigla.interfacegrafica.util;

import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.util.StringConverter;

public final class TradutorInterface {

    private TradutorInterface() {
    }

    public static <T> void aplicar(ComboBox<T> comboBox) {
        if (comboBox == null) {
            return;
        }
        comboBox.setConverter(converter());
        comboBox.setCellFactory(listView -> celula());
        comboBox.setButtonCell(celula());
    }

    public static <T> StringConverter<T> converter() {
        return new StringConverter<>() {
            @Override
            public String toString(T valor) {
                return texto(valor);
            }

            @Override
            public T fromString(String string) {
                return null;
            }
        };
    }

    public static String texto(Object valor) {
        if (valor == null) {
            return "";
        }
        if (valor instanceof Boolean booleano) {
            return Boolean.TRUE.equals(booleano) ? "Sim" : "Não";
        }
        if (valor instanceof Enum<?> enumValor) {
            return textoNome(enumValor.name());
        }
        if (valor instanceof String texto) {
            return textoNome(texto);
        }
        return valor.toString();
    }

    private static <T> ListCell<T> celula() {
        return new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : texto(item));
            }
        };
    }

    private static String textoNome(String nome) {
        if (nome == null || nome.isBlank()) {
            return "";
        }
        return switch (nome.trim().toUpperCase()) {
            case "CLIENTE" -> "Cliente";
            case "FUNCIONARIO" -> "Funcionário";
            case "PESSOA_FISICA" -> "Pessoa física";
            case "PESSOA_JURIDICA" -> "Pessoa jurídica";
            case "ACTIVE" -> "Ativo";
            case "INACTIVE" -> "Inativo";
            case "ON_LEAVE" -> "Afastado";
            case "ENTRY" -> "Entrada";
            case "EXPENSE" -> "Despesa";
            case "PENDING" -> "Pendente";
            case "PAID" -> "Pago";
            case "PARTIALLY_PAID" -> "Parcialmente pago";
            case "OVERDUE" -> "Vencido";
            case "CANCELLED" -> "Cancelado";
            case "RECEIVED" -> "Recebido";
            case "SCHEDULED" -> "Agendado";
            case "IN_PROGRESS" -> "Em andamento";
            case "COMPLETED" -> "Concluído";
            case "MISSED" -> "Não realizado";
            case "NONE" -> "Nenhuma";
            case "MONTHLY" -> "Mensal";
            case "BIWEEKLY" -> "Quinzenal";
            case "ONE_OFF" -> "Avulso";
            case "NORMAL" -> "Normal";
            case "LOW" -> "Baixa";
            case "HIGH" -> "Alta";
            case "URGENT" -> "Urgente";
            case "MANUAL" -> "Manual";
            case "REPLACED" -> "Substituído";
            case "SERVICOS" -> "Serviços";
            case "CONTRATOS" -> "Contratos";
            case "COMBUSTIVEL" -> "Combustível";
            case "PRODUTOS" -> "Produtos";
            case "ALIMENTACAO" -> "Alimentação";
            case "EXTRAS" -> "Extras";
            case "VISITA_MENSAL" -> "Visita mensal";
            case "VISITA_QUINZENAL" -> "Visita quinzenal";
            case "SERVICO_AVULSO" -> "Serviço avulso";
            case "SIM" -> "Sim";
            case "NAO" -> "Não";
            case "TODOS" -> "Todos";
            case "NOVO" -> "Novo";
            case "CONTATADO" -> "Contatado";
            case "AGUARDANDO_RETORNO" -> "Aguardando retorno";
            case "CONVERTIDO" -> "Convertido";
            case "PERDIDO" -> "Perdido";
            case "CANCELADO", "CANCELADA" -> "Cancelado";
            case "ABERTA" -> "Aberta";
            case "AGENDADA" -> "Agendada";
            case "EM_ANDAMENTO" -> "Em andamento";
            case "CONCLUIDA" -> "Concluída";
            case "ATRASADA" -> "Atrasada";
            case "INBOUND", "ENTRADA" -> "Entrada";
            case "OUTBOUND", "SAIDA" -> "Saída";
            case "AJUSTE" -> "Ajuste";
            case "COMPRA" -> "Compra";
            case "USO_OS" -> "Uso em OS";
            case "ASSINATURA" -> "Assinatura";
            case "COMPROVANTE" -> "Comprovante";
            case "FOTO_SERVICO" -> "Foto do serviço";
            case "DOCUMENTO" -> "Documento";
            case "OUTRO" -> "Outro";
            default -> nome;
        };
    }
}
