package com.rdn.rdn_api.api.promotion;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rdn.rdn_api.application.promotion.PromotionService;
import com.rdn.rdn_api.application.promotion.PromotionView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Consulta das campanhas promocionais ativas.
 */
@RestController
@RequestMapping("/promocoes")
@Tag(name = "Promocoes", description = "Campanhas ativas e indicador de vigencia")
@SecurityRequirement(name = "bearerAuth")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    /**
     * Lista as campanhas ativas com o indicador de vigencia calculado agora.
     *
     * @return campanhas ativas
     */
    @GetMapping
    @Operation(summary = "Lista campanhas ativas com vigencia calculada")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Campanhas ativas"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado")
    })
    public List<PromotionResponse> list() {
        LocalDateTime now = LocalDateTime.now();

        return promotionService.listActive().stream()
                .map(promotion -> PromotionResponse.from(PromotionView.from(promotion, now)))
                .toList();
    }
}
