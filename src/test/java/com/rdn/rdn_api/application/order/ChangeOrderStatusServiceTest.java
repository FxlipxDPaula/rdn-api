package com.rdn.rdn_api.application.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.rdn.rdn_api.application.audit.AuditGateway;
import com.rdn.rdn_api.application.auth.AuthenticatedUser;
import com.rdn.rdn_api.application.order.exception.InvalidStatusTransitionException;
import com.rdn.rdn_api.application.order.exception.OperationNotAllowedException;
import com.rdn.rdn_api.application.order.exception.ResourceNotFoundException;
import com.rdn.rdn_api.domain.Order;
import com.rdn.rdn_api.domain.OrderStatus;
import com.rdn.rdn_api.domain.SalesChannel;
import com.rdn.rdn_api.domain.User;
import com.rdn.rdn_api.domain.UserRole;

/**
 * Testes unitarios do caso de uso de mudanca de status.
 */
class ChangeOrderStatusServiceTest {

    private static final UUID PEDIDO_ID = UUID.randomUUID();

    private static final UUID OWNER_ID = UUID.randomUUID();

    private final OrderGateway orderGateway = mock(OrderGateway.class);

    private final AuditGateway auditGateway = mock(AuditGateway.class);

    private ChangeOrderStatusService service;

    @BeforeEach
    void setUp() {
        service = new ChangeOrderStatusService(orderGateway, auditGateway);
        when(orderGateway.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void staffAvancaPedidoRecebidoParaPreparo() {
        when(orderGateway.findWithItemsById(PEDIDO_ID)).thenReturn(Optional.of(order(OrderStatus.RECEIVED)));

        Order updated = service.changeStatus(PEDIDO_ID,
                new ChangeStatusCommand(OrderStatus.PREPARING, null), staffPrincipal());

        assertThat(updated.getStatus()).isEqualTo(OrderStatus.PREPARING);
    }

    @Test
    void recusaTransicaoInvalidaComConflitoDeNegocio() {
        when(orderGateway.findWithItemsById(PEDIDO_ID)).thenReturn(Optional.of(order(OrderStatus.RECEIVED)));

        assertThatThrownBy(() -> service.changeStatus(PEDIDO_ID,
                new ChangeStatusCommand(OrderStatus.DELIVERED, null), staffPrincipal()))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void clienteCancelaOProprioPedidoRegistrandoMotivo() {
        when(orderGateway.findWithItemsById(PEDIDO_ID)).thenReturn(Optional.of(order(OrderStatus.RECEIVED)));

        Order updated = service.changeStatus(PEDIDO_ID,
                new ChangeStatusCommand(OrderStatus.CANCELLED, "desisti"), customerPrincipal());

        assertThat(updated.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(updated.getCancellationReason()).isEqualTo("desisti");
    }

    @Test
    void clienteNaoPodeAvancarStatus() {
        when(orderGateway.findWithItemsById(PEDIDO_ID)).thenReturn(Optional.of(order(OrderStatus.RECEIVED)));

        assertThatThrownBy(() -> service.changeStatus(PEDIDO_ID,
                new ChangeStatusCommand(OrderStatus.PREPARING, null), customerPrincipal()))
                .isInstanceOf(OperationNotAllowedException.class);
    }

    @Test
    void pedidoInexistenteRetornaRecursoNaoEncontrado() {
        when(orderGateway.findWithItemsById(PEDIDO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeStatus(PEDIDO_ID,
                new ChangeStatusCommand(OrderStatus.PREPARING, null), staffPrincipal()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static Order order(OrderStatus status) {
        User owner = new User();
        owner.setId(OWNER_ID);
        owner.setFullName("Cliente Teste");
        owner.setRole(UserRole.CUSTOMER);

        Order order = new Order();
        order.setId(PEDIDO_ID);
        order.setCustomer(owner);
        order.setStatus(status);
        order.setOriginChannel(SalesChannel.TOTEM);
        order.setTotalAmount(new BigDecimal("51.80"));
        order.setCreatedAt(LocalDateTime.now());
        return order;
    }

    private static AuthenticatedUser staffPrincipal() {
        return new AuthenticatedUser(UUID.randomUUID(), "Atendente", "staff@rdn.local", UserRole.STAFF);
    }

    private static AuthenticatedUser customerPrincipal() {
        return new AuthenticatedUser(OWNER_ID, "Cliente Teste", "cliente@rdn.local", UserRole.CUSTOMER);
    }
}
