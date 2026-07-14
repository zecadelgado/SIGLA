package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.contratos.porta.saida.PortaFaturamentoMensalidades;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public class AdaptadorPortaFaturamentoMensalidades implements PortaFaturamentoMensalidades {

    private final EntityManager entityManager;

    public AdaptadorPortaFaturamentoMensalidades(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public List<ResultadoMensalidade> faturar(LocalDate dataReferencia, String chaveExecucao) {
        List<Object[]> linhas = entityManager.createNativeQuery(
                        "select contrato_id, contrato_vigencia_id, competencia, lancamento_id, resultado "
                                + "from rpc_faturar_mensalidades_v1(:dataReferencia, :chaveExecucao)")
                .setParameter("dataReferencia", dataReferencia)
                .setParameter("chaveExecucao", chaveExecucao)
                .getResultList();
        return linhas.stream()
                .map(linha -> new ResultadoMensalidade(
                        texto(linha[0]),
                        texto(linha[1]),
                        data(linha[2]),
                        texto(linha[3]),
                        texto(linha[4])))
                .toList();
    }

    private String texto(Object valor) {
        if (valor == null) {
            return "";
        }
        if (valor instanceof UUID uuid) {
            return uuid.toString();
        }
        return valor.toString();
    }

    private LocalDate data(Object valor) {
        if (valor instanceof LocalDate localDate) {
            return localDate;
        }
        if (valor instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return null;
    }
}

@Repository
@Profile("memoria")
class InMemoryAdaptadorPortaFaturamentoMensalidades implements PortaFaturamentoMensalidades {

    @Override
    public List<ResultadoMensalidade> faturar(LocalDate dataReferencia, String chaveExecucao) {
        // Perfil de memoria nao possui a autoridade PostgreSQL: nada e faturado.
        return List.of();
    }
}
