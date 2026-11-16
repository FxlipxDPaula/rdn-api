package com.rdn.rdn_api.application.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rdn.rdn_api.application.audit.AuditAction;
import com.rdn.rdn_api.application.audit.AuditEntry;
import com.rdn.rdn_api.application.audit.AuditGateway;
import com.rdn.rdn_api.application.auth.AuthenticatedUser;
import com.rdn.rdn_api.application.order.CreateOrderCommand.CreateOrderItemCommand;
import com.rdn.rdn_api.application.order.exception.InsufficientStockException;
import com.rdn.rdn_api.application.order.exception.ResourceNotFoundException;
import com.rdn.rdn_api.application.promotion.PromotionGateway;
import com.rdn.rdn_api.application.promotion.exception.PromotionNotApplicableException;
import com.rdn.rdn_api.domain.Branch;
import com.rdn.rdn_api.domain.MenuItem;
import com.rdn.rdn_api.domain.Order;
import com.rdn.rdn_api.domain.OrderStatus;
import com.rdn.rdn_api.domain.PaymentMethod;
import com.rdn.rdn_api.domain.Promotion;
import com.rdn.rdn_api.domain.User;
import com.rdn.rdn_api.domain.UserRole;

/**
 * Caso de uso de criacao de pedido.
 *
 * <p>Orquestra quatro passos: validar o catalogo (unidade e produtos),
 * resolver o cliente a partir da identidade autenticada, reservar o estoque da
 * unidade e persistir o agregado. Todos os passos rodam na mesma transacao:
 * se um item falhar, nenhuma baixa de estoque nem pedido permanece gravado.</p>
 */
@Service
public class CreateOrderService {

    private final OrderGateway orderGateway;

    private final CatalogGateway catalogGateway;

    private final CustomerGateway customerGateway;

    private final InventoryGateway inventoryGateway;

    private final PromotionGateway promotionGateway;

    private final AuditGateway auditGateway;

    public CreateOrderService(OrderGateway orderGateway, CatalogGateway catalogGateway,
            CustomerGateway customerGateway, InventoryGateway inventoryGateway,
            PromotionGateway promotionGateway, AuditGateway auditGateway) {
        this.orderGateway = orderGateway;
        this.catalogGateway = catalogGateway;
        this.customerGateway = customerGateway;
        this.inventoryGateway = inventoryGateway;
        this.promotionGateway = promotionGateway;
        this.auditGateway = auditGateway;
    }

    /**
     * Cria o pedido, baixa o estoque e grava a movimentacao de saida.
     *
     * @param command   dados do pedido
     * @param principal usuario autenticado que originou a requisicao
     * @return pedido persistido
     * @throws ResourceNotFoundException   quando unidade, produto ou cliente nao existem
     * @throws InsufficientStockException  quando o saldo da unidade nao atende algum item
     */
    @Transactional
    public Order create(CreateOrderCommand command, AuthenticatedUser principal) {
        Branch branch = catalogGateway.findBranch(command.unidadeId())
                .orElseThrow(() -> new ResourceNotFoundException("Unidade", command.unidadeId()));

        User customer = resolveCustomer(command.clienteId(), principal);

        Order order = new Order();
        order.setBranch(branch);
        order.setCustomer(customer);
        order.setOriginChannel(command.canalPedido());
        order.setCreatedAt(LocalDateTime.now());

        Map<UUID, Integer> quantities = mergeQuantities(command.itens());

        // 1) valida catalogo inteiro antes de tocar em estoque
        for (Map.Entry<UUID, Integer> entry : quantities.entrySet()) {
            MenuItem menuItem = catalogGateway.findMenuItem(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Produto", entry.getKey()));
            if (!Boolean.TRUE.equals(menuItem.getIsAvailable())) {
                throw new ResourceNotFoundException("Produto disponivel", entry.getKey());
            }
            order.addItem(menuItem, entry.getValue());
        }

        // 2) desconto de promocao calculado pelo dominio, nunca recebido do cliente
        Promotion promotion = applyPromotion(command.promocaoId(), order);
        if (promotion == null) {
            order.calculateTotal();
        }

        // 3) baixa de estoque por unidade, com lock na linha
        String notes = "Baixa por pedido do canal " + command.canalPedido();
        for (Map.Entry<UUID, Integer> entry : quantities.entrySet()) {
            BigDecimal quantity = BigDecimal.valueOf(entry.getValue());
            InventoryGateway.ReservationResult result = inventoryGateway.reserve(
                    command.unidadeId(), entry.getKey(), quantity, notes);
            if (!result.reserved()) {
                throw new InsufficientStockException(entry.getKey(), result.available(), quantity);
            }
        }

        // 4) status inicial depende da forma de pagamento
        if (PaymentMethod.MOCK == command.formaPagamento()) {
            order.awaitPayment();
        } else {
            order.changeStatus(OrderStatus.RECEIVED);
        }

        Order saved = orderGateway.save(order);
        auditOrderCreated(saved, promotion, principal);

        return saved;
    }

    private Promotion applyPromotion(UUID promocaoId, Order order) {
        if (promocaoId == null) {
            return null;
        }

        Promotion promotion = promotionGateway.findById(promocaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Promocao", promocaoId));

        if (!Boolean.TRUE.equals(promotion.isValidFor(order))) {
            throw new PromotionNotApplicableException(
                    "Promocao " + promotion.getTitle() + " nao esta vigente ou nao atende ao valor minimo.");
        }

        order.applyDiscount(promotion.applyDiscount(order));
        order.setPromotion(promotion);

        return promotion;
    }

    private void auditOrderCreated(Order order, Promotion promotion, AuthenticatedUser principal) {
        String details = "canal=" + order.getOriginChannel() + " total=" + order.getTotalAmount();
        auditGateway.record(auditEntry(principal, AuditAction.ORDER_CREATED, order.getId(), details));

        if (promotion != null) {
            String promotionDetails = "promocao=" + promotion.getId()
                    + " desconto=" + order.getDiscountAmount();
            auditGateway.record(auditEntry(principal, AuditAction.PROMOTION_APPLIED, order.getId(),
                    promotionDetails));
        }
    }

    private static AuditEntry auditEntry(AuthenticatedUser principal, AuditAction action, UUID resourceId,
            String details) {
        if (principal == null) {
            return new AuditEntry(null, null, null, action, "ORDER", resourceId, details);
        }

        return new AuditEntry(principal.id(), principal.email(), principal.role(), action, "ORDER",
                resourceId, details);
    }

    private User resolveCustomer(UUID clienteId, AuthenticatedUser principal) {
        if (principal != null && UserRole.CUSTOMER == principal.role()) {
            return customerGateway.findById(principal.id())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente", principal.id()));
        }

        if (clienteId == null) {
            return null;
        }

        return customerGateway.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", clienteId));
    }

    /**
     * Agrupa linhas repetidas do mesmo produto somando as quantidades.
     *
     * <p>Sem isso, duas linhas do mesmo item seriam validadas isoladamente e a
     * soma poderia estourar o saldo da unidade.</p>
     */
    private static Map<UUID, Integer> mergeQuantities(List<CreateOrderItemCommand> itens) {
        Map<UUID, Integer> quantities = new LinkedHashMap<>();
        for (CreateOrderItemCommand item : itens) {
            quantities.merge(item.produtoId(), item.quantidade(), Integer::sum);
        }

        return quantities;
    }
}
