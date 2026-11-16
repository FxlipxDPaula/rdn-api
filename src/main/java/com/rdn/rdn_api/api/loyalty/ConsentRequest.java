package com.rdn.rdn_api.api.loyalty;

import jakarta.validation.constraints.NotNull;

/**
 * Corpo de {@code PUT /fidelidade/consentimento}.
 *
 * @param consentimento {@code true} para opt-in, {@code false} para opt-out
 */
public record ConsentRequest(@NotNull(message = "consentimento e obrigatorio") Boolean consentimento) {
}
