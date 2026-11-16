package com.rdn.rdn_api.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Regras de conversao entre reais e pontos do programa de fidelidade.
 *
 * <p>Os valores sao deliberadamente fixos e deterministicos para os testes:
 * o cliente ganha 1 ponto por real pago (arredondado para baixo) e cada ponto
 * vale R$ 0,10 no resgate. Uma campanha futura pode externalizar esses numeros
 * em configuracao, mas o calculo continua sendo responsabilidade do dominio.</p>
 */
public final class LoyaltyPolicy {

    /** Pontos ganhos por real pago. */
    public static final int POINTS_PER_REAL_EARNED = 1;

    /** Valor em reais de um ponto no momento do resgate. */
    public static final BigDecimal REAL_PER_POINT_REDEEMED = new BigDecimal("0.10");

    private LoyaltyPolicy() {
    }

    /**
     * Calcula quantos pontos um pedido pago gera.
     *
     * @param total valor efetivamente pago
     * @return pontos inteiros, arredondados para baixo; zero para total nulo ou nao positivo
     */
    public static int earnedPointsFor(BigDecimal total) {
        if (total == null || total.signum() <= 0) {
            return 0;
        }

        return total.setScale(0, RoundingMode.FLOOR).intValue() * POINTS_PER_REAL_EARNED;
    }

    /**
     * Converte pontos em desconto monetario.
     *
     * @param points quantidade de pontos a resgatar
     * @return desconto com duas casas; zero para quantidade nula ou nao positiva
     */
    public static BigDecimal discountFor(int points) {
        if (points <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return REAL_PER_POINT_REDEEMED.multiply(BigDecimal.valueOf(points))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
