package com.rdn.rdn_api.infrastructure.persistence.mapper;

import com.rdn.rdn_api.domain.LoyaltyTransaction;
import com.rdn.rdn_api.infrastructure.persistence.entity.LoyaltyTransactionJpaEntity;

/**
 * Conversao entre {@link LoyaltyTransaction} e
 * {@link LoyaltyTransactionJpaEntity}.
 */
public final class LoyaltyTransactionMapper {

    private LoyaltyTransactionMapper() {
    }

    public static LoyaltyTransaction toDomain(LoyaltyTransactionJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        LoyaltyTransaction transaction = new LoyaltyTransaction();
        transaction.setId(entity.getId());
        transaction.setLoyaltyAccount(
                LoyaltyAccountMapper.toDomain(entity.getLoyaltyAccount()));
        transaction.setOrder(OrderMapper.toDomain(entity.getOrder()));
        transaction.setPoints(entity.getPoints());
        transaction.setType(entity.getType());
        transaction.setCreatedAt(entity.getCreatedAt());

        return transaction;
    }

    public static void copyScalarsToEntity(LoyaltyTransaction domain,
            LoyaltyTransactionJpaEntity entity) {
        entity.setId(domain.getId());
        entity.setPoints(domain.getPoints());
        entity.setType(domain.getType());
        entity.setCreatedAt(domain.getCreatedAt());
    }

}
