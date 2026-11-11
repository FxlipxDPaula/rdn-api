package com.rdn.rdn_api.infrastructure.persistence.mapper;

import com.rdn.rdn_api.domain.LoyaltyAccount;
import com.rdn.rdn_api.infrastructure.persistence.entity.LoyaltyAccountJpaEntity;

/**
 * Conversao entre {@link LoyaltyAccount} e {@link LoyaltyAccountJpaEntity}.
 */
public final class LoyaltyAccountMapper {

    private LoyaltyAccountMapper() {
    }

    public static LoyaltyAccount toDomain(LoyaltyAccountJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        LoyaltyAccount account = new LoyaltyAccount();
        account.setId(entity.getId());
        account.setCustomer(UserMapper.toDomain(entity.getCustomer()));
        account.setPointsBalance(entity.getPointsBalance());
        account.setConsentGiven(entity.getConsentGiven());
        account.setConsentTimestamp(entity.getConsentTimestamp());

        return account;
    }

    public static void copyScalarsToEntity(LoyaltyAccount domain,
            LoyaltyAccountJpaEntity entity) {
        entity.setId(domain.getId());
        entity.setPointsBalance(domain.getPointsBalance());
        entity.setConsentGiven(domain.getConsentGiven());
        entity.setConsentTimestamp(domain.getConsentTimestamp());
    }

}
