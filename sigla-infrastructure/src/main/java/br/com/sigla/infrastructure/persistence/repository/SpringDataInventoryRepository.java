package br.com.sigla.infrastructure.persistence.repository;

import br.com.sigla.infrastructure.persistence.entity.InventoryItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataInventoryRepository extends JpaRepository<InventoryItemEntity, String> {
}
