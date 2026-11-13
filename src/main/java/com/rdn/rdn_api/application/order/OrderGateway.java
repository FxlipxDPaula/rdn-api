package com.rdn.rdn_api.application.order;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;

import com.rdn.rdn_api.domain.Order;
import com.rdn.rdn_api.domain.OrderStatus;
import com.rdn.rdn_api.domain.SalesChannel;

/**
 * Porta de saida para o agregado {@link Order}.
 *
 * <p>A camada de aplicacao conhece apenas esta interface: nenhum caso de uso
 * importa JPA, repositorio Spring Data ou entidade de persistencia.</p>
 */
public interface OrderGateway {

    /**
     * Persiste o pedido (insercao quando o id e nulo, atualizacao caso contrario).
     *
     * @param order pedido de dominio
     * @return pedido persistido, com id e itens
     */
    Order save(Order order);

    /**
     * Busca o pedido com os itens ja carregados.
     *
     * @param id identificador do pedido
     * @return pedido completo, ou vazio
     */
    Optional<Order> findWithItemsById(UUID id);

    /**
     * Lista pedidos paginados, filtrando opcionalmente por canal e status.
     *
     * <p>O retorno traz apenas dados escalares do pedido: os itens ficam
     * disponiveis somente em {@link #findWithItemsById(UUID)}.</p>
     *
     * @param canalPedido canal de origem, ou {@code null} para todos
     * @param status      status desejado, ou {@code null} para todos
     * @param page        pagina (base zero)
     * @param size        tamanho da pagina
     * @return pagina de pedidos
     */
    Page<Order> search(SalesChannel canalPedido, OrderStatus status, int page, int size);
}
