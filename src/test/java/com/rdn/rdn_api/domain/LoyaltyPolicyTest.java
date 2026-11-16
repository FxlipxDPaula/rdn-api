package com.rdn.rdn_api.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/**
 * Testes da conversao entre reais e pontos.
 */
class LoyaltyPolicyTest {

    @Test
    void acumulaUmPontoPorRealArredondandoParaBaixo() {
        assertThat(LoyaltyPolicy.earnedPointsFor(new BigDecimal("51.80"))).isEqualTo(51);
        assertThat(LoyaltyPolicy.earnedPointsFor(new BigDecimal("0.99"))).isZero();
    }

    @Test
    void totalNuloOuNaoPositivoNaoGeraPontos() {
        assertThat(LoyaltyPolicy.earnedPointsFor(null)).isZero();
        assertThat(LoyaltyPolicy.earnedPointsFor(BigDecimal.ZERO)).isZero();
        assertThat(LoyaltyPolicy.earnedPointsFor(new BigDecimal("-1.00"))).isZero();
    }

    @Test
    void convertePontosEmReaisComDuasCasas() {
        assertThat(LoyaltyPolicy.discountFor(100)).isEqualByComparingTo("10.00");
        assertThat(LoyaltyPolicy.discountFor(1)).isEqualByComparingTo("0.10");
        assertThat(LoyaltyPolicy.discountFor(0)).isEqualByComparingTo("0.00");
    }
}
