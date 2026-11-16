package com.rdn.rdn_api.api.audit;

import java.time.LocalDateTime;
import java.util.UUID;

import com.rdn.rdn_api.application.audit.AuditAction;
import com.rdn.rdn_api.application.audit.AuditLogView;
import com.rdn.rdn_api.domain.UserRole;

/**
 * Evento de auditoria na resposta HTTP.
 */
public record AuditLogResponse(UUID auditoriaId, LocalDateTime ocorridoEm, String atorEmail,
        UserRole atorPerfil, AuditAction acao, String recurso, UUID recursoId, String detalhes,
        String ip) {

    public static AuditLogResponse from(AuditLogView view) {
        return new AuditLogResponse(view.id(), view.occurredAt(), view.actorEmail(), view.actorRole(),
                view.action(), view.resource(), view.resourceId(), view.details(), view.ipAddress());
    }
}
