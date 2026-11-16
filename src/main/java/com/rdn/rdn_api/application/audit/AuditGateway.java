package com.rdn.rdn_api.application.audit;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;

/**
 * Porta de saida da trilha de auditoria.
 *
 * <p>A aplicacao depende apenas desta interface; a implementacao grava em
 * {@code audit_logs} e replica o evento no log da aplicacao.</p>
 */
public interface AuditGateway {

    /**
     * Registra um evento de auditoria.
     *
     * @param entry dados do evento
     */
    void record(AuditEntry entry);

    /**
     * Consulta paginada dos eventos.
     *
     * @param action     filtro por acao, ou {@code null}
     * @param resourceId filtro por recurso, ou {@code null}
     * @param from       inicio do periodo, ou {@code null}
     * @param to         fim do periodo, ou {@code null}
     * @param page       pagina (base zero)
     * @param size       tamanho da pagina
     * @return pagina de eventos, do mais recente para o mais antigo
     */
    Page<AuditLogView> search(AuditAction action, UUID resourceId, LocalDateTime from,
            LocalDateTime to, int page, int size);
}
