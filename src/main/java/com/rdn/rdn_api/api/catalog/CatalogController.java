package com.rdn.rdn_api.api.catalog;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rdn.rdn_api.application.catalog.CatalogQueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Consulta de unidades e cardapio por unidade.
 */
@RestController
@RequestMapping("/unidades")
@Tag(name = "Catalogo", description = "Unidades abertas e cardapio por unidade")
@SecurityRequirement(name = "bearerAuth")
public class CatalogController {

    private final CatalogQueryService catalogQueryService;

    public CatalogController(CatalogQueryService catalogQueryService) {
        this.catalogQueryService = catalogQueryService;
    }

    /**
     * Lista as unidades abertas.
     *
     * @return unidades com endereco
     */
    @GetMapping
    @Operation(summary = "Lista as unidades abertas")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Unidades com endereco"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado")
    })
    public List<BranchResponse> list() {
        return catalogQueryService.listOpenBranches().stream()
                .map(BranchResponse::from)
                .toList();
    }

    /**
     * Consulta o cardapio da unidade.
     *
     * @param id identificador da unidade
     * @return cardapio com itens
     */
    @GetMapping("/{id}/cardapio")
    @Operation(summary = "Consulta o cardapio da unidade")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cardapio com itens"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado"),
            @ApiResponse(responseCode = "404", description = "Unidade sem cardapio")
    })
    public MenuResponse menu(@PathVariable UUID id) {
        return MenuResponse.from(catalogQueryService.findMenu(id));
    }
}
