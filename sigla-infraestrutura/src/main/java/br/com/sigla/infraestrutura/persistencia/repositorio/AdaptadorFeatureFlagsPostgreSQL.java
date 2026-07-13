package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.configuracao.porta.saida.ProvedorFeatureFlags;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class AdaptadorFeatureFlagsPostgreSQL implements ProvedorFeatureFlags {

    private final EntityManager entityManager;

    public AdaptadorFeatureFlagsPostgreSQL(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean habilitada(String chave) {
        if (chave == null || chave.isBlank()) {
            return false;
        }
        return !entityManager.createNativeQuery(
                        "select 1 from sigla_feature_flags where chave = :chave and habilitada = true")
                .setParameter("chave", chave.trim())
                .setMaxResults(1)
                .getResultList()
                .isEmpty();
    }
}
