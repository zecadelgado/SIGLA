package br.com.sigla.interfacegrafica.consulta;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ContextoDetalheOrdemServico {

    private LocalDate dataSelecionada;
    private String ordemServicoId;

    public void selecionarData(LocalDate dataSelecionada) {
        this.dataSelecionada = dataSelecionada;
        this.ordemServicoId = "";
    }

    public void selecionarOrdem(LocalDate dataSelecionada, String ordemServicoId) {
        this.dataSelecionada = dataSelecionada;
        this.ordemServicoId = ordemServicoId == null ? "" : ordemServicoId;
    }

    public LocalDate dataSelecionada() {
        return dataSelecionada;
    }

    public String ordemServicoId() {
        return ordemServicoId;
    }
}
