package com.rdn.rdn_api.infrastructure.persistence.mapper;

import com.rdn.rdn_api.domain.LoyaltyTransaction;
import com.rdn.rdn_api.domain.Order;
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

    /**
     * Converte o lancamento sem carregar conta nem pedido.
     *
     * <p>Usado no extrato: o pedido vira apenas uma referencia com id, sem
     * disparar consultas adicionais para cada linha.</p>
     *
     * @param entity entidade de persistencia
     * @return lancamento de dominio com referencia de pedido
     */
    public static LoyaltyTransaction toDomainShallow(LoyaltyTransactionJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        LoyaltyTransaction transaction = new LoyaltyTransaction();
        transaction.setId(entity.getId());
        transaction.setPoints(entity.getPoints());
        transaction.setType(entity.getType());
        transaction.setCreatedAt(entity.getCreatedAt());

        if (entity.getOrder() != null) {
            Order orderReference = new Order();
            orderReference.setId(entity.getOrder().getId());
            transaction.setOrder(orderReference);
        }

        return transaction;
    }

    public static LoyaltyTransactionJpaEntity toEntity(LoyaltyTransaction domain) {
        if (domain == null) {
            return null;
        }

        LoyaltyTransactionJpaEntity entity = new LoyaltyTransactionJpaEntity();
        copyScalarsToEntity(domain, entity);

        return entity;
    }

    public static void copyScalarsToEntity(LoyaltyTransaction domain,
            LoyaltyTransactionJpaEntity entity) {
        entity.setId(domain.getId());
        entity.setPoints(domain.getPoints());
        entity.setType(domain.getType());
        entity.setCreatedAt(domain.getCreatedAt());
    }

}
