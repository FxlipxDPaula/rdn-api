package com.rdn.rdn_api.application.promotion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.rdn.rdn_api.domain.Promotion;

/**
 * Porta de saida das campanhas promocionais.
 */
public interface PromotionGateway {

    /**
     * Busca a campanha pelo identificador.
     *
     * @param id identificador
     * @return campanha, ou vazio
     */
    Optional<Promotion> findById(UUID id);

    /**
     * Lista as campanhas marcadas como ativas.
     *
     * @return campanhas ativas
     */
    List<Promotion> findActive();
}
