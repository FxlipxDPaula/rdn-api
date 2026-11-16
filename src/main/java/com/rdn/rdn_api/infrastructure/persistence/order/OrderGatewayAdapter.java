package com.rdn.rdn_api.infrastructure.persistence.order;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rdn.rdn_api.application.order.OrderGateway;
import com.rdn.rdn_api.domain.Order;
import com.rdn.rdn_api.domain.OrderItem;
import com.rdn.rdn_api.domain.OrderStatus;
import com.rdn.rdn_api.domain.SalesChannel;
import com.rdn.rdn_api.infrastructure.persistence.entity.OrderItemJpaEntity;
import com.rdn.rdn_api.infrastructure.persistence.entity.OrderJpaEntity;
import com.rdn.rdn_api.infrastructure.persistence.mapper.OrderItemMapper;
import com.rdn.rdn_api.infrastructure.persistence.mapper.OrderMapper;
import com.rdn.rdn_api.infrastructure.repository.BranchJpaRepository;
import com.rdn.rdn_api.infrastructure.repository.MenuItemJpaRepository;
import com.rdn.rdn_api.infrastructure.repository.OrderJpaRepository;
import com.rdn.rdn_api.infrastructure.repository.PromotionJpaRepository;
import com.rdn.rdn_api.infrastructure.repository.UserJpaRepository;

/**
 * Adaptador de {@link OrderGateway} sobre Spring Data JPA.
 */
@Component
public class OrderGatewayAdapter implements OrderGateway {

    private final OrderJpaRepository orderRepository;

    private final UserJpaRepository userRepository;

    private final BranchJpaRepository branchRepository;

    private final MenuItemJpaRepository menuItemRepository;

    private final PromotionJpaRepository promotionRepository;

    public OrderGatewayAdapter(OrderJpaRepository orderRepository, UserJpaRepository userRepository,
            BranchJpaRepository branchRepository, MenuItemJpaRepository menuItemRepository,
            PromotionJpaRepository promotionRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.menuItemRepository = menuItemRepository;
        this.promotionRepository = promotionRepository;
    }

    @Override
    @Transactional
    public Order save(Order order) {
        if (order.getId() == null) {
            return insert(order);
        }

        return update(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findWithItemsById(UUID id) {
        return orderRepository.findWithItemsById(id).map(OrderMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Order> search(SalesChannel canalPedido, OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<OrderJpaEntity> result;
        if (canalPedido != null && status != null) {
            result = orderRepository.findByOriginChannelAndStatus(canalPedido, status, pageable);
        } else if (canalPedido != null) {
            result = orderRepository.findByOriginChannel(canalPedido, pageable);
        } else if (status != null) {
            result = orderRepository.findByStatus(status, pageable);
        } else {
            result = orderRepository.findAll(pageable);
        }

        return result.map(OrderMapper::toDomainShallow);
    }

    private Order insert(Order order) {
        OrderJpaEntity entity = OrderMapper.toEntity(order);
        entity.setBranch(branchRepository.getReferenceById(order.getBranch().getId()));
        if (order.getCustomer() != null) {
            entity.setCustomer(userRepository.getReferenceById(order.getCustomer().getId()));
        }
        if (order.getPromotion() != null) {
            entity.setPromotion(promotionRepository.getReferenceById(order.getPromotion().getId()));
        }

        for (OrderItem item : order.getItems()) {
            OrderItemJpaEntity itemEntity = OrderItemMapper.toEntity(item);
            itemEntity.setOrder(entity);
            itemEntity.setMenuItem(menuItemRepository.getReferenceById(item.getMenuItem().getId()));
            entity.getItems().add(itemEntity);
        }

        return OrderMapper.toDomain(orderRepository.saveAndFlush(entity));
    }

    private Order update(Order order) {
        OrderJpaEntity entity = orderRepository.findWithItemsById(order.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "pedido " + order.getId() + " nao encontrado na atualizacao"));

        OrderMapper.copyScalarsToEntity(order, entity);

        return OrderMapper.toDomain(orderRepository.saveAndFlush(entity));
    }
}
