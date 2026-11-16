package com.rdn.rdn_api.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/**
 * Testes das regras de vigencia e desconto da campanha.
 */
class PromotionTest {

    @Test
    void percentualCalculaSobreOSubtotalDosItens() {
        Order order = orderWithBurger(2);
        Promotion promotion = promotion(DiscountType.PERCENTAGE, "10.00", "30.00", null);

        assertThat(promotion.isValidFor(order)).isTrue();
        assertThat(promotion.applyDiscount(order)).isEqualByComparingTo("5.18");
    }

    @Test
    void valorFixoEIgnoradoQuandoInativo() {
        Order order = orderWithBurger(2);
        Promotion promotion = promotion(DiscountType.FIXED_AMOUNT, "5.00", "30.00", null);
        promotion.setIsActive(Boolean.FALSE);

        assertThat(promotion.isValidFor(order)).isFalse();
        assertThat(promotion.applyDiscount(order)).isEqualByComparingTo("0.00");
    }

    @Test
    void campanhaExpiradaNaoEhValida() {
        Order order = orderWithBurger(2);
        Promotion promotion = promotion(DiscountType.PERCENTAGE, "10.00", "30.00",
                LocalDateTime.now().minusDays(1));

        assertThat(promotion.isValidFor(order)).isFalse();
    }

    @Test
    void pedidoAbaixoDoMinimoNaoRecebeDesconto() {
        Order order = orderWithBurger(2);
        Promotion promotion = promotion(DiscountType.PERCENTAGE, "10.00", "60.00", null);

        assertThat(promotion.isValidFor(order)).isFalse();
    }

    @Test
    void descontoNuncaUltrapassaOSubtotal() {
        Order order = orderWithBurger(1);
        Promotion promotion = promotion(DiscountType.FIXED_AMOUNT, "100.00", null, null);

        order.applyDiscount(promotion.applyDiscount(order));

        assertThat(order.getDiscountAmount()).isEqualByComparingTo("25.90");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void janelaSemDatasEstaSempreVigente() {
        Promotion promotion = promotion(DiscountType.PERCENTAGE, "10.00", null, null);

        assertThat(promotion.isWithinValidity(LocalDateTime.now())).isTrue();
    }

    private static Order orderWithBurger(int quantity) {
        MenuItem burger = new MenuItem();
        burger.setName("X-Burger");
        burger.setPrice(new BigDecimal("25.90"));
        burger.setIsAvailable(Boolean.TRUE);

        Order order = new Order();
        order.addItem(burger, quantity);
        order.calculateTotal();

        return order;
    }

    private static Promotion promotion(DiscountType type, String value, String minimum,
            LocalDateTime endDate) {
        Promotion promotion = new Promotion();
        promotion.setTitle("Campanha Teste");
        promotion.setDiscountType(type);
        promotion.setDiscountValue(new BigDecimal(value));
        promotion.setMinOrderValue(minimum == null ? null : new BigDecimal(minimum));
        promotion.setEndDate(endDate);
        promotion.setIsActive(Boolean.TRUE);

        return promotion;
    }
}
