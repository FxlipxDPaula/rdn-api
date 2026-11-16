package com.rdn.rdn_api.application.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.rdn.rdn_api.application.audit.AuditGateway;
import com.rdn.rdn_api.application.auth.AuthenticatedUser;
import com.rdn.rdn_api.application.order.CreateOrderCommand.CreateOrderItemCommand;
import com.rdn.rdn_api.application.order.exception.InsufficientStockException;
import com.rdn.rdn_api.application.order.exception.ResourceNotFoundException;
import com.rdn.rdn_api.application.promotion.PromotionGateway;
import com.rdn.rdn_api.application.promotion.exception.PromotionNotApplicableException;
import com.rdn.rdn_api.domain.Branch;
import com.rdn.rdn_api.domain.DiscountType;
import com.rdn.rdn_api.domain.MenuItem;
import com.rdn.rdn_api.domain.Order;
import com.rdn.rdn_api.domain.OrderStatus;
import com.rdn.rdn_api.domain.PaymentMethod;
import com.rdn.rdn_api.domain.Promotion;
import com.rdn.rdn_api.domain.SalesChannel;
import com.rdn.rdn_api.domain.User;
import com.rdn.rdn_api.domain.UserRole;

/**
 * Testes unitarios do caso de uso de criacao de pedido.
 */
class CreateOrderServiceTest {

    private static final UUID BRANCH_ID = UUID.randomUUID();

    private static final UUID PRODUTO_ID = UUID.randomUUID();

    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    private static final UUID PROMOCAO_ID = UUID.randomUUID();

    private final OrderGateway orderGateway = mock(OrderGateway.class);

    private final CatalogGateway catalogGateway = mock(CatalogGateway.class);

    private final CustomerGateway customerGateway = mock(CustomerGateway.class);

    private final InventoryGateway inventoryGateway = mock(InventoryGateway.class);

    private final PromotionGateway promotionGateway = mock(PromotionGateway.class);

    private final AuditGateway auditGateway = mock(AuditGateway.class);

    private CreateOrderService createOrderService;

    @BeforeEach
    void setUp() {
        createOrderService = new CreateOrderService(orderGateway, catalogGateway,
                customerGateway, inventoryGateway, promotionGateway, auditGateway);

        when(orderGateway.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void criaPedidoCalculandoTotalPeloDominio() {
        givenCatalogAndCustomer();
        givenStock(new BigDecimal("10.000"));

        Order order = createOrderService.create(command(PaymentMethod.CASH, 2), customerPrincipal());

        assertThat(order.getTotalAmount()).isEqualByComparingTo("51.80");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.RECEIVED);
        assertThat(order.getOriginChannel()).isEqualTo(SalesChannel.TOTEM);
        assertThat(order.getCustomer().getId()).isEqualTo(CUSTOMER_ID);
        verify(inventoryGateway).reserve(eq(BRANCH_ID), eq(PRODUTO_ID), eq(BigDecimal.valueOf(2)),
                anyString());
    }

    @Test
    void pedidoComPagamentoMockNasceAguardandoPagamento() {
        givenCatalogAndCustomer();
        givenStock(new BigDecimal("10.000"));

        Order order = createOrderService.create(command(PaymentMethod.MOCK, 1), customerPrincipal());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.AGUARDANDO_PAGAMENTO);
    }

    @Test
    void recusaEstoqueInsuficienteSemPersistirPedido() {
        givenCatalogAndCustomer();
        when(inventoryGateway.reserve(eq(BRANCH_ID), eq(PRODUTO_ID), any(), anyString()))
                .thenReturn(new InventoryGateway.ReservationResult(false, new BigDecimal("1.000")));

        assertThatThrownBy(() -> createOrderService.create(command(PaymentMethod.CASH, 5),
                customerPrincipal()))
                .isInstanceOf(InsufficientStockException.class);

        verify(orderGateway, never()).save(any());
    }

    @Test
    void recusaProdutoInexistenteSemTocarNoEstoque() {
        when(catalogGateway.findBranch(BRANCH_ID)).thenReturn(Optional.of(branch()));
        when(catalogGateway.findMenuItem(PRODUTO_ID)).thenReturn(Optional.empty());
        when(customerGateway.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));

        assertThatThrownBy(() -> createOrderService.create(command(PaymentMethod.CASH, 1),
                customerPrincipal()))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(inventoryGateway);
        verify(orderGateway, never()).save(any());
    }

    @Test
    void agrupaItensRepetidosAntesDeReservarEstoque() {
        givenCatalogAndCustomer();
        givenStock(new BigDecimal("10.000"));

        CreateOrderCommand command = new CreateOrderCommand(SalesChannel.TOTEM, BRANCH_ID, null,
                PaymentMethod.CASH, null,
                List.of(new CreateOrderItemCommand(PRODUTO_ID, 2, null),
                        new CreateOrderItemCommand(PRODUTO_ID, 3, null)));

        Order order = createOrderService.create(command, customerPrincipal());

        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().get(0).getQuantity()).isEqualTo(5);
        verify(inventoryGateway).reserve(eq(BRANCH_ID), eq(PRODUTO_ID), eq(BigDecimal.valueOf(5)),
                anyString());
    }

    @Test
    void aplicaPromocaoPercentualSobreOSubtotal() {
        givenCatalogAndCustomer();
        givenStock(new BigDecimal("10.000"));
        when(promotionGateway.findById(PROMOCAO_ID)).thenReturn(Optional.of(promotion(null)));

        CreateOrderCommand command = new CreateOrderCommand(SalesChannel.TOTEM, BRANCH_ID, null,
                PaymentMethod.CASH, PROMOCAO_ID,
                List.of(new CreateOrderItemCommand(PRODUTO_ID, 2, null)));

        Order order = createOrderService.create(command, customerPrincipal());

        assertThat(order.getDiscountAmount()).isEqualByComparingTo("5.18");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("46.62");
        assertThat(order.getPromotion().getId()).isEqualTo(PROMOCAO_ID);
    }

    @Test
    void recusaPromocaoExpiradaSemPersistirPedido() {
        givenCatalogAndCustomer();
        givenStock(new BigDecimal("10.000"));
        when(promotionGateway.findById(PROMOCAO_ID))
                .thenReturn(Optional.of(promotion(java.time.LocalDateTime.now().minusDays(1))));

        CreateOrderCommand command = new CreateOrderCommand(SalesChannel.TOTEM, BRANCH_ID, null,
                PaymentMethod.CASH, PROMOCAO_ID,
                List.of(new CreateOrderItemCommand(PRODUTO_ID, 2, null)));

        assertThatThrownBy(() -> createOrderService.create(command, customerPrincipal()))
                .isInstanceOf(PromotionNotApplicableException.class);

        verify(orderGateway, never()).save(any());
    }

    @Test
    void recusaPromocaoInexistente() {
        givenCatalogAndCustomer();
        givenStock(new BigDecimal("10.000"));
        when(promotionGateway.findById(PROMOCAO_ID)).thenReturn(Optional.empty());

        CreateOrderCommand command = new CreateOrderCommand(SalesChannel.TOTEM, BRANCH_ID, null,
                PaymentMethod.CASH, PROMOCAO_ID,
                List.of(new CreateOrderItemCommand(PRODUTO_ID, 2, null)));

        assertThatThrownBy(() -> createOrderService.create(command, customerPrincipal()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderGateway, never()).save(any());
    }

    private void givenCatalogAndCustomer() {
        when(catalogGateway.findBranch(BRANCH_ID)).thenReturn(Optional.of(branch()));
        when(catalogGateway.findMenuItem(PRODUTO_ID)).thenReturn(Optional.of(menuItem()));
        when(customerGateway.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
    }

    private void givenStock(BigDecimal available) {
        when(inventoryGateway.reserve(eq(BRANCH_ID), eq(PRODUTO_ID), any(), anyString()))
                .thenReturn(new InventoryGateway.ReservationResult(true, available));
    }

    private static CreateOrderCommand command(PaymentMethod method, int quantity) {
        return new CreateOrderCommand(SalesChannel.TOTEM, BRANCH_ID, null, method, null,
                List.of(new CreateOrderItemCommand(PRODUTO_ID, quantity, "sem cebola")));
    }

    private static Promotion promotion(java.time.LocalDateTime endDate) {
        Promotion promotion = new Promotion();
        promotion.setId(PROMOCAO_ID);
        promotion.setTitle("Semana do Burger");
        promotion.setDiscountType(DiscountType.PERCENTAGE);
        promotion.setDiscountValue(new BigDecimal("10.00"));
        promotion.setMinOrderValue(new BigDecimal("30.00"));
        promotion.setEndDate(endDate);
        promotion.setIsActive(Boolean.TRUE);
        return promotion;
    }

    private static AuthenticatedUser customerPrincipal() {
        return new AuthenticatedUser(CUSTOMER_ID, "Cliente Teste", "cliente@rdn.local", UserRole.CUSTOMER);
    }

    private static Branch branch() {
        Branch branch = new Branch();
        branch.setId(BRANCH_ID);
        branch.setName("RDN Teste");
        branch.setIsOpen(Boolean.TRUE);
        return branch;
    }

    private static MenuItem menuItem() {
        MenuItem menuItem = new MenuItem();
        menuItem.setId(PRODUTO_ID);
        menuItem.setName("X-Burger");
        menuItem.setPrice(new BigDecimal("25.90"));
        menuItem.setIsAvailable(Boolean.TRUE);
        return menuItem;
    }

    private static User customer() {
        User user = new User();
        user.setId(CUSTOMER_ID);
        user.setFullName("Cliente Teste");
        user.setEmail("cliente@rdn.local");
        user.setRole(UserRole.CUSTOMER);
        user.setIsActive(Boolean.TRUE);
        return user;
    }
}
