package com.rdn.rdn_api.application.audit;

import java.util.UUID;

import com.rdn.rdn_api.domain.UserRole;

/**
 * Dados de um evento de auditoria.
 *
 * <p>O ator e guardado como snapshot (id, e-mail e perfil) para o registro
 * sobreviver a desativacao ou exclusao do usuario. Nunca inclua senha, token ou
 * payload completo de pagamento em {@code details}.</p>
 *
 * @param actorId    identificador do usuario, quando autenticado
 * @param actorEmail e-mail do usuario ou o e-mail tentado no login
 * @param actorRole  perfil do usuario, quando conhecido
 * @param action     acao registrada
 * @param resource   recurso afetado (AUTH, ORDER, PAYMENT, LOYALTY_ACCOUNT)
 * @param resourceId identificador do recurso, quando houver
 * @param details    detalhe curto e sem dados sensiveis
 */
public record AuditEntry(UUID actorId, String actorEmail, UserRole actorRole, AuditAction action,
        String resource, UUID resourceId, String details) {
}
