package com.rdn.rdn_api.application.order;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rdn.rdn_api.application.auth.AuthenticatedUser;
import com.rdn.rdn_api.application.order.exception.OperationNotAllowedException;
import com.rdn.rdn_api.application.order.exception.ResourceNotFoundException;
import com.rdn.rdn_api.domain.Order;
import com.rdn.rdn_api.domain.OrderStatus;
import com.rdn.rdn_api.domain.SalesChannel;
import com.rdn.rdn_api.domain.UserRole;

/**
 * Caso de uso de consulta de pedidos.
 */
@Service
public class QueryOrderService {

    private static final int MAX_PAGE_SIZE = 100;

    private final OrderGateway orderGateway;

    public QueryOrderService(OrderGateway orderGateway) {
        this.orderGateway = orderGateway;
    }

    /**
     * Lista pedidos paginados com filtros opcionais de canal e status.
     *
     * @param canalPedido canal de origem (opcional)
     * @param status      status (opcional)
     * @param page        pagina solicitada (base zero)
     * @param size        tamanho solicitado
     * @return pagina de pedidos sem itens
     */
    @Transactional(readOnly = true)
    public Page<Order> list(SalesChannel canalPedido, OrderStatus status, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        return orderGateway.search(canalPedido, status, safePage, safeSize);
    }

    /**
     * Busca o pedido completo, aplicando a regra de dono para {@code CUSTOMER}.
     *
     * @param id        identificador do pedido
     * @param principal usuario autenticado
     * @return pedido com itens
     * @throws ResourceNotFoundException    quando o pedido nao existe
     * @throws OperationNotAllowedException quando o cliente consulta pedido alheio
     */
    @Transactional(readOnly = true)
    public Order findById(UUID id, AuthenticatedUser principal) {
        Order order = orderGateway.findWithItemsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));

        boolean isStaff = principal != null && UserRole.CUSTOMER != principal.role();
        boolean isOwner = principal != null && order.getCustomer() != null
                && principal.id().equals(order.getCustomer().getId());

        if (!isStaff && !isOwner) {
            throw new OperationNotAllowedException("Pedido pertence a outro cliente.");
        }

        return order;
    }
}
