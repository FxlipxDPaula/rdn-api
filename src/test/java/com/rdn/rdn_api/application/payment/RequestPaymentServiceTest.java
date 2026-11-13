package com.rdn.rdn_api.application.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import com.rdn.rdn_api.domain.PaymentMethod;
import com.rdn.rdn_api.domain.PaymentStatus;
import com.rdn.rdn_api.domain.User;
import com.rdn.rdn_api.domain.UserRole;

/**
 * Testes unitarios do caso de uso de solicitacao de pagamento.
 */
class RequestPaymentServiceTest {

    private static final String KEY = "chave-idempotencia-1";

    private static final UUID PEDIDO_ID = UUID.randomUUID();

    private static final UUID OWNER_ID = UUID.randomUUID();

    private final OrderGateway orderGateway = mock(OrderGateway.class);

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);

    private final PaymentProvider paymentProvider = mock(PaymentProvider.class);

    private RequestPaymentService service;

    @BeforeEach
    void setUp() {
        service = new RequestPaymentService(orderGateway, paymentRepository, paymentProvider);

        when(paymentRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            if (payment.getId() == null) {
                payment.setId(UUID.randomUUID());
            }
            return payment;
        });
    }

    @Test
    void pagamentoAprovadoLiberaPedidoParaCozinha() {
        Order order = awaitingPaymentOrder();
        when(orderGateway.findWithItemsById(PEDIDO_ID)).thenReturn(Optional.of(order));
        when(paymentProvider.charge(any())).thenReturn(
                new ChargeResult(true, false, "MOCK-1", "{\"status\":\"APROVADO\"}"));

        PaymentResult result = service.request(PEDIDO_ID, KEY, mockCommand(SimulationMode.APROVADO),
                customerPrincipal());

        assertThat(result.replay()).isFalse();
        assertThat(result.payment().getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(result.payment().getGatewayTransactionId()).isEqualTo("MOCK-1");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.RECEIVED);
    }

    @Test
    void pagamentoRecusadoCancelaPedido() {
        Order order = awaitingPaymentOrder();
        when(orderGateway.findWithItemsById(PEDIDO_ID)).thenReturn(Optional.of(order));
        when(paymentProvider.charge(any())).thenReturn(
                new ChargeResult(false, false, null, "{\"status\":\"RECUSADO\"}"));

        PaymentResult result = service.request(PEDIDO_ID, KEY, mockCommand(SimulationMode.RECUSADO),
                customerPrincipal());

        assertThat(result.payment().getStatus()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancellationReason()).contains("recusado");
    }

    @Test
    void falhaTecnicaMantemPedidoAguardandoPagamento() {
        Order order = awaitingPaymentOrder();
        when(orderGateway.findWithItemsById(PEDIDO_ID)).thenReturn(Optional.of(order));
        when(paymentProvider.charge(any())).thenReturn(
                new ChargeResult(false, true, null, "{\"status\":\"FALHA\"}"));

        PaymentResult result = service.request(PEDIDO_ID, KEY, mockCommand(SimulationMode.FALHA),
                customerPrincipal());

        assertThat(result.payment().getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.AGUARDANDO_PAGAMENTO);
    }

    @Test
    void replayDaMesmaChaveNaoCobraNovamente() {
        Payment existing = new Payment(awaitingPaymentOrder(), new BigDecimal("51.80"),
                PaymentMethod.MOCK, KEY);
        existing.setId(UUID.randomUUID());
        when(paymentRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(existing));

        PaymentResult result = service.request(PEDIDO_ID, KEY, mockCommand(SimulationMode.APROVADO),
                customerPrincipal());

        assertThat(result.replay()).isTrue();
        assertThat(result.payment().getId()).isEqualTo(existing.getId());
        verifyNoInteractions(paymentProvider);
    }

    @Test
    void chaveReutilizadaEmOutroPedidoRetornaConflito() {
        Payment existing = new Payment(otherOrder(), new BigDecimal("10.00"), PaymentMethod.MOCK, KEY);
        existing.setId(UUID.randomUUID());
        when(paymentRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.request(PEDIDO_ID, KEY,
                mockCommand(SimulationMode.APROVADO), customerPrincipal()))
                .isInstanceOf(IdempotencyConflictException.class);

        verifyNoInteractions(paymentProvider);
    }

    @Test
    void pedidoForaDoEstadoDeCobrancaRetornaConflito() {
        Order order = awaitingPaymentOrder();
        order.changeStatus(OrderStatus.RECEIVED);
        when(orderGateway.findWithItemsById(PEDIDO_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.request(PEDIDO_ID, KEY,
                mockCommand(SimulationMode.APROVADO), customerPrincipal()))
                .isInstanceOf(OrderNotAwaitingPaymentException.class);

        verifyNoInteractions(paymentProvider);
    }

    @Test
    void pedidoInexistenteRetornaRecursoNaoEncontrado() {
        when(orderGateway.findWithItemsById(PEDIDO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.request(PEDIDO_ID, KEY,
                mockCommand(SimulationMode.APROVADO), customerPrincipal()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void clienteNaoCobraPedidoDeOutroCliente() {
        when(orderGateway.findWithItemsById(PEDIDO_ID))
                .thenReturn(Optional.of(awaitingPaymentOrder()));

        AuthenticatedUser outro = new AuthenticatedUser(UUID.randomUUID(), "Outro",
                "outro@rdn.local", UserRole.CUSTOMER);

        assertThatThrownBy(() -> service.request(PEDIDO_ID, KEY,
                mockCommand(SimulationMode.APROVADO), outro))
                .isInstanceOf(OperationNotAllowedException.class);
    }

    @Test
    void exigeCabecalhoDeIdempotencia() {
        assertThatThrownBy(() -> service.request(PEDIDO_ID, "  ",
                mockCommand(SimulationMode.APROVADO), customerPrincipal()))
                .isInstanceOf(MissingIdempotencyKeyException.class);

        verifyNoInteractions(paymentProvider);
    }

    private static PaymentCommand mockCommand(SimulationMode mode) {
        return new PaymentCommand(PaymentMethod.MOCK, mode);
    }

    private static AuthenticatedUser customerPrincipal() {
        return new AuthenticatedUser(OWNER_ID, "Cliente Teste", "cliente@rdn.local", UserRole.CUSTOMER);
    }

    private static Order awaitingPaymentOrder() {
        Order order = orderWithOwner(OWNER_ID);
        order.awaitPayment();
        return order;
    }

    private static Order otherOrder() {
        Order order = orderWithOwner(UUID.randomUUID());
        // identificador diferente: a chave pertence a outro pedido
        order.setId(UUID.randomUUID());
        order.awaitPayment();
        return order;
    }

    private static Order orderWithOwner(UUID ownerId) {
        User owner = new User();
        owner.setId(ownerId);
        owner.setFullName("Cliente Teste");
        owner.setRole(UserRole.CUSTOMER);

        Order order = new Order();
        order.setId(PEDIDO_ID);
        order.setCustomer(owner);
        order.setTotalAmount(new BigDecimal("51.80"));
        order.setCreatedAt(LocalDateTime.now());
        return order;
    }
}
