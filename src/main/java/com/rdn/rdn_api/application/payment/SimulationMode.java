package com.rdn.rdn_api.application.payment;

/**
 * Resultado que o provedor simulado deve devolver.
 *
 * <p>Existe apenas porque o provedor e um mock: em uma integracao real o
 * desfecho viria do proprio servico externo e este enum nao existiria.</p>
 */
public enum SimulationMode {

    APROVADO,
    RECUSADO,
    FALHA

}
