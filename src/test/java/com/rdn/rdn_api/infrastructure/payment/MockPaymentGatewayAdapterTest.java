package com.rdn.rdn_api.infrastructure.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.rdn.rdn_api.application.payment.ChargeRequest;
import com.rdn.rdn_api.application.payment.ChargeResult;
import com.rdn.rdn_api.application.payment.SimulationMode;
import com.rdn.rdn_api.domain.PaymentMethod;

/**
 * Testes do provedor simulado.
 */
class MockPaymentGatewayAdapterTest {

    private final MockPaymentGatewayAdapter adapter = new MockPaymentGatewayAdapter();

    @Test
    void aprovaCobrancaComIdentificadorDeTransacao() {
        ChargeResult result = adapter.charge(request(SimulationMode.APROVADO));

        assertThat(result.approved()).isTrue();
        assertThat(result.failed()).isFalse();
        assertThat(result.transactionId()).startsWith("MOCK-");
        assertThat(result.rawResponse()).contains("\"simulation\":\"APROVADO\"");
    }

    @Test
    void recusaCobrancaSemIdentificadorDeTransacao() {
        ChargeResult result = adapter.charge(request(SimulationMode.RECUSADO));

        assertThat(result.approved()).isFalse();
        assertThat(result.failed()).isFalse();
        assertThat(result.transactionId()).isNull();
    }

    @Test
    void sinalizaFalhaTecnica() {
        ChargeResult result = adapter.charge(request(SimulationMode.FALHA));

        assertThat(result.approved()).isFalse();
        assertThat(result.failed()).isTrue();
    }

    @Test
    void assumeAprovacaoQuandoNaoHaSimulacao() {
        ChargeResult result = adapter.charge(request(null));

        assertThat(result.approved()).isTrue();
    }

    private static ChargeRequest request(SimulationMode mode) {
        return new ChargeRequest(UUID.randomUUID(), new BigDecimal("51.80"), PaymentMethod.MOCK,
                "chave-teste", mode);
    }
}
