package com.rdn.rdn_api.infrastructure.security;

import java.time.Instant;

/**
 * Serializacao minima do corpo de erro para respostas escritas fora do
 * {@code DispatcherServlet} (entry point e access denied handler).
 *
 * <p>O JSON e montado manualmente de proposito: esses pontos rodam antes (ou
 * fora) da cadeia de conversores do Spring MVC, e um escape simples de aspas e
 * barra resolve os valores controlados pelo servidor.</p>
 */
final class SecurityErrorJson {

    private SecurityErrorJson() {
    }

    /**
     * Monta o corpo no mesmo formato de {@code ApiError}.
     *
     * @param error   codigo do erro
     * @param message mensagem legivel
     * @param path    caminho da requisicao
     * @return JSON pronto para escrita na resposta
     */
    static String body(String error, String message, String path) {
        return "{\"error\":\"" + escape(error)
                + "\",\"message\":\"" + escape(message)
                + "\",\"details\":[],\"timestamp\":\"" + Instant.now()
                + "\",\"path\":\"" + escape(path) + "\"}";
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
