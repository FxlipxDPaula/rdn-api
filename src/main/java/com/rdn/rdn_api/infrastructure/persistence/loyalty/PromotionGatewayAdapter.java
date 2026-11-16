package com.rdn.rdn_api.infrastructure.persistence.loyalty;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rdn.rdn_api.application.promotion.PromotionGateway;
import com.rdn.rdn_api.domain.Promotion;
import com.rdn.rdn_api.infrastructure.persistence.mapper.PromotionMapper;
import com.rdn.rdn_api.infrastructure.repository.PromotionJpaRepository;

/**
 * Adaptador de {@link PromotionGateway} sobre Spring Data JPA.
 */
@Component
public class PromotionGatewayAdapter implements PromotionGateway {

    private final PromotionJpaRepository promotionRepository;

    public PromotionGatewayAdapter(PromotionJpaRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Promotion> findById(UUID id) {
        return promotionRepository.findById(id).map(PromotionMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Promotion> findActive() {
        return promotionRepository.findByIsActiveTrue().stream()
                .map(PromotionMapper::toDomain)
                .toList();
    }
}
