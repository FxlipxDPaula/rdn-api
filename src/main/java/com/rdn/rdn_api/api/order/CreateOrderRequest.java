package com.rdn.rdn_api.api.order;

import java.util.List;
import java.util.UUID;

import com.rdn.rdn_api.domain.PaymentMethod;
import com.rdn.rdn_api.domain.SalesChannel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo de {@code POST /pedidos}.
 *
 * @param canalPedido    canal de origem (obrigatorio pela multicanalidade)
 * @param unidadeId      unidade que produz o pedido
 * @param clienteId      cliente informado por STAFF/ADMIN; ignorado para CUSTOMER
 * @param formaPagamento forma de pagamento; {@code MOCK} deixa o pedido aguardando pagamento
 * @param itens          itens solicitados
 */
public record CreateOrderRequest(
        @NotNull(message = "canalPedido e obrigatorio") SalesChannel canalPedido,
        @NotNull(message = "unidadeId e obrigatorio") UUID unidadeId,
        UUID clienteId,
        @NotNull(message = "formaPagamento e obrigatoria") PaymentMethod formaPagamento,
        @NotEmpty(message = "itens deve ter pelo menos um item") @Valid List<CreateOrderItemRequest> itens) {
}
