package com.rdn.rdn_api.infrastructure.persistence.mapper;

import com.rdn.rdn_api.domain.Promotion;
import com.rdn.rdn_api.infrastructure.persistence.entity.PromotionJpaEntity;

/**
 * Conversao entre {@link Promotion} e {@link PromotionJpaEntity}.
 */
public final class PromotionMapper {

    private PromotionMapper() {
    }

    public static Promotion toDomain(PromotionJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        Promotion promotion = new Promotion();
        promotion.setId(entity.getId());
        promotion.setTitle(entity.getTitle());
        promotion.setDescription(entity.getDescription());
        promotion.setDiscountType(entity.getDiscountType());
        promotion.setDiscountValue(entity.getDiscountValue());
        promotion.setStartDate(entity.getStartDate());
        promotion.setEndDate(entity.getEndDate());
        promotion.setMinOrderValue(entity.getMinOrderValue());
        promotion.setIsActive(entity.getIsActive());

        return promotion;
    }

    public static PromotionJpaEntity toEntity(Promotion domain) {
        if (domain == null) {
            return null;
        }

        PromotionJpaEntity entity = new PromotionJpaEntity();
        copyScalarsToEntity(domain, entity);

        return entity;
    }

    public static void copyScalarsToEntity(Promotion domain, PromotionJpaEntity entity) {
        entity.setId(domain.getId());
        entity.setTitle(domain.getTitle());
        entity.setDescription(domain.getDescription());
        entity.setDiscountType(domain.getDiscountType());
        entity.setDiscountValue(domain.getDiscountValue());
        entity.setStartDate(domain.getStartDate());
        entity.setEndDate(domain.getEndDate());
        entity.setMinOrderValue(domain.getMinOrderValue());
        entity.setIsActive(domain.getIsActive());
    }

}
