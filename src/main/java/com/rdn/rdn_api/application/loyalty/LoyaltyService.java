package com.rdn.rdn_api.application.loyalty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rdn.rdn_api.application.audit.AuditAction;
import com.rdn.rdn_api.application.audit.AuditEntry;
import com.rdn.rdn_api.application.audit.AuditGateway;
import com.rdn.rdn_api.application.auth.AuthenticatedUser;
import com.rdn.rdn_api.application.loyalty.exception.ConsentRequiredException;
import com.rdn.rdn_api.application.loyalty.exception.InsufficientPointsException;
import com.rdn.rdn_api.application.loyalty.exception.RedemptionAlreadyAppliedException;
import com.rdn.rdn_api.application.loyalty.exception.RedemptionExceedsOrderTotalException;
import com.rdn.rdn_api.application.loyalty.exception.RedemptionNotAllowedException;
import com.rdn.rdn_api.application.order.OrderGateway;
import com.rdn.rdn_api.application.order.exception.OperationNotAllowedException;
import com.rdn.rdn_api.application.order.exception.ResourceNotFoundException;
import com.rdn.rdn_api.domain.LoyaltyAccount;
import com.rdn.rdn_api.domain.LoyaltyPolicy;
import com.rdn.rdn_api.domain.LoyaltyTransaction;
import com.rdn.rdn_api.domain.Order;
import com.rdn.rdn_api.domain.OrderStatus;
import com.rdn.rdn_api.domain.TransactionType;
import com.rdn.rdn_api.domain.User;
import com.rdn.rdn_api.domain.UserRole;

/**
 * Caso de uso do programa de fidelidade.
 *
 * <p>Consentimento e pre-condicao: sem opt-in nao ha acumulo nem resgate. O
 * resgate debita a conta com lock pessimista e aplica o valor como desconto no
 * pedido, que precisa estar aguardando pagamento.</p>
 */
@Service
public class LoyaltyService {

    private static final int MAX_PAGE_SIZE = 100;

    private final LoyaltyAccountGateway accountGateway;

    private final OrderGateway orderGateway;

    private final AuditGateway auditGateway;

    public LoyaltyService(LoyaltyAccountGateway accountGateway, OrderGateway orderGateway,
            AuditGateway auditGateway) {
        this.accountGateway = accountGateway;
        this.orderGateway = orderGateway;
        this.auditGateway = auditGateway;
    }

    /**
     * Consulta a conta do cliente autenticado.
     *
     * @param principal cliente autenticado
     * @return conta, ou visao neutra quando o cliente ainda nao consentiu
     */
    @Transactional(readOnly = true)
    public LoyaltyAccountView getAccount(AuthenticatedUser principal) {
        UUID customerId = requireCustomer(principal);

        return accountGateway.findByCustomerId(customerId)
                .map(LoyaltyAccountView::from)
                .orElseGet(() -> LoyaltyAccountView.empty(customerId));
    }

    /**
     * Registra ou revoga o consentimento do cliente.
     *
     * @param consent   {@code true} para opt-in, {@code false} para opt-out
     * @param principal cliente autenticado
     * @return conta atualizada
     */
    @Transactional
    public LoyaltyAccountView setConsent(boolean consent, AuthenticatedUser principal) {
        UUID customerId = requireCustomer(principal);
        Optional<LoyaltyAccount> existing = accountGateway.findByCustomerId(customerId);

        if (existing.isEmpty()) {
            if (!consent) {
                return LoyaltyAccountView.empty(customerId);
            }

            LoyaltyAccount account = newAccount(customerId);
            account.optIn();
            account = accountGateway.saveAccount(account);
            auditConsent(principal, account.getId(), AuditAction.LOYALTY_CONSENT_GRANTED, true);

            return LoyaltyAccountView.from(account);
        }

        LoyaltyAccount account = existing.get();
        if (consent && !account.hasConsent()) {
            account.optIn();
            account = accountGateway.saveAccount(account);
            auditConsent(principal, account.getId(), AuditAction.LOYALTY_CONSENT_GRANTED, true);
        } else if (!consent && account.hasConsent()) {
            account.optOut();
            account = accountGateway.saveAccount(account);
            auditConsent(principal, account.getId(), AuditAction.LOYALTY_CONSENT_REVOKED, false);
        }

        return LoyaltyAccountView.from(account);
    }

    /**
     * Lista o extrato do cliente autenticado.
     *
     * @param principal cliente autenticado
     * @param page      pagina (base zero)
     * @param size      tamanho
     * @return pagina de lancamentos
     */
    @Transactional(readOnly = true)
    public Page<LoyaltyTransaction> listTransactions(AuthenticatedUser principal, int page, int size) {
        UUID customerId = requireCustomer(principal);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        return accountGateway.findTransactions(customerId, safePage, safeSize);
    }

    /**
     * Resgata pontos aplicando o valor como desconto no pedido.
     *
     * @param command   pedido e quantidade de pontos
     * @param principal cliente autenticado
     * @return resumo do resgate
     */
    @Transactional
    public RedeemPointsResult redeem(RedeemPointsCommand command, AuthenticatedUser principal) {
        UUID customerId = requireCustomer(principal);
        if (command == null || command.pedidoId() == null) {
            throw new IllegalArgumentException("pedidoId e obrigatorio");
        }
        if (command.pontos() <= 0) {
            throw new IllegalArgumentException("pontos devem ser positivos");
        }

        LoyaltyAccount account = accountGateway.findForUpdateByCustomerId(customerId)
                .orElseThrow(ConsentRequiredException::new);
        if (!account.hasConsent()) {
            throw new ConsentRequiredException();
        }

        Order order = orderGateway.findWithItemsById(command.pedidoId())
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", command.pedidoId()));

        checkOwnership(order, customerId);
        if (OrderStatus.AGUARDANDO_PAGAMENTO != order.getStatus()) {
            throw new RedemptionNotAllowedException(order.getId(), order.getStatus());
        }
        if (accountGateway.existsTransaction(order.getId(), TransactionType.REDEEMED)) {
            throw new RedemptionAlreadyAppliedException(order.getId());
        }

        int balance = account.getPointsBalance() == null ? 0 : account.getPointsBalance();
        if (balance < command.pontos()) {
            throw new InsufficientPointsException(balance, command.pontos());
        }

        BigDecimal discount = LoyaltyPolicy.discountFor(command.pontos());
        BigDecimal currentDiscount = order.getDiscountAmount() == null
                ? BigDecimal.ZERO
                : order.getDiscountAmount();
        BigDecimal remaining = order.subtotal().subtract(currentDiscount);
        if (discount.compareTo(remaining) > 0) {
            throw new RedemptionExceedsOrderTotalException(discount, remaining);
        }

        if (!Boolean.TRUE.equals(account.redeemPoints(command.pontos()))) {
            throw new InsufficientPointsException(balance, command.pontos());
        }
        LoyaltyAccount savedAccount = accountGateway.saveAccount(account);

        order.addDiscount(discount);
        Order savedOrder = orderGateway.save(order);

        LoyaltyTransaction transaction = LoyaltyTransaction.redeemed(savedAccount, savedOrder,
                command.pontos(), LocalDateTime.now());
        accountGateway.saveTransaction(transaction);

        audit(principal, AuditAction.LOYALTY_POINTS_REDEEMED, savedAccount.getId(),
                "pedido=" + savedOrder.getId() + " pontos=" + command.pontos() + " desconto=" + discount);

        int newBalance = savedAccount.getPointsBalance() == null ? 0 : savedAccount.getPointsBalance();
        return new RedeemPointsResult(command.pontos(), discount, newBalance, savedOrder.getTotalAmount());
    }

    /**
     * Acumula pontos quando o pagamento do pedido e aprovado.
     *
     * <p>Idempotente por pedido: um segundo evento nao gera novo lancamento
     * {@code EARNED}. Cliente sem conta ou sem consentimento nao acumula.</p>
     *
     * @param order     pedido pago
     * @param principal usuario que confirmou o pagamento
     * @return pontos acumulados, ou zero quando nao houve acumulo
     */
    @Transactional
    public int awardForApprovedOrder(Order order, AuthenticatedUser principal) {
        if (order == null || order.getCustomer() == null || order.getId() == null) {
            return 0;
        }

        UUID customerId = order.getCustomer().getId();
        Optional<LoyaltyAccount> existing = accountGateway.findForUpdateByCustomerId(customerId);
        if (existing.isEmpty()) {
            return 0;
        }

        LoyaltyAccount account = existing.get();
        if (!account.hasConsent()) {
            return 0;
        }
        if (accountGateway.existsTransaction(order.getId(), TransactionType.EARNED)) {
            return 0;
        }

        int points = LoyaltyPolicy.earnedPointsFor(order.getTotalAmount());
        if (points <= 0) {
            return 0;
        }

        account.addPoints(points);
        LoyaltyAccount savedAccount = accountGateway.saveAccount(account);

        LoyaltyTransaction transaction = LoyaltyTransaction.earned(savedAccount, order, points,
                LocalDateTime.now());
        accountGateway.saveTransaction(transaction);

        audit(principal, AuditAction.LOYALTY_POINTS_EARNED, savedAccount.getId(),
                "pedido=" + order.getId() + " pontos=" + points);

        return points;
    }

    private static UUID requireCustomer(AuthenticatedUser principal) {
        if (principal == null || principal.id() == null || UserRole.CUSTOMER != principal.role()) {
            throw new OperationNotAllowedException("Recurso disponivel apenas para clientes.");
        }

        return principal.id();
    }

    private static LoyaltyAccount newAccount(UUID customerId) {
        User customer = new User();
        customer.setId(customerId);

        LoyaltyAccount account = new LoyaltyAccount();
        account.setCustomer(customer);
        account.setPointsBalance(0);
        account.setConsentGiven(Boolean.FALSE);

        return account;
    }

    private static void checkOwnership(Order order, UUID customerId) {
        boolean owner = order.getCustomer() != null && customerId.equals(order.getCustomer().getId());
        if (!owner) {
            throw new OperationNotAllowedException("Pedido pertence a outro cliente.");
        }
    }

    private void auditConsent(AuthenticatedUser principal, UUID accountId, AuditAction action,
            boolean consent) {
        audit(principal, action, accountId, "consentimento=" + consent);
    }

    private void audit(AuthenticatedUser principal, AuditAction action, UUID resourceId, String details) {
        UUID actorId = principal == null ? null : principal.id();
        String actorEmail = principal == null ? null : principal.email();
        UserRole actorRole = principal == null ? null : principal.role();

        auditGateway.record(new AuditEntry(actorId, actorEmail, actorRole, action, "LOYALTY_ACCOUNT",
                resourceId, details));
    }
}
