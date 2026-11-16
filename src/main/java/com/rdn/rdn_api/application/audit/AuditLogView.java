package com.rdn.rdn_api.application.audit;

import java.time.LocalDateTime;
import java.util.UUID;

import com.rdn.rdn_api.domain.UserRole;

/**
 * Visao de um registro de auditoria para consulta administrativa.
 *
 * @param id         identificador do registro
 * @param occurredAt instante do evento
 * @param actorEmail e-mail do ator
 * @param actorRole  perfil do ator
 * @param action     acao registrada
 * @param resource   recurso afetado
 * @param resourceId identificador do recurso
 * @param details    detalhe curto
 * @param ipAddress  IP de origem, quando capturado
 */
public record AuditLogView(UUID id, LocalDateTime occurredAt, String actorEmail, UserRole actorRole,
        AuditAction action, String resource, UUID resourceId, String details, String ipAddress) {
}
