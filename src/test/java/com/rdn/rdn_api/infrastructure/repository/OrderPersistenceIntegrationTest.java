package com.rdn.rdn_api.infrastructure.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import com.rdn.rdn_api.domain.OrderStatus;
import com.rdn.rdn_api.domain.PaymentMethod;
import com.rdn.rdn_api.domain.PaymentStatus;
import com.rdn.rdn_api.domain.SalesChannel;
import com.rdn.rdn_api.domain.UserRole;
import com.rdn.rdn_api.infrastructure.persistence.entity.AddressEmbeddable;
import com.rdn.rdn_api.infrastructure.persistence.entity.BranchJpaEntity;
import com.rdn.rdn_api.infrastructure.persistence.entity.MenuItemJpaEntity;
import com.rdn.rdn_api.infrastructure.persistence.entity.MenuJpaEntity;
import com.rdn.rdn_api.infrastructure.persistence.entity.OrderItemJpaEntity;
import com.rdn.rdn_api.infrastructure.persistence.entity.OrderJpaEntity;
import com.rdn.rdn_api.infrastructure.persistence.entity.PaymentJpaEntity;
import com.rdn.rdn_api.infrastructure.persistence.entity.UserJpaEntity;

import jakarta.persistence.EntityManager;

/**
 * Prova que o modelo persistente do Fluxo A grava e le de volta um pedido com
 * itens e um pagamento, e que as restricoes de unicidade sao respeitadas.
 *
 * <p>Roda sobre H2 em modo PostgreSQL (ver src/test/resources/application.properties),
 * porque a maquina de desenvolvimento nao possui Docker nem PostgreSQL local. A
 * validade do DDL de producao e coberta por
 * {@code com.rdn.rdn_api.infrastructure.persistence.SchemaDdlTest}.</p>
 */
@SpringBootTest
@Transactional
class OrderPersistenceIntegrationTest {

    private static final BigDecimal BURGER_PRICE = new BigDecimal("25.90");

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private BranchJpaRepository branchRepository;

    @Autowired
    private MenuJpaRepository menuRepository;

    @Autowired
    private MenuItemJpaRepository menuItemRepository;

    @Autowired
    private OrderJpaRepository orderRepository;

    @Autowired
    private PaymentJpaRepository paymentRepository;

    @Autowired
    private InventoryItemJpaRepository inventoryItemRepository;

    @Test
    void persistePedidoComItensEPagamento() {
        MenuItemJpaEntity burger = persistMenuItem("X-Burger");
        BranchJpaEntity branch = burger.getMenu().getBranch();

        OrderJpaEntity order = newOrder(branch, burger, SalesChannel.APP,
                OrderStatus.RECEIVED, "order-" + UUID.randomUUID());
        order.setBranch(branch);
        orderRepository.saveAndFlush(order);

        PaymentJpaEntity payment = new PaymentJpaEntity();
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setMethod(PaymentMethod.MOCK);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setIdempotencyKey("payment-" + UUID.randomUUID());
        payment.setRequestedAt(LocalDateTime.now());
        paymentRepository.saveAndFlush(payment);

        entityManager.clear();

        OrderJpaEntity reloaded = orderRepository.findWithItemsById(order.getId()).orElseThrow();

        assertThat(reloaded.getItems()).hasSize(1);
        assertThat(reloaded.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(reloaded.getItems().get(0).getSubtotal()).isEqualByComparingTo("51.80");
        assertThat(reloaded.getTotalAmount()).isEqualByComparingTo("51.80");
        assertThat(reloaded.getOriginChannel()).isEqualTo(SalesChannel.APP);
        assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.RECEIVED);
        assertThat(reloaded.getBranch().getName()).isEqualTo("RDN Centro");

        List<PaymentJpaEntity> payments =
                paymentRepository.findByOrderIdOrderByRequestedAtDesc(order.getId());
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payments.get(0).getMethod()).isEqualTo(PaymentMethod.MOCK);
    }

    @Test
    void filtraPedidosPorCanalEStatus() {
        MenuItemJpaEntity burger = persistMenuItem("X-Burger");
        BranchJpaEntity branch = burger.getMenu().getBranch();

        OrderJpaEntity order = newOrder(branch, burger, SalesChannel.TOTEM,
                OrderStatus.PREPARING, "order-" + UUID.randomUUID());
        orderRepository.saveAndFlush(order);

        var byChannelAndStatus = orderRepository.findByOriginChannelAndStatus(
                SalesChannel.TOTEM, OrderStatus.PREPARING, PageRequest.of(0, 10));
        assertThat(byChannelAndStatus.getContent())
                .extracting(OrderJpaEntity::getId)
                .contains(order.getId());

        var otherChannel = orderRepository.findByOriginChannel(
                SalesChannel.WEB, PageRequest.of(0, 10));
        assertThat(otherChannel.getContent())
                .extracting(OrderJpaEntity::getId)
                .doesNotContain(order.getId());
    }

    @Test
    void persisteEstoquePorFilialEItem() {
        MenuItemJpaEntity burger = persistMenuItem("X-Burger");
        BranchJpaEntity branch = burger.getMenu().getBranch();

        assertThat(inventoryItemRepository.findByBranchIdAndMenuItemId(branch.getId(), burger.getId()))
                .isPresent()
                .get()
                .extracting(item -> item.getQuantityOnHand())
                .isEqualTo(new BigDecimal("100.000"));
    }

    @Test
    void rejeitaEmailDuplicado() {
        userRepository.saveAndFlush(newUser("duplicado@rdn.local"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(newUser("duplicado@rdn.local")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejeitaIdempotencyKeyDuplicada() {
        MenuItemJpaEntity burger = persistMenuItem("X-Burger");
        BranchJpaEntity branch = burger.getMenu().getBranch();
        String key = "idem-" + UUID.randomUUID();

        orderRepository.saveAndFlush(
                newOrder(branch, burger, SalesChannel.APP, OrderStatus.RECEIVED, key));

        assertThatThrownBy(() -> orderRepository.saveAndFlush(
                newOrder(branch, burger, SalesChannel.APP, OrderStatus.RECEIVED, key)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private MenuItemJpaEntity persistMenuItem(String name) {
        AddressEmbeddable address = new AddressEmbeddable();
        address.setStreet("Rua das Laranjeiras");
        address.setNumber("100");
        address.setComplement("Loja 1");
        address.setNeighborhood("Centro");
        address.setCity("Sao Paulo");
        address.setState("SP");
        address.setZipCode("01000-000");

        BranchJpaEntity branch = new BranchJpaEntity();
        branch.setName("RDN Centro");
        branch.setIsOpen(Boolean.TRUE);
        branch.setAddress(address);
        branchRepository.saveAndFlush(branch);

        MenuJpaEntity menu = new MenuJpaEntity();
        menu.setBranch(branch);
        menu.setTitle("Cardapio Centro");
        menu.setIsActive(Boolean.TRUE);
        menuRepository.saveAndFlush(menu);

        MenuItemJpaEntity item = new MenuItemJpaEntity();
        item.setMenu(menu);
        item.setName(name);
        item.setDescription("Item de teste");
        item.setPrice(BURGER_PRICE);
        item.setIsAvailable(Boolean.TRUE);
        menuItemRepository.saveAndFlush(item);

        var inventory = new com.rdn.rdn_api.infrastructure.persistence.entity.InventoryItemJpaEntity();
        inventory.setBranch(branch);
        inventory.setMenuItem(item);
        inventory.setQuantityOnHand(new BigDecimal("100.000"));
        inventory.setMinimumThreshold(new BigDecimal("5.000"));
        inventoryItemRepository.saveAndFlush(inventory);

        return item;
    }

    private OrderJpaEntity newOrder(BranchJpaEntity branch, MenuItemJpaEntity menuItem,
            SalesChannel channel, OrderStatus status, String idempotencyKey) {
        BigDecimal subtotal = BURGER_PRICE.multiply(BigDecimal.valueOf(2));

        OrderJpaEntity order = new OrderJpaEntity();
        order.setBranch(branch);
        order.setOriginChannel(channel);
        order.setStatus(status);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalAmount(subtotal);
        order.setCreatedAt(LocalDateTime.now());
        order.setIdempotencyKey(idempotencyKey);

        OrderItemJpaEntity item = new OrderItemJpaEntity();
        item.setOrder(order);
        item.setMenuItem(menuItem);
        item.setQuantity(2);
        item.setUnitPrice(BURGER_PRICE);
        item.setSubtotal(subtotal);
        order.getItems().add(item);

        return order;
    }

    private UserJpaEntity newUser(String email) {
        UserJpaEntity user = new UserJpaEntity();
        user.setFullName("Usuario de Teste");
        user.setEmail(email);
        user.setPasswordHash("{noop}nao-usado-em-teste");
        user.setRole(UserRole.CUSTOMER);
        user.setIsActive(Boolean.TRUE);

        return user;
    }

}
