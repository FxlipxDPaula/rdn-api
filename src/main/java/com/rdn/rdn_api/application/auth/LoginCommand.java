package com.rdn.rdn_api.application.auth;

/**
 * Entrada do caso de uso de login.
 *
 * <p>A camada de aplicacao recebe a senha em texto puro apenas neste ponto:
 * a comparacao com o hash armazenado acontece dentro de {@link LoginService}
 * e o valor nunca e persistido nem devolvido.</p>
 *
 * @param email    e-mail informado pelo usuario
 * @param password senha em texto puro informada pelo usuario
 */
public record LoginCommand(String email, String password) {
}
