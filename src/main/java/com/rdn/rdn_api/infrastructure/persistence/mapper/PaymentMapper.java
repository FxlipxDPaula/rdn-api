package com.rdn.rdn_api.infrastructure.persistence.mapper;

import com.rdn.rdn_api.domain.Payment;
import com.rdn.rdn_api.infrastructure.persistence.entity.PaymentJpaEntity;

/**
 * Conversao entre {@link Payment} e {@link PaymentJpaEntity}.
 */
public final class PaymentMapper {

    private PaymentMapper() {
    }

    public static Payment toDomain(PaymentJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        Payment payment = new Payment();
        payment.setId(entity.getId());
        payment.setOrder(OrderMapper.toDomain(entity.getOrder()));
        payment.setAmount(entity.getAmount());
        payment.setMethod(entity.getMethod());
        payment.setStatus(entity.getStatus());
        payment.setGatewayTransactionId(entity.getGatewayTransactionId());
        payment.setGatewayRawResponse(entity.getGatewayRawResponse());
        payment.setIdempotencyKey(entity.getIdempotencyKey());
        payment.setRequestedAt(entity.getRequestedAt());
        payment.setProcessedAt(entity.getProcessedAt());

        return payment;
    }

    public static PaymentJpaEntity toEntity(Payment domain) {
        if (domain == null) {
            return null;
        }

        PaymentJpaEntity entity = new PaymentJpaEntity();
        copyScalarsToEntity(domain, entity);

        return entity;
    }

    public static void copyScalarsToEntity(Payment domain, PaymentJpaEntity entity) {
        entity.setId(domain.getId());
        entity.setAmount(domain.getAmount());
        entity.setMethod(domain.getMethod());
        entity.setStatus(domain.getStatus());
        entity.setGatewayTransactionId(domain.getGatewayTransactionId());
        entity.setGatewayRawResponse(domain.getGatewayRawResponse());
        entity.setIdempotencyKey(domain.getIdempotencyKey());
        entity.setRequestedAt(domain.getRequestedAt());
        entity.setProcessedAt(domain.getProcessedAt());
    }

}
