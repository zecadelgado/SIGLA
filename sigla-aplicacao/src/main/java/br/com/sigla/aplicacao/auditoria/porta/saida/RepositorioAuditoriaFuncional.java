package br.com.sigla.aplicacao.auditoria.porta.saida;

import br.com.sigla.dominio.auditoria.EventoAuditoria;

import java.util.List;

public interface RepositorioAuditoriaFuncional {

    void save(EventoAuditoria evento);

    List<EventoAuditoria> findByEntidade(String entidadeTipo, String entidadeId);
}
