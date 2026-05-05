package br.com.sigla.infraestrutura.seguranca;

import br.com.sigla.aplicacao.usuarios.porta.saida.ServicoSenhaUsuario;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class ServicoSenhaUsuarioBCrypt implements ServicoSenhaUsuario {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

    @Override
    public String hash(String senha) {
        if (senha == null || senha.isBlank()) {
            throw new IllegalArgumentException("Senha obrigatoria.");
        }
        return passwordEncoder.encode(senha);
    }

    @Override
    public boolean matches(String senha, String senhaHash) {
        if (senha == null || senha.isBlank() || senhaHash == null || senhaHash.isBlank()) {
            return false;
        }
        return passwordEncoder.matches(senha, senhaHash);
    }
}
