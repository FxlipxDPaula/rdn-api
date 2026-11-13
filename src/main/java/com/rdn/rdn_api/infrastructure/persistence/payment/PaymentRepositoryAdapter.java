package com.rdn.rdn_api.infrastructure.persistence.payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rdn.rdn_api.application.payment.PaymentRepository;
import com.rdn.rdn_api.domain.Payment;
import com.rdn.rdn_api.infrastructure.persistence.entity.PaymentJpaEntity;
import com.rdn.rdn_api.infrastructure.persistence.mapper.PaymentMapper;
import com.rdn.rdn_api.infrastructure.repository.OrderJpaRepository;
import com.rdn.rdn_api.infrastructure.repository.PaymentJpaRepository;

/**
 * Adaptador de {@link PaymentRepository} sobre {@code payments}.
 */
@Component
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final PaymentJpaRepository paymentRepository;

    private final OrderJpaRepository orderRepository;

    public PaymentRepositoryAdapter(PaymentJpaRepository paymentRepository,
            OrderJpaRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional
    public Payment save(Payment payment) {
        PaymentJpaEntity entity = PaymentMapper.toEntity(payment);
        if (payment.getId() != null) {
            PaymentJpaEntity existing = paymentRepository.findById(payment.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "pagamento " + payment.getId() + " nao encontrado na atualizacao"));
            entity = existing;
            PaymentMapper.copyScalarsToEntity(payment, entity);
        }

        entity.setOrder(orderRepository.getReferenceById(payment.getOrder().getId()));

        return PaymentMapper.toDomain(paymentRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        return paymentRepository.findByIdempotencyKey(idempotencyKey).map(PaymentMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> findByOrderId(UUID orderId) {
        return paymentRepository.findByOrderIdOrderByRequestedAtDesc(orderId).stream()
                .map(PaymentMapper::toDomain)
                .toList();
    }
}
