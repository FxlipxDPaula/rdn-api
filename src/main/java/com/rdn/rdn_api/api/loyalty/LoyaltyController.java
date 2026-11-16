package com.rdn.rdn_api.api.loyalty;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rdn.rdn_api.api.order.PageResponse;
import com.rdn.rdn_api.application.auth.AuthenticatedUser;
import com.rdn.rdn_api.application.loyalty.LoyaltyService;
import com.rdn.rdn_api.application.loyalty.RedeemPointsCommand;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Endpoints do programa de fidelidade do cliente autenticado.
 */
@RestController
@RequestMapping("/fidelidade")
@Tag(name = "Fidelidade", description = "Consentimento, saldo, extrato e resgate de pontos")
@SecurityRequirement(name = "bearerAuth")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    public LoyaltyController(LoyaltyService loyaltyService) {
        this.loyaltyService = loyaltyService;
    }

    /**
     * Consulta saldo e consentimento.
     *
     * @param principal cliente autenticado
     * @return conta de fidelidade
     */
    @GetMapping
    @Operation(summary = "Consulta saldo e consentimento do cliente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conta de fidelidade"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado"),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissao")
    })
    public LoyaltyAccountResponse account(@AuthenticationPrincipal AuthenticatedUser principal) {
        return LoyaltyAccountResponse.from(loyaltyService.getAccount(principal));
    }

    /**
     * Lista o extrato de pontos.
     *
     * @param page      pagina (base zero)
     * @param limit     tamanho da pagina
     * @param principal cliente autenticado
     * @return pagina de lancamentos
     */
    @GetMapping("/transacoes")
    @Operation(summary = "Lista o extrato de pontos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de lancamentos"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado"),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissao")
    })
    public PageResponse<LoyaltyTransactionResponse> transactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        return PageResponse.from(loyaltyService.listTransactions(principal, page, limit),
                LoyaltyTransactionResponse::from);
    }

    /**
     * Registra ou revoga o consentimento.
     *
     * @param request   consentimento desejado
     * @param principal cliente autenticado
     * @return conta atualizada
     */
    @PutMapping("/consentimento")
    @Operation(summary = "Registra ou revoga o consentimento de fidelidade")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conta atualizada"),
            @ApiResponse(responseCode = "400", description = "Consentimento ausente"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado"),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissao")
    })
    public LoyaltyAccountResponse consent(@Valid @RequestBody ConsentRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        return LoyaltyAccountResponse.from(
                loyaltyService.setConsent(request.consentimento(), principal));
    }

    /**
     * Resgata pontos aplicando desconto em um pedido aguardando pagamento.
     *
     * @param request   pedido e pontos
     * @param principal cliente autenticado
     * @return resumo do resgate
     */
    @PostMapping("/resgates")
    @Operation(summary = "Resgata pontos como desconto em pedido aguardando pagamento")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pontos debitados e desconto aplicado"),
            @ApiResponse(responseCode = "400", description = "Pontos invalidos"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado"),
            @ApiResponse(responseCode = "403", description = "Pedido de outro cliente"),
            @ApiResponse(responseCode = "404", description = "Pedido inexistente"),
            @ApiResponse(responseCode = "409", description = "Sem consentimento, saldo ou status invalido")
    })
    public RedeemPointsResponse redeem(@Valid @RequestBody RedeemPointsRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        RedeemPointsCommand command = new RedeemPointsCommand(request.pedidoId(), request.pontos());

        return RedeemPointsResponse.from(loyaltyService.redeem(command, principal));
    }
}
