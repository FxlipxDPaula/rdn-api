package com.rdn.rdn_api.api.order;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.rdn.rdn_api.application.auth.AuthenticatedUser;
import com.rdn.rdn_api.application.order.ChangeOrderStatusService;
import com.rdn.rdn_api.application.order.ChangeStatusCommand;
import com.rdn.rdn_api.application.order.CreateOrderCommand;
import com.rdn.rdn_api.application.order.CreateOrderService;
import com.rdn.rdn_api.application.order.QueryOrderService;
import com.rdn.rdn_api.domain.OrderStatus;
import com.rdn.rdn_api.domain.SalesChannel;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Endpoints de pedido.
 *
 * <p>O controller apenas traduz HTTP em comando: nenhuma regra de estoque,
 * status ou autorizacao vive aqui.</p>
 */
@RestController
@RequestMapping("/pedidos")
@Tag(name = "Pedidos", description = "Criacao, consulta e mudanca de status")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final CreateOrderService createOrderService;

    private final ChangeOrderStatusService changeOrderStatusService;

    private final QueryOrderService queryOrderService;

    public OrderController(CreateOrderService createOrderService,
            ChangeOrderStatusService changeOrderStatusService, QueryOrderService queryOrderService) {
        this.createOrderService = createOrderService;
        this.changeOrderStatusService = changeOrderStatusService;
        this.queryOrderService = queryOrderService;
    }

    /**
     * Cria o pedido, valida estoque e aplica o status inicial conforme o pagamento.
     *
     * @param request   corpo do pedido
     * @param principal usuario autenticado
     * @return pedido criado
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria pedido com desconto opcional de promocao",
            description = "Exige canalPedido (APP, TOTEM, BALCAO, PICKUP ou WEB), valida o catalogo e o "
                    + "estoque da unidade e aplica a promocao quando informada. O pedido nasce em "
                    + "AGUARDANDO_PAGAMENTO e o total e calculado pelo dominio.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido criado"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado"),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissao"),
            @ApiResponse(responseCode = "404", description = "Unidade, produto ou promocao inexistente"),
            @ApiResponse(responseCode = "409", description = "Estoque insuficiente ou promocao nao aplicavel")
    })
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        CreateOrderCommand command = new CreateOrderCommand(
                request.canalPedido(),
                request.unidadeId(),
                request.clienteId(),
                request.formaPagamento(),
                request.promocaoId(),
                request.itens().stream()
                        .map(item -> new CreateOrderCommand.CreateOrderItemCommand(
                                item.produtoId(), item.quantidade(), item.observacao()))
                        .toList());

        return OrderResponse.from(createOrderService.create(command, principal));
    }

    /**
     * Lista pedidos com filtros de canal e status.
     *
     * @param canalPedido filtro por canal (opcional)
     * @param status      filtro por status (opcional)
     * @param page        pagina (base zero)
     * @param limit       tamanho da pagina
     * @return pagina de resumos
     */
    @GetMapping
    @Operation(summary = "Lista pedidos com filtro por canal e status",
            description = "Consulta paginada para ADMIN e STAFF, usada para consolidar os pedidos por "
                    + "canal de origem e acompanhar o fluxo operacional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de pedidos"),
            @ApiResponse(responseCode = "400", description = "Parametro invalido"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado"),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissao")
    })
    public PageResponse<OrderSummaryResponse> list(
            @Parameter(description = "Filtra pelo canal de origem do pedido", example = "TOTEM")
            @RequestParam(required = false) SalesChannel canalPedido,
            @Parameter(description = "Filtra pelo status atual do pedido", example = "AGUARDANDO_PAGAMENTO")
            @RequestParam(required = false) OrderStatus status,
            @Parameter(description = "Pagina da listagem, comecando em zero", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Quantidade de pedidos por pagina", example = "10")
            @RequestParam(defaultValue = "10") int limit) {

        return PageResponse.from(queryOrderService.list(canalPedido, status, page, limit),
                OrderSummaryResponse::from);
    }

    /**
     * Consulta um pedido com itens.
     *
     * @param id        identificador do pedido
     * @param principal usuario autenticado
     * @return pedido completo
     */
    @GetMapping("/{id}")
    @Operation(summary = "Consulta pedido com itens",
            description = "Retorna o pedido com itens e valores. CUSTOMER acessa apenas os proprios "
                    + "pedidos; ADMIN e STAFF consultam qualquer pedido da rede.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido encontrado"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado"),
            @ApiResponse(responseCode = "403", description = "Pedido de outro cliente"),
            @ApiResponse(responseCode = "404", description = "Pedido inexistente")
    })
    public OrderResponse findById(
            @Parameter(description = "Identificador do pedido", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return OrderResponse.from(queryOrderService.findById(id, principal));
    }

    /**
     * Aplica a transicao de status validada pelo dominio.
     *
     * @param id        identificador do pedido
     * @param request   status desejado e motivo opcional
     * @param principal usuario autenticado
     * @return pedido atualizado
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Aplica transicao de status validada pelo dominio",
            description = "Aceita apenas transicoes previstas na maquina de estados (por exemplo "
                    + "RECEIVED -> PREPARING -> READY -> DELIVERED) e exige motivo ao cancelar.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status atualizado"),
            @ApiResponse(responseCode = "400", description = "Status nulo"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado"),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissao"),
            @ApiResponse(responseCode = "404", description = "Pedido inexistente"),
            @ApiResponse(responseCode = "409", description = "Transicao invalida")
    })
    public OrderResponse changeStatus(
            @Parameter(description = "Identificador do pedido", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID id,
            @Valid @RequestBody ChangeOrderStatusRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        ChangeStatusCommand command = new ChangeStatusCommand(request.status(), request.motivo());

        return OrderResponse.from(changeOrderStatusService.changeStatus(id, command, principal));
    }
}
