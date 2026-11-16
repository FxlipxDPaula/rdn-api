package com.rdn.rdn_api.application.loyalty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.rdn.rdn_api.application.audit.AuditGateway;
import com.rdn.rdn_api.application.auth.AuthenticatedUser;
import com.rdn.rdn_api.application.loyalty.exception.ConsentRequiredException;
import com.rdn.rdn_api.application.loyalty.exception.InsufficientPointsException;
import com.rdn.rdn_api.application.loyalty.exception.RedemptionAlreadyAppliedException;
import com.rdn.rdn_api.application.loyalty.exception.RedemptionNotAllowedException;
import com.rdn.rdn_api.application.order.OrderGateway;
import com.rdn.rdn_api.domain.LoyaltyAccount;
import com.rdn.rdn_api.domain.LoyaltyTransaction;
import com.rdn.rdn_api.domain.MenuItem;
import com.rdn.rdn_api.domain.Order;
import com.rdn.rdn_api.domain.OrderStatus;
import com.rdn.rdn_api.domain.TransactionType;
import com.rdn.rdn_api.domain.User;
import com.rdn.rdn_api.domain.UserRole;

/**
 * Testes unitarios do caso de uso de fidelidade.
 */
class LoyaltyServiceTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    private static final UUID ORDER_ID = UUID.randomUUID();

    private final LoyaltyAccountGateway accountGateway = mock(LoyaltyAccountGateway.class);

    private final OrderGateway orderGateway = mock(OrderGateway.class);

    private final AuditGateway auditGateway = mock(AuditGateway.class);

    private LoyaltyService service;

    @BeforeEach
    void setUp() {
        service = new LoyaltyService(accountGateway, orderGateway, auditGateway);

        when(accountGateway.saveAccount(any(LoyaltyAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(accountGateway.saveTransaction(any(LoyaltyTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderGateway.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void clienteSemContaRecebeVisaoNeutra() {
        when(accountGateway.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.empty());

        LoyaltyAccountView view = service.getAccount(customerPrincipal());

        assertThat(view.saldoPontos()).isZero();
        assertThat(view.consentimento()).isFalse();
        assertThat(view.consentimentoEm()).isNull();
    }

    @Test
    void optInCriaContaZerada() {
        when(accountGateway.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.empty());
        when(accountGateway.saveAccount(any(LoyaltyAccount.class))).thenAnswer(invocation -> {
            LoyaltyAccount account = invocation.getArgument(0);
            account.setId(ACCOUNT_ID);
            return account;
        });

        LoyaltyAccountView view = service.setConsent(true, customerPrincipal());

        assertThat(view.consentimento()).isTrue();
        assertThat(view.saldoPontos()).isZero();
        assertThat(view.consentimentoEm()).isNotNull();
    }

    @Test
    void optOutCongelaContaSemApagarSaldo() {
        LoyaltyAccount account = account(50, true);
        when(accountGateway.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(account));

        LoyaltyAccountView view = service.setConsent(false, customerPrincipal());

        assertThat(view.consentimento()).isFalse();
        assertThat(view.saldoPontos()).isEqualTo(50);
        assertThat(view.consentimentoEm()).isNull();
    }

    @Test
    void resgateAplicaDescontoEDebitaPontos() {
        LoyaltyAccount account = account(100, true);
        Order order = awaitingPaymentOrder();

        when(accountGateway.findForUpdateByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(account));
        when(orderGateway.findWithItemsById(ORDER_ID)).thenReturn(Optional.of(order));
        when(accountGateway.existsTransaction(ORDER_ID, TransactionType.REDEEMED)).thenReturn(false);

        RedeemPointsResult result = service.redeem(new RedeemPointsCommand(ORDER_ID, 100),
                customerPrincipal());

        assertThat(result.pontosResgatados()).isEqualTo(100);
        assertThat(result.descontoAplicado()).isEqualByComparingTo("10.00");
        assertThat(result.novoSaldo()).isZero();
        assertThat(result.totalPedido()).isEqualByComparingTo("41.80");
        verify(accountGateway).saveTransaction(any(LoyaltyTransaction.class));
    }

    @Test
    void resgateSemConsentimentoEhRecusado() {
        when(accountGateway.findForUpdateByCustomerId(CUSTOMER_ID))
                .thenReturn(Optional.of(account(100, false)));

        assertThatThrownBy(() -> service.redeem(new RedeemPointsCommand(ORDER_ID, 10),
                customerPrincipal()))
                .isInstanceOf(ConsentRequiredException.class);
    }

    @Test
    void resgateComSaldoInsuficienteEhRecusado() {
        when(accountGateway.findForUpdateByCustomerId(CUSTOMER_ID))
                .thenReturn(Optional.of(account(10, true)));
        when(orderGateway.findWithItemsById(ORDER_ID)).thenReturn(Optional.of(awaitingPaymentOrder()));

        assertThatThrownBy(() -> service.redeem(new RedeemPointsCommand(ORDER_ID, 100),
                customerPrincipal()))
                .isInstanceOf(InsufficientPointsException.class);
    }

    @Test
    void resgateEmPedidoForaDoStatusEhRecusado() {
        LoyaltyAccount account = account(100, true);
        Order order = awaitingPaymentOrder();
        order.changeStatus(OrderStatus.RECEIVED);

        when(accountGateway.findForUpdateByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(account));
        when(orderGateway.findWithItemsById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.redeem(new RedeemPointsCommand(ORDER_ID, 10),
                customerPrincipal()))
                .isInstanceOf(RedemptionNotAllowedException.class);
    }

    @Test
    void resgateRepetidoNoMesmoPedidoEhRecusado() {
        when(accountGateway.findForUpdateByCustomerId(CUSTOMER_ID))
                .thenReturn(Optional.of(account(100, true)));
        when(orderGateway.findWithItemsById(ORDER_ID)).thenReturn(Optional.of(awaitingPaymentOrder()));
        when(accountGateway.existsTransaction(ORDER_ID, TransactionType.REDEEMED)).thenReturn(true);

        assertThatThrownBy(() -> service.redeem(new RedeemPointsCommand(ORDER_ID, 10),
                customerPrincipal()))
                .isInstanceOf(RedemptionAlreadyAppliedException.class);
    }

    @Test
    void acumulaPontosNoPagamentoAprovado() {
        LoyaltyAccount account = account(0, true);
        Order order = awaitingPaymentOrder();

        when(accountGateway.findForUpdateByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(account));
        when(accountGateway.existsTransaction(ORDER_ID, TransactionType.EARNED)).thenReturn(false);

        int points = service.awardForApprovedOrder(order, customerPrincipal());

        assertThat(points).isEqualTo(51);
        assertThat(account.getPointsBalance()).isEqualTo(51);
        verify(accountGateway).saveTransaction(any(LoyaltyTransaction.class));
    }

    @Test
    void naoAcumulaPontosSemConsentimento() {
        when(accountGateway.findForUpdateByCustomerId(CUSTOMER_ID))
                .thenReturn(Optional.of(account(0, false)));

        int points = service.awardForApprovedOrder(awaitingPaymentOrder(), customerPrincipal());

        assertThat(points).isZero();
    }

    private static LoyaltyAccount account(int points, boolean consent) {
        User customer = new User();
        customer.setId(CUSTOMER_ID);

        LoyaltyAccount account = new LoyaltyAccount();
        account.setId(ACCOUNT_ID);
        account.setCustomer(customer);
        account.setPointsBalance(points);
        account.setConsentGiven(consent);
        return account;
    }

    private static Order awaitingPaymentOrder() {
        User customer = new User();
        customer.setId(CUSTOMER_ID);
        customer.setRole(UserRole.CUSTOMER);

        MenuItem burger = new MenuItem();
        burger.setName("X-Burger");
        burger.setPrice(new BigDecimal("25.90"));
        burger.setIsAvailable(Boolean.TRUE);

        Order order = new Order();
        order.setId(ORDER_ID);
        order.setCustomer(customer);
        order.addItem(burger, 2);
        order.calculateTotal();
        order.awaitPayment();
        order.setCreatedAt(LocalDateTime.now());

        return order;
    }

    private static AuthenticatedUser customerPrincipal() {
        return new AuthenticatedUser(CUSTOMER_ID, "Cliente Teste", "cliente@rdn.local",
                UserRole.CUSTOMER);
    }
}
