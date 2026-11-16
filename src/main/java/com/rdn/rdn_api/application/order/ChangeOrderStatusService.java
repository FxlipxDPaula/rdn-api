package com.rdn.rdn_api.application.order;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rdn.rdn_api.application.audit.AuditAction;
import com.rdn.rdn_api.application.audit.AuditEntry;
import com.rdn.rdn_api.application.audit.AuditGateway;
import com.rdn.rdn_api.application.auth.AuthenticatedUser;
import com.rdn.rdn_api.application.order.exception.InvalidStatusTransitionException;
import com.rdn.rdn_api.application.order.exception.OperationNotAllowedException;
import com.rdn.rdn_api.application.order.exception.ResourceNotFoundException;
import com.rdn.rdn_api.domain.Order;
import com.rdn.rdn_api.domain.OrderStatus;
import com.rdn.rdn_api.domain.UserRole;

/**
 * Caso de uso de mudanca de status do pedido.
 *
 * <p>A regra de transicao continua no dominio ({@link Order#changeStatus});
 * aqui apenas traduzimos {@link IllegalStateException} em conflito de negocio e
 * aplicamos a restricao de dono para o perfil {@code CUSTOMER}.</p>
 */
@Service
public class ChangeOrderStatusService {

    private final OrderGateway orderGateway;

    private final AuditGateway auditGateway;

    public ChangeOrderStatusService(OrderGateway orderGateway, AuditGateway auditGateway) {
        this.orderGateway = orderGateway;
        this.auditGateway = auditGateway;
    }

    /**
     * Aplica a transicao solicitada.
     *
     * @param pedidoId identificador do pedido
     * @param command  status desejado e motivo opcional
     * @param principal usuario autenticado
     * @return pedido atualizado
     * @throws ResourceNotFoundException        quando o pedido nao existe
     * @throws OperationNotAllowedException     quando o cliente tenta agir em pedido alheio
     * @throws InvalidStatusTransitionException quando a maquina de estados recusa
     */
    @Transactional
    public Order changeStatus(UUID pedidoId, ChangeStatusCommand command, AuthenticatedUser principal) {
        Order order = orderGateway.findWithItemsById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", pedidoId));

        checkPermission(order, command.status(), principal);

        OrderStatus anterior = order.getStatus();
        try {
            if (OrderStatus.CANCELLED == command.status()) {
                order.cancel(normalizeReason(command.motivo()));
            } else {
                order.changeStatus(command.status());
            }
        } catch (IllegalStateException ex) {
            throw new InvalidStatusTransitionException(pedidoId, anterior, command.status());
        }

        Order saved = orderGateway.save(order);
        auditStatusChange(saved, anterior, principal);

        return saved;
    }

    private void auditStatusChange(Order order, OrderStatus anterior, AuthenticatedUser principal) {
        AuditAction action = OrderStatus.CANCELLED == order.getStatus()
                ? AuditAction.ORDER_CANCELLED
                : AuditAction.ORDER_STATUS_CHANGED;
        String details = (anterior == null ? "null" : anterior) + "->" + order.getStatus();
        if (order.getCancellationReason() != null) {
            details = details + " motivo=" + order.getCancellationReason();
        }

        UUID actorId = principal == null ? null : principal.id();
        String actorEmail = principal == null ? null : principal.email();
        UserRole actorRole = principal == null ? null : principal.role();

        auditGateway.record(new AuditEntry(actorId, actorEmail, actorRole, action, "ORDER",
                order.getId(), details));
    }

    private static void checkPermission(Order order, OrderStatus target, AuthenticatedUser principal) {
        if (principal == null || UserRole.CUSTOMER != principal.role()) {
            return;
        }

        boolean owner = order.getCustomer() != null
                && principal.id().equals(order.getCustomer().getId());
        if (!owner || OrderStatus.CANCELLED != target) {
            throw new OperationNotAllowedException("Cliente pode apenas cancelar o proprio pedido.");
        }
    }

    private static String normalizeReason(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            return "Cancelado pela operacao";
        }

        return motivo.trim();
    }
}
