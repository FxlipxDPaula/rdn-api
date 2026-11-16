package com.rdn.rdn_api.infrastructure.persistence.audit;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.rdn.rdn_api.application.audit.AuditAction;
import com.rdn.rdn_api.application.audit.AuditEntry;
import com.rdn.rdn_api.application.audit.AuditGateway;
import com.rdn.rdn_api.application.audit.AuditLogView;
import com.rdn.rdn_api.infrastructure.persistence.entity.AuditLogJpaEntity;
import com.rdn.rdn_api.infrastructure.persistence.mapper.AuditLogMapper;
import com.rdn.rdn_api.infrastructure.repository.AuditLogJpaRepository;

/**
 * Adaptador de {@link AuditGateway} sobre Spring Data JPA.
 *
 * <p>Alem da tabela, replica o evento no log da aplicacao. O IP e detalhe de
 * infraestrutura: e lido do request corrente sem poluir a camada de aplicacao.</p>
 */
@Component
public class AuditGatewayAdapter implements AuditGateway {

    private static final Logger log = LoggerFactory.getLogger(AuditGatewayAdapter.class);

    private final AuditLogJpaRepository auditLogRepository;

    public AuditGatewayAdapter(AuditLogJpaRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void record(AuditEntry entry) {
        if (entry == null) {
            return;
        }

        AuditLogJpaEntity entity = AuditLogMapper.toEntity(entry, LocalDateTime.now(), currentIp());
        auditLogRepository.save(entity);

        log.info("audit action={} resource={} resourceId={} actor={}",
                entry.action(), entry.resource(), entry.resourceId(), entry.actorEmail());
    }

    @Override
    public Page<AuditLogView> search(AuditAction action, UUID resourceId, LocalDateTime start,
            LocalDateTime end, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"));

        return auditLogRepository.search(action == null ? null : action.name(), resourceId, start, end, pageable)
                .map(AuditLogMapper::toView);
    }

    private static String currentIp() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest().getRemoteAddr();
        }

        return null;
    }
}
