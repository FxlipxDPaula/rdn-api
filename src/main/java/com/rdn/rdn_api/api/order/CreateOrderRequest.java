package com.rdn.rdn_api.api.order;

import java.util.List;
import java.util.UUID;

import com.rdn.rdn_api.domain.PaymentMethod;
import com.rdn.rdn_api.domain.SalesChannel;

import io.swagger.v3.oas.annotations.media.Schema;
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
 * @param promocaoId     campanha opcional aplicada ao pedido
 * @param itens          itens solicitados
 */
public record CreateOrderRequest(
        @NotNull(message = "canalPedido e obrigatorio")
        @Schema(description = "Canal de origem do pedido (multicanalidade)", example = "TOTEM")
        SalesChannel canalPedido,

        @NotNull(message = "unidadeId e obrigatorio")
        @Schema(description = "Unidade que produz o pedido",
                example = "00000000-0000-0000-0000-000000000001")
        UUID unidadeId,

        @Schema(description = "Cliente informado por STAFF ou ADMIN; ignorado para CUSTOMER",
                example = "00000000-0000-0000-0000-00000000000f")
        UUID clienteId,

        @NotNull(message = "formaPagamento e obrigatoria")
        @Schema(description = "Forma de pagamento; MOCK deixa o pedido aguardando pagamento", example = "MOCK")
        PaymentMethod formaPagamento,

        @Schema(description = "Campanha opcional aplicada ao pedido",
                example = "00000000-0000-0000-0000-0000000000c1")
        UUID promocaoId,

        @NotEmpty(message = "itens deve ter pelo menos um item") @Valid
        @Schema(description = "Itens solicitados; pelo menos um item")
        List<CreateOrderItemRequest> itens) {
}
