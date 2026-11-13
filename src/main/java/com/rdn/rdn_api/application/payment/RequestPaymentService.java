package com.rdn.rdn_api.application.payment;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rdn.rdn_api.application.auth.AuthenticatedUser;
import com.rdn.rdn_api.application.order.OrderGateway;
import com.rdn.rdn_api.application.order.exception.OperationNotAllowedException;
import com.rdn.rdn_api.application.order.exception.ResourceNotFoundException;
import com.rdn.rdn_api.application.payment.exception.IdempotencyConflictException;
import com.rdn.rdn_api.application.payment.exception.MissingIdempotencyKeyException;
import com.rdn.rdn_api.application.payment.exception.OrderNotAwaitingPaymentException;
import com.rdn.rdn_api.domain.Order;
import com.rdn.rdn_api.domain.OrderStatus;
import com.rdn.rdn_api.domain.Payment;
import com.rdn.rdn_api.domain.UserRole;

/**
 * Caso de uso de solicitacao de pagamento ao provedor simulado.
 *
 * <p>Responsabilidades: garantir idempotencia pela chave, cobrar o valor
 * calculado pelo dominio, registrar a tentativa e refletir o desfecho no status
 * do pedido. Aprovado libera o pedido para a cozinha; recusado cancela; falha
 * tecnica mantem o pedido aguardando nova tentativa.</p>
 */
@Service
public class RequestPaymentService {

    private final OrderGateway orderGateway;

    private final PaymentRepository paymentRepository;

    private final PaymentProvider paymentProvider;

    public RequestPaymentService(OrderGateway orderGateway, PaymentRepository paymentRepository,
            PaymentProvider paymentProvider) {
        this.orderGateway = orderGateway;
        this.paymentRepository = paymentRepository;
        this.paymentProvider = paymentProvider;
    }

    /**
     * Solicita o pagamento de um pedido.
     *
     * @param pedidoId       pedido a cobrar
     * @param idempotencyKey chave enviada no cabecalho {@code Idempotency-Key}
     * @param command        forma de pagamento e desfecho simulado
     * @param principal      usuario autenticado
     * @return tentativa processada e indicacao de replay
     */
    @Transactional
    public PaymentResult request(UUID pedidoId, String idempotencyKey, PaymentCommand command,
            AuthenticatedUser principal) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new MissingIdempotencyKeyException();
        }
        String key = idempotencyKey.trim();

        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            return replay(existing.get(), pedidoId);
        }

        Order order = orderGateway.findWithItemsById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", pedidoId));
        checkOwnership(order, principal);

        if (OrderStatus.AGUARDANDO_PAGAMENTO != order.getStatus()) {
            throw new OrderNotAwaitingPaymentException(pedidoId, order.getStatus());
        }

        // total sempre vem do dominio; pedido criado com itens garante valor positivo
        BigDecimal amount = order.getTotalAmount();
        Payment payment = new Payment(order, amount, command.formaPagamento(), key);

        ChargeResult result = paymentProvider.charge(new ChargeRequest(
                pedidoId, amount, command.formaPagamento(), key, command.simular()));

        applyResult(payment, order, result);

        orderGateway.save(order);
        return new PaymentResult(paymentRepository.save(payment), false);
    }

    private PaymentResult replay(Payment payment, UUID pedidoId) {
        UUID paymentOrderId = payment.getOrder() == null ? null : payment.getOrder().getId();
        if (paymentOrderId != null && !paymentOrderId.equals(pedidoId)) {
            throw new IdempotencyConflictException(
                    "Chave de idempotencia ja utilizada em outro pedido.");
        }

        return new PaymentResult(payment, true);
    }

    private static void applyResult(Payment payment, Order order, ChargeResult result) {
        if (result.approved()) {
            payment.markApproved(result.transactionId(), result.rawResponse());
            order.confirmPayment();
            return;
        }

        if (result.failed()) {
            payment.markFailed(result.rawResponse());
            return;
        }

        payment.markDeclined(result.rawResponse());
        order.cancel("Pagamento recusado pelo provedor.");
    }

    private static void checkOwnership(Order order, AuthenticatedUser principal) {
        if (principal == null || UserRole.CUSTOMER != principal.role()) {
            return;
        }

        boolean owner = order.getCustomer() != null
                && principal.id().equals(order.getCustomer().getId());
        if (!owner) {
            throw new OperationNotAllowedException("Pedido pertence a outro cliente.");
        }
    }
}
