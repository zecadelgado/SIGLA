package br.com.sigla.interfacegrafica.consulta;

import org.springframework.stereotype.Component;

/**
 * Carrega o id da Ordem de Servico que sera editada da tela de listagem para a
 * tela (reutilizada) de criacao. A mesma tela serve para criar e editar, garantindo
 * que os campos disponiveis sejam sempre identicos. E consumido uma unica vez no
 * {@code initialize} da tela: ao abrir "Nova Ordem" o contexto fica limpo (modo
 * criacao); ao abrir "Editar" guarda o id (modo edicao).
 */
@Component
public class ContextoEdicaoOrdemServico {

    private String ordemServicoId;

    /** Marca o modo edicao para a OS informada. */
    public void editar(String ordemServicoId) {
        this.ordemServicoId = ordemServicoId == null || ordemServicoId.isBlank() ? null : ordemServicoId;
    }

    /** Volta ao modo criacao (nenhuma OS em edicao). */
    public void limpar() {
        this.ordemServicoId = null;
    }

    public boolean emEdicao() {
        return ordemServicoId != null && !ordemServicoId.isBlank();
    }

    public String ordemServicoId() {
        return ordemServicoId;
    }
}
