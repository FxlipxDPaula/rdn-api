package com.rdn.rdn_api.infrastructure.persistence.mapper;

import java.time.LocalDateTime;

import com.rdn.rdn_api.application.audit.AuditAction;
import com.rdn.rdn_api.application.audit.AuditEntry;
import com.rdn.rdn_api.application.audit.AuditLogView;
import com.rdn.rdn_api.infrastructure.persistence.entity.AuditLogJpaEntity;

/**
 * Conversao entre {@link AuditEntry}/{@link AuditLogView} e
 * {@link AuditLogJpaEntity}.
 */
public final class AuditLogMapper {

    private AuditLogMapper() {
    }

    public static AuditLogJpaEntity toEntity(AuditEntry entry, LocalDateTime occurredAt, String ipAddress) {
        if (entry == null) {
            return null;
        }

        AuditLogJpaEntity entity = new AuditLogJpaEntity();
        entity.setOccurredAt(occurredAt);
        entity.setActorId(entry.actorId());
        entity.setActorEmail(entry.actorEmail());
        entity.setActorRole(entry.actorRole());
        entity.setAction(entry.action() == null ? null : entry.action().name());
        entity.setResource(entry.resource());
        entity.setResourceId(entry.resourceId());
        entity.setDetails(entry.details());
        entity.setIpAddress(ipAddress);

        return entity;
    }

    public static AuditLogView toView(AuditLogJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return new AuditLogView(
                entity.getId(),
                entity.getOccurredAt(),
                entity.getActorEmail(),
                entity.getActorRole(),
                entity.getAction() == null ? null : AuditAction.valueOf(entity.getAction()),
                entity.getResource(),
                entity.getResourceId(),
                entity.getDetails(),
                entity.getIpAddress());
    }

}
