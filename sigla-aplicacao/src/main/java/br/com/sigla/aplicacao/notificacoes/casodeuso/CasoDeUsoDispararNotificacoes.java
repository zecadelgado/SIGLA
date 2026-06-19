package br.com.sigla.aplicacao.notificacoes.casodeuso;

import br.com.sigla.aplicacao.notificacoes.porta.entrada.CasoDeUsoEnvioNotificacao;
import br.com.sigla.aplicacao.notificacoes.porta.saida.PortaEnvioWhatsapp;
import br.com.sigla.aplicacao.notificacoes.porta.saida.PortaEnvioWhatsapp.PayloadNotificacaoWhatsapp;
import br.com.sigla.aplicacao.notificacoes.porta.saida.PortaEnvioWhatsapp.ResultadoEnvio;
import br.com.sigla.aplicacao.notificacoes.porta.saida.RepositorioNotificacao;
import br.com.sigla.dominio.notificacoes.Notificacao;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class CasoDeUsoDispararNotificacoes implements CasoDeUsoEnvioNotificacao {

    private final RepositorioNotificacao repositorio;
    private final PortaEnvioWhatsapp envioWhatsapp;

    public CasoDeUsoDispararNotificacoes(RepositorioNotificacao repositorio, PortaEnvioWhatsapp envioWhatsapp) {
        this.repositorio = repositorio;
        this.envioWhatsapp = envioWhatsapp;
    }

    @Override
    public int dispatchDue(LocalDateTime momento) {
        int enviados = 0;
        for (Notificacao notificacao : repositorio.findDuePending(momento)) {
            if (enviarEAtualizar(notificacao)) {
                enviados++;
            }
        }
        return enviados;
    }

    @Override
    public int reprocessarFalhas() {
        List<Notificacao> falhas = repositorio.findAll().stream()
                .filter(notificacao -> notificacao.status() == Notificacao.NotificacaoStatus.FAILED)
                .toList();
        int enviados = 0;
        for (Notificacao notificacao : falhas) {
            if (enviarEAtualizar(notificacao)) {
                enviados++;
            }
        }
        return enviados;
    }

    @Override
    public List<Notificacao> listar() {
        return repositorio.findAll().stream()
                .sorted(Comparator.comparing(Notificacao::momentoDisparo).reversed()
                        .thenComparing(Notificacao::title))
                .toList();
    }

    /** Envia uma notificacao e persiste o novo estado. Retorna true se foi enviada (ou simulada). */
    private boolean enviarEAtualizar(Notificacao notificacao) {
        ResultadoEnvio resultado = envioWhatsapp.enviar(PayloadNotificacaoWhatsapp.de(notificacao));
        switch (resultado.situacao()) {
            case ENVIADO, SIMULADO -> {
                repositorio.save(notificacao.marcarEnviada(LocalDateTime.now()));
                return true;
            }
            case FALHA -> repositorio.save(notificacao.registrarFalha(resultado.detalhe()));
            case DESABILITADO -> {
                // Envio global desligado: mantem PENDING para reenviar quando habilitar.
            }
        }
        return false;
    }
}
