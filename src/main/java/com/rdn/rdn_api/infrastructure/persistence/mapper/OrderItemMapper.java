package com.rdn.rdn_api.infrastructure.persistence.mapper;

import com.rdn.rdn_api.domain.OrderItem;
import com.rdn.rdn_api.infrastructure.persistence.entity.OrderItemJpaEntity;

/**
 * Conversao entre {@link OrderItem} e {@link OrderItemJpaEntity}.
 *
 * <p>A referencia ao pedido existe apenas na entidade, porque o dominio nao
 * mantem navegacao de volta para o agregado.</p>
 */
public final class OrderItemMapper {

    private OrderItemMapper() {
    }

    public static OrderItem toDomain(OrderItemJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        OrderItem item = new OrderItem();
        item.setId(entity.getId());
        item.setMenuItem(MenuItemMapper.toDomain(entity.getMenuItem()));
        item.setQuantity(entity.getQuantity());
        item.setUnitPrice(entity.getUnitPrice());
        item.setSubtotal(entity.getSubtotal());
        item.setNotes(entity.getNotes());

        return item;
    }

    public static OrderItemJpaEntity toEntity(OrderItem domain) {
        if (domain == null) {
            return null;
        }

        OrderItemJpaEntity entity = new OrderItemJpaEntity();
        copyScalarsToEntity(domain, entity);

        return entity;
    }

    public static void copyScalarsToEntity(OrderItem domain, OrderItemJpaEntity entity) {
        entity.setQuantity(domain.getQuantity());
        entity.setUnitPrice(domain.getUnitPrice());
        entity.setSubtotal(domain.getSubtotal());
        entity.setNotes(domain.getNotes());
    }

}
