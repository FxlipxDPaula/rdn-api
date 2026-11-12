package com.rdn.rdn_api.api.error;

/**
 * Detalhe de um campo invalido dentro do erro padronizado.
 *
 * @param field nome do campo com problema
 * @param issue descricao do problema
 */
public record ApiErrorDetail(String field, String issue) {
}
