package com.rdn.rdn_api.api.error;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Detalhe de um campo invalido dentro do erro padronizado.
 *
 * @param field nome do campo com problema
 * @param issue descricao do problema
 */
public record ApiErrorDetail(
        @Schema(description = "Nome do campo com problema", example = "itens[0].quantidade")
        String field,

        @Schema(description = "Descricao do problema", example = "Disponivel: 1; solicitado: 3")
        String issue) {
}
