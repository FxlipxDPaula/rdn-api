package com.rdn.rdn_api.application.promotion;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rdn.rdn_api.domain.Promotion;

/**
 * Caso de uso de consulta das campanhas promocionais.
 */
@Service
public class PromotionService {

    private final PromotionGateway promotionGateway;

    public PromotionService(PromotionGateway promotionGateway) {
        this.promotionGateway = promotionGateway;
    }

    /**
     * Lista as campanhas ativas.
     *
     * @return campanhas ativas
     */
    @Transactional(readOnly = true)
    public List<Promotion> listActive() {
        return promotionGateway.findActive();
    }
}
