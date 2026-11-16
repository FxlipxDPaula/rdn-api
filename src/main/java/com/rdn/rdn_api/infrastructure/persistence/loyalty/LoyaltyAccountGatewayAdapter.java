package com.rdn.rdn_api.infrastructure.persistence.loyalty;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rdn.rdn_api.application.loyalty.LoyaltyAccountGateway;
import com.rdn.rdn_api.domain.LoyaltyAccount;
import com.rdn.rdn_api.domain.LoyaltyTransaction;
import com.rdn.rdn_api.domain.TransactionType;
import com.rdn.rdn_api.infrastructure.persistence.entity.LoyaltyAccountJpaEntity;
import com.rdn.rdn_api.infrastructure.persistence.entity.LoyaltyTransactionJpaEntity;
import com.rdn.rdn_api.infrastructure.persistence.mapper.LoyaltyAccountMapper;
import com.rdn.rdn_api.infrastructure.persistence.mapper.LoyaltyTransactionMapper;
import com.rdn.rdn_api.infrastructure.repository.LoyaltyAccountJpaRepository;
import com.rdn.rdn_api.infrastructure.repository.LoyaltyTransactionJpaRepository;
import com.rdn.rdn_api.infrastructure.repository.OrderJpaRepository;
import com.rdn.rdn_api.infrastructure.repository.UserJpaRepository;

/**
 * Adaptador de {@link LoyaltyAccountGateway} sobre Spring Data JPA.
 */
@Component
public class LoyaltyAccountGatewayAdapter implements LoyaltyAccountGateway {

    private final LoyaltyAccountJpaRepository accountRepository;

    private final LoyaltyTransactionJpaRepository transactionRepository;

    private final UserJpaRepository userRepository;

    private final OrderJpaRepository orderRepository;

    public LoyaltyAccountGatewayAdapter(LoyaltyAccountJpaRepository accountRepository,
            LoyaltyTransactionJpaRepository transactionRepository, UserJpaRepository userRepository,
            OrderJpaRepository orderRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LoyaltyAccount> findByCustomerId(UUID customerId) {
        return accountRepository.findByCustomerId(customerId).map(LoyaltyAccountMapper::toDomain);
    }

    @Override
    @Transactional
    public Optional<LoyaltyAccount> findForUpdateByCustomerId(UUID customerId) {
        return accountRepository.findForUpdateByCustomerId(customerId).map(LoyaltyAccountMapper::toDomain);
    }

    @Override
    @Transactional
    public LoyaltyAccount saveAccount(LoyaltyAccount account) {
        LoyaltyAccountJpaEntity entity;
        if (account.getId() == null) {
            entity = new LoyaltyAccountJpaEntity();
            if (account.getCustomer() != null) {
                entity.setCustomer(userRepository.getReferenceById(account.getCustomer().getId()));
            }
        } else {
            entity = accountRepository.findById(account.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "conta de fidelidade " + account.getId() + " nao encontrada"));
        }

        LoyaltyAccountMapper.copyScalarsToEntity(account, entity);

        return LoyaltyAccountMapper.toDomain(accountRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public LoyaltyTransaction saveTransaction(LoyaltyTransaction transaction) {
        LoyaltyTransactionJpaEntity entity = LoyaltyTransactionMapper.toEntity(transaction);
        entity.setLoyaltyAccount(
                accountRepository.getReferenceById(transaction.getLoyaltyAccount().getId()));
        if (transaction.getOrder() != null) {
            entity.setOrder(orderRepository.getReferenceById(transaction.getOrder().getId()));
        }

        return LoyaltyTransactionMapper.toDomain(transactionRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoyaltyTransaction> findTransactions(UUID customerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Optional<LoyaltyAccountJpaEntity> account = accountRepository.findByCustomerId(customerId);
        if (account.isEmpty()) {
            return Page.empty(pageable);
        }

        return transactionRepository
                .findByLoyaltyAccountIdOrderByCreatedAtDesc(account.get().getId(), pageable)
                .map(LoyaltyTransactionMapper::toDomainShallow);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsTransaction(UUID orderId, TransactionType type) {
        return transactionRepository.existsByOrderIdAndType(orderId, type);
    }
}
