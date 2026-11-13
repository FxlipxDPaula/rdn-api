package com.rdn.rdn_api.api.payment;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rdn.rdn_api.application.auth.AuthenticatedUser;
import com.rdn.rdn_api.application.payment.PaymentCommand;
import com.rdn.rdn_api.application.payment.PaymentResult;
import com.rdn.rdn_api.application.payment.QueryPaymentService;
import com.rdn.rdn_api.application.payment.RequestPaymentService;

import jakarta.validation.Valid;

/**
 * Endpoints de pagamento do pedido.
 *
 * <p>A cobranca e idempotente por cabecalho: repetir a mesma
 * {@code Idempotency-Key} devolve {@code 200} com a tentativa original em vez de
 * criar uma segunda cobranca.</p>
 */
@RestController
@RequestMapping("/pedidos/{pedidoId}/pagamentos")
public class PaymentController {

    private final RequestPaymentService requestPaymentService;

    private final QueryPaymentService queryPaymentService;

    public PaymentController(RequestPaymentService requestPaymentService,
            QueryPaymentService queryPaymentService) {
        this.requestPaymentService = requestPaymentService;
        this.queryPaymentService = queryPaymentService;
    }

    /**
     * Solicita o pagamento do pedido ao provedor simulado.
     *
     * @param pedidoId       pedido a cobrar
     * @param idempotencyKey chave de idempotencia (obrigatoria)
     * @param request        forma de pagamento e desfecho simulado
     * @param principal      usuario autenticado
     * @return {@code 201} na primeira tentativa, {@code 200} em replay
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> request(@PathVariable UUID pedidoId,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody RequestPaymentRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        PaymentCommand command = new PaymentCommand(request.formaPagamento(), request.simular());
        PaymentResult result = requestPaymentService.request(pedidoId, idempotencyKey, command, principal);

        HttpStatus status = result.replay() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(PaymentResponse.from(result.payment()));
    }

    /**
     * Lista o historico de tentativas do pedido.
     *
     * @param pedidoId  identificador do pedido
     * @param principal usuario autenticado
     * @return tentativas, da mais recente para a mais antiga
     */
    @GetMapping
    public List<PaymentResponse> history(@PathVariable UUID pedidoId,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        return queryPaymentService.listByOrder(pedidoId, principal).stream()
                .map(PaymentResponse::from)
                .toList();
    }
}
