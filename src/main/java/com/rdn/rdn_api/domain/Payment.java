package com.rdn.rdn_api.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro de uma solicitacao de pagamento de um pedido.
 *
 * <p>POJO de dominio: guarda o resultado da chamada ao provedor externo
 * (simulado) sem conhecer detalhes de HTTP ou serializacao. O pagamento e
 * desacoplado do pedido: um pedido pode ter mais de uma tentativa, e cada
 * tentativa ocupa uma linha propria.</p>
 *
 * <p>A chave de idempotencia ({@code idempotencyKey}) identifica a tentativa
 * perante o provedor e impede que uma repeticao de requisicao gere cobranca
 * duplicada.</p>
 */
public class Payment {

    private UUID id;

    private Order order;

    private BigDecimal amount;

    private PaymentMethod method;

    private PaymentStatus status = PaymentStatus.PENDING;

    private String gatewayTransactionId;

    private String gatewayRawResponse;

    private String idempotencyKey;

    private LocalDateTime requestedAt;

    private LocalDateTime processedAt;

    public Payment() {
    }

    /**
     * Cria uma tentativa de pagamento ainda pendente.
     *
     * @param order          pedido alvo do pagamento
     * @param amount         valor a ser cobrado
     * @param method         forma de pagamento
     * @param idempotencyKey chave que identifica a tentativa
     * @throws IllegalArgumentException quando o valor nao for positivo ou a
     *                                  chave de idempotencia estiver vazia
     */
    public Payment(Order order, BigDecimal amount, PaymentMethod method, String idempotencyKey) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount deve ser positivo");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey e obrigatoria");
        }

        this.order = order;
        this.amount = amount;
        this.method = method;
        this.idempotencyKey = idempotencyKey.trim();
        this.status = PaymentStatus.PENDING;
        this.requestedAt = LocalDateTime.now();
    }

    /**
     * Marca a tentativa como aprovada pelo provedor.
     *
     * @param transactionId identificador da transacao devolvido pelo provedor
     * @param rawResponse   payload bruto devolvido, para auditoria
     * @throws IllegalStateException quando a tentativa ja estiver finalizada
     */
    public void markApproved(String transactionId, String rawResponse) {
        requirePending();
        this.status = PaymentStatus.APPROVED;
        this.gatewayTransactionId = transactionId;
        this.gatewayRawResponse = rawResponse;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Marca a tentativa como recusada pelo provedor.
     *
     * @param rawResponse payload bruto devolvido, para auditoria
     * @throws IllegalStateException quando a tentativa ja estiver finalizada
     */
    public void markDeclined(String rawResponse) {
        requirePending();
        this.status = PaymentStatus.DECLINED;
        this.gatewayRawResponse = rawResponse;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Marca a tentativa como falha tecnica (timeout, erro de rede).
     *
     * @param rawResponse detalhe da falha, para auditoria
     * @throws IllegalStateException quando a tentativa ja estiver finalizada
     */
    public void markFailed(String rawResponse) {
        requirePending();
        this.status = PaymentStatus.FAILED;
        this.gatewayRawResponse = rawResponse;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Informa se a tentativa terminou com aprovacao.
     *
     * @return {@code true} quando o status e {@link PaymentStatus#APPROVED}
     */
    public boolean isApproved() {
        return PaymentStatus.APPROVED == status;
    }

    /**
     * Informa se a tentativa ja recebeu resposta do provedor.
     *
     * @return {@code true} quando o status difere de {@link PaymentStatus#PENDING}
     */
    public boolean isSettled() {
        return status != null && PaymentStatus.PENDING != status;
    }

    private void requirePending() {
        if (status != null && PaymentStatus.PENDING != status) {
            throw new IllegalStateException(
                    "pagamento ja finalizado com status " + status + " e nao pode ser alterado");
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public void setGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
    }

    public String getGatewayRawResponse() {
        return gatewayRawResponse;
    }

    public void setGatewayRawResponse(String gatewayRawResponse) {
        this.gatewayRawResponse = gatewayRawResponse;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

}
