package com.rdn.rdn_api.api.order;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Envelope de paginacao usado em todas as listagens da API.
 *
 * @param content       itens da pagina
 * @param page          numero da pagina (base zero)
 * @param size          tamanho da pagina
 * @param totalElements total de registros que atendem ao filtro
 * @param totalPages    total de paginas
 * @param <T>           tipo de saida
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    /**
     * Converte uma pagina de dominio no envelope de resposta.
     *
     * @param page   pagina de origem
     * @param mapper conversor de cada elemento
     * @return envelope pronto para serializacao
     */
    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
