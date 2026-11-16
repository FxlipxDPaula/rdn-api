package com.rdn.rdn_api.application.loyalty.exception;

/**
 * Levantada quando o saldo de pontos nao cobre o resgate solicitado.
 */
public class InsufficientPointsException extends RuntimeException {

    private final int available;

    private final int requested;

    public InsufficientPointsException(int available, int requested) {
        super("Saldo de pontos insuficiente.");
        this.available = available;
        this.requested = requested;
    }

    public int getAvailable() {
        return available;
    }

    public int getRequested() {
        return requested;
    }
}
