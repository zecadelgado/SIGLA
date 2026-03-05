package br.com.sigla.infrastructure.persistence.mapper;

import br.com.sigla.domain.inventory.InventoryItem;
import br.com.sigla.infrastructure.persistence.entity.InventoryItemEntity;
import org.springframework.stereotype.Component;

@Component
public class InventoryEntityMapper {

    public InventoryItemEntity toEntity(InventoryItem item) {
        InventoryItemEntity entity = new InventoryItemEntity();
        entity.setSku(item.sku());
        entity.setDescription(item.description());
        entity.setQuantity(item.quantity());
        return entity;
    }

    public InventoryItem toDomain(InventoryItemEntity entity) {
        return new InventoryItem(entity.getSku(), entity.getDescription(), entity.getQuantity());
    }
}
