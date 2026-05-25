package br.com.sigla.aplicacao.auditoria.casodeuso;

import br.com.sigla.aplicacao.auditoria.porta.saida.RepositorioAuditoriaFuncional;
import br.com.sigla.dominio.auditoria.EventoAuditoria;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ServicoAuditoriaFuncional {

    private final RepositorioAuditoriaFuncional repository;

    public ServicoAuditoriaFuncional(RepositorioAuditoriaFuncional repository) {
        this.repository = repository;
    }

    public void registrar(String entidadeTipo, String entidadeId, String acao, String detalhe, String usuarioId) {
        repository.save(new EventoAuditoria(
                UUID.randomUUID().toString(),
                entidadeTipo,
                entidadeId,
                acao,
                detalhe,
                usuarioId,
                LocalDateTime.now()
        ));
    }
}
