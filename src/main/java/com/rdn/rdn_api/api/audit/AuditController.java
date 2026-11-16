package com.rdn.rdn_api.api.audit;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rdn.rdn_api.api.order.PageResponse;
import com.rdn.rdn_api.application.audit.AuditAction;
import com.rdn.rdn_api.application.audit.AuditQueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Consulta administrativa da trilha de auditoria.
 */
@RestController
@RequestMapping("/auditoria")
@Tag(name = "Auditoria", description = "Consulta de acoes sensiveis restrita a ADMIN")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

    private final AuditQueryService auditQueryService;

    public AuditController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    /**
     * Lista eventos com filtros opcionais.
     *
     * @param acao      acao desejada, ou {@code null}
     * @param recursoId recurso desejado, ou {@code null}
     * @param de        inicio do periodo, ou {@code null}
     * @param ate       fim do periodo, ou {@code null}
     * @param page      pagina (base zero)
     * @param limit     tamanho da pagina
     * @return pagina de eventos
     */
    @GetMapping
    @Operation(summary = "Lista eventos de auditoria com filtros")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de eventos"),
            @ApiResponse(responseCode = "400", description = "Parametro invalido"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado"),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissao")
    })
    public PageResponse<AuditLogResponse> list(
            @RequestParam(required = false) AuditAction acao,
            @RequestParam(required = false) UUID recursoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime ate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit) {

        return PageResponse.from(auditQueryService.search(acao, recursoId, de, ate, page, limit),
                AuditLogResponse::from);
    }
}
