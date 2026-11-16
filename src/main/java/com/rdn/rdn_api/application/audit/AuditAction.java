package com.rdn.rdn_api.application.audit;

/**
 * Acoes sensiveis registradas na trilha de auditoria.
 *
 * <p>O valor e gravado como texto em {@code audit_logs.action}; novos valores
 * exigem apenas codigo, sem migration, porque a coluna nao tem CHECK de acao.</p>
 */
public enum AuditAction {

    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    USER_CREATED,
    ORDER_CREATED,
    ORDER_STATUS_CHANGED,
    ORDER_CANCELLED,
    PAYMENT_APPROVED,
    PAYMENT_DECLINED,
    PAYMENT_FAILED,
    PROMOTION_APPLIED,
    LOYALTY_CONSENT_GRANTED,
    LOYALTY_CONSENT_REVOKED,
    LOYALTY_POINTS_EARNED,
    LOYALTY_POINTS_REDEEMED
}
