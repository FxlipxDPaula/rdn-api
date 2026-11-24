package com.rdn.rdn_api.api.error;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Corpo unico de erro da API.
 *
 * <p>Todas as falhas respondem neste formato, conforme o template do roteiro:
 * {@code error} (codigo estavel), {@code message} (texto legivel),
 * {@code details} (problemas por campo), {@code timestamp} e {@code path}.</p>
 *
 * @param error     codigo do erro, por exemplo {@code CREDENCIAIS_INVALIDAS}
 * @param message   mensagem legivel para o cliente
 * @param details   lista de problemas por campo (vazia quando nao se aplica)
 * @param timestamp instante da falha
 * @param path      caminho da requisicao
 */
public record ApiError(
        @Schema(description = "Codigo estavel do erro", example = "ESTOQUE_INSUFICIENTE")
        String error,

        @Schema(description = "Mensagem legivel para o cliente",
                example = "Nao ha quantidade suficiente para um ou mais itens.")
        String message,

        @Schema(description = "Problemas por campo; lista vazia quando nao se aplica")
        List<ApiErrorDetail> details,

        @Schema(description = "Instante da falha em UTC", example = "2026-11-16T12:00:00Z")
        Instant timestamp,

        @Schema(description = "Caminho da requisicao que falhou", example = "/pedidos")
        String path) {

    /**
     * Cria um erro sem detalhes por campo.
     *
     * @param error   codigo do erro
     * @param message mensagem legivel
     * @param path    caminho da requisicao
     * @return corpo de erro pronto para serializacao
     */
    public static ApiError of(String error, String message, String path) {
        return new ApiError(error, message, List.of(), Instant.now(), path);
    }

    /**
     * Cria um erro com detalhes por campo.
     *
     * @param error   codigo do erro
     * @param message mensagem legivel
     * @param details problemas por campo
     * @param path    caminho da requisicao
     * @return corpo de erro pronto para serializacao
     */
    public static ApiError of(String error, String message, List<ApiErrorDetail> details, String path) {
        return new ApiError(error, message, details, Instant.now(), path);
    }
}
