package com.rdn.rdn_api.infrastructure.persistence.mapper;

import java.util.ArrayList;
import java.util.List;

import com.rdn.rdn_api.domain.Order;
import com.rdn.rdn_api.domain.OrderItem;
import com.rdn.rdn_api.infrastructure.persistence.entity.OrderItemJpaEntity;
import com.rdn.rdn_api.infrastructure.persistence.entity.OrderJpaEntity;

/**
 * Conversao entre {@link Order} e {@link OrderJpaEntity}.
 *
 * <p>Cliente e filial sao montados como referencias (a filial sem cardapio) para
 * limitar o custo da leitura e evitar carregar a arvore inteira a cada consulta
 * de pedido.</p>
 */
public final class OrderMapper {

    private OrderMapper() {
    }

    public static Order toDomain(OrderJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        Order order = new Order();
        order.setId(entity.getId());
        order.setCustomer(UserMapper.toDomain(entity.getCustomer()));
        order.setBranch(BranchMapper.toDomainShallow(entity.getBranch()));
        order.setOriginChannel(entity.getOriginChannel());
        order.setStatus(entity.getStatus());
        order.setTotalAmount(entity.getTotalAmount());
        order.setDiscountAmount(entity.getDiscountAmount());
        order.setCreatedAt(entity.getCreatedAt());
        order.setCancellationReason(entity.getCancellationReason());
        order.setIdempotencyKey(entity.getIdempotencyKey());

        List<OrderItem> items = new ArrayList<>();
        for (OrderItemJpaEntity item : entity.getItems()) {
            items.add(OrderItemMapper.toDomain(item));
        }
        order.setItems(items);

        return order;
    }

    public static void copyScalarsToEntity(Order domain, OrderJpaEntity entity) {
        entity.setId(domain.getId());
        entity.setOriginChannel(domain.getOriginChannel());
        entity.setStatus(domain.getStatus());
        entity.setTotalAmount(domain.getTotalAmount());
        entity.setDiscountAmount(domain.getDiscountAmount());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setCancellationReason(domain.getCancellationReason());
        entity.setIdempotencyKey(domain.getIdempotencyKey());
    }

}
