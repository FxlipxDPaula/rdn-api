package com.rdn.rdn_api.api.promotion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.rdn.rdn_api.application.promotion.PromotionView;
import com.rdn.rdn_api.domain.DiscountType;

/**
 * Campanha promocional na resposta HTTP.
 */
public record PromotionResponse(UUID promocaoId, String titulo, String descricao,
        DiscountType tipoDesconto, BigDecimal valorDesconto, BigDecimal valorMinimoPedido,
        LocalDateTime inicioEm, LocalDateTime fimEm, boolean vigente) {

    public static PromotionResponse from(PromotionView view) {
        return new PromotionResponse(view.promocaoId(), view.titulo(), view.descricao(),
                view.tipoDesconto(), view.valorDesconto(), view.valorMinimoPedido(), view.inicioEm(),
                view.fimEm(), view.vigente());
    }
}
