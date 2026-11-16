package com.rdn.rdn_api.application.audit;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso de consulta da trilha de auditoria.
 */
@Service
public class AuditQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AuditGateway auditGateway;

    public AuditQueryService(AuditGateway auditGateway) {
        this.auditGateway = auditGateway;
    }

    /**
     * Lista os eventos de auditoria com filtros opcionais.
     *
     * @param action     acao desejada, ou {@code null}
     * @param resourceId recurso desejado, ou {@code null}
     * @param from       inicio do periodo, ou {@code null}
     * @param to         fim do periodo, ou {@code null}
     * @param page       pagina solicitada (base zero)
     * @param size       tamanho solicitado
     * @return pagina de eventos
     */
    @Transactional(readOnly = true)
    public Page<AuditLogView> search(AuditAction action, UUID resourceId, LocalDateTime from,
            LocalDateTime to, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        return auditGateway.search(action, resourceId, from, to, safePage, safeSize);
    }
}
