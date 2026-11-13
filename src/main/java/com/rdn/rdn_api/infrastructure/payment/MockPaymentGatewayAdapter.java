package com.rdn.rdn_api.infrastructure.payment;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.rdn.rdn_api.application.payment.ChargeRequest;
import com.rdn.rdn_api.application.payment.ChargeResult;
import com.rdn.rdn_api.application.payment.PaymentProvider;
import com.rdn.rdn_api.application.payment.SimulationMode;

/**
 * Provedor de pagamento simulado.
 *
 * <p>Nao existe chamada de rede: o desfecho vem do campo {@code simular} do
 * request, o que torna os testes deterministicos. O payload devolvido imita o
 * formato de um gateway real para exercitar o registro de auditoria
 * ({@code gateway_raw_response}).</p>
 */
@Component
public class MockPaymentGatewayAdapter implements PaymentProvider {

    private static final String PROVIDER = "mock";

    @Override
    public ChargeResult charge(ChargeRequest request) {
        SimulationMode mode = request.simular() == null ? SimulationMode.APROVADO : request.simular();
        String transactionId = "MOCK-" + UUID.randomUUID();

        return switch (mode) {
            case APROVADO -> new ChargeResult(true, false, transactionId,
                    payload(mode, request, transactionId));
            case RECUSADO -> new ChargeResult(false, false, null,
                    payload(mode, request, null));
            case FALHA -> new ChargeResult(false, true, null,
                    payload(mode, request, null));
        };
    }

    private static String payload(SimulationMode mode, ChargeRequest request, String transactionId) {
        return "{\"provider\":\"" + PROVIDER
                + "\",\"simulation\":\"" + mode
                + "\",\"orderId\":\"" + request.orderId()
                + "\",\"amount\":" + request.amount()
                + ",\"idempotencyKey\":\"" + request.idempotencyKey()
                + "\",\"transactionId\":\"" + (transactionId == null ? "" : transactionId)
                + "\"}";
    }
}
