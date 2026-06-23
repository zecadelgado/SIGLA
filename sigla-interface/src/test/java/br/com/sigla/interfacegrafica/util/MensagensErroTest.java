package br.com.sigla.interfacegrafica.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MensagensErroTest {

    @Test
    void diferenciaFkDeInsercaoDeFkDeExclusao() {
        IllegalStateException inserirPerfil = new IllegalStateException(
                "ERROR: insert or update on table \"usuarios\" violates foreign key constraint \"fk_usuarios_auth_user\"");
        assertEquals(
                "Nao foi possivel criar o perfil local porque o usuario do Supabase Auth nao foi encontrado. Tente novamente.",
                MensagensErro.descrever(inserirPerfil));

        IllegalStateException excluirVinculado = new IllegalStateException(
                "ERROR: update or delete on table \"usuarios\" violates foreign key constraint \"fk_movimentacoes_usuario\"");
        assertEquals(
                "Operacao bloqueada: este registro esta vinculado a outros. Use inativacao em vez de exclusao.",
                semAcentos(MensagensErro.descrever(excluirVinculado)));
    }

    private String semAcentos(String texto) {
        return texto
                .replace("ç", "c")
                .replace("ã", "a")
                .replace("á", "a")
                .replace("í", "i")
                .replace("é", "e")
                .replace("ó", "o");
    }
}
