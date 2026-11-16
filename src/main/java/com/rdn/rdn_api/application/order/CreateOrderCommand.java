package com.rdn.rdn_api.application.order;

import java.util.List;
import java.util.UUID;

import com.rdn.rdn_api.domain.PaymentMethod;
import com.rdn.rdn_api.domain.SalesChannel;

/**
 * Comando de criacao de pedido.
 *
 * <p>Isola a aplicacao do DTO HTTP: o controller pode mudar de formato sem
 * alterar a regra de negocio. A lista de itens e copiada no construtor
 * compacto para que o comando seja imutavel de fato.</p>
 *
 * @param canalPedido    canal de origem, obrigatorio (multicanalidade)
 * @param unidadeId      unidade que produz o pedido
 * @param clienteId      cliente informado por STAFF/ADMIN; ignorado para CUSTOMER
 * @param formaPagamento forma de pagamento; {@code MOCK} cria o pedido aguardando pagamento
 * @param promocaoId     campanha aplicada, quando informada
 * @param itens          itens solicitados
 */
public record CreateOrderCommand(
        SalesChannel canalPedido,
        UUID unidadeId,
        UUID clienteId,
        PaymentMethod formaPagamento,
        UUID promocaoId,
        List<CreateOrderItemCommand> itens) {

    public CreateOrderCommand {
        itens = itens == null ? List.of() : List.copyOf(itens);
    }

    /**
     * Item solicitado no pedido.
     *
     * @param produtoId  item de cardapio
     * @param quantidade quantidade desejada
     * @param observacao texto livre (ex.: "sem cebola")
     */
    public record CreateOrderItemCommand(UUID produtoId, Integer quantidade, String observacao) {
    }
}
