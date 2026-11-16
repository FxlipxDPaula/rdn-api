package com.rdn.rdn_api.application.promotion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.rdn.rdn_api.domain.DiscountType;
import com.rdn.rdn_api.domain.Promotion;

/**
 * Visao de campanha para a API.
 *
 * @param promocaoId       identificador
 * @param titulo           titulo
 * @param descricao        descricao
 * @param tipoDesconto     percentual ou valor fixo
 * @param valorDesconto    valor do desconto
 * @param valorMinimoPedido valor minimo para a campanha valer
 * @param inicioEm         inicio da janela
 * @param fimEm            fim da janela
 * @param vigente          indicador calculado no instante da consulta
 */
public record PromotionView(UUID promocaoId, String titulo, String descricao,
        DiscountType tipoDesconto, BigDecimal valorDesconto, BigDecimal valorMinimoPedido,
        LocalDateTime inicioEm, LocalDateTime fimEm, boolean vigente) {

    /**
     * Converte a campanha aplicando a janela de vigencia.
     *
     * @param promotion campanha de dominio
     * @param reference instante de referencia
     * @return visao correspondente
     */
    public static PromotionView from(Promotion promotion, LocalDateTime reference) {
        if (promotion == null) {
            return null;
        }

        boolean active = Boolean.TRUE.equals(promotion.getIsActive());
        boolean withinWindow = promotion.isWithinValidity(reference);

        return new PromotionView(
                promotion.getId(),
                promotion.getTitle(),
                promotion.getDescription(),
                promotion.getDiscountType(),
                promotion.getDiscountValue(),
                promotion.getMinOrderValue(),
                promotion.getStartDate(),
                promotion.getEndDate(),
                active && withinWindow);
    }
}
