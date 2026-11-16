package com.rdn.rdn_api.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rdn.rdn_api.infrastructure.persistence.entity.AuditLogJpaEntity;

/**
 * Acesso a tabela {@code audit_logs}.
 */
public interface AuditLogJpaRepository extends JpaRepository<AuditLogJpaEntity, UUID> {

    /**
     * Busca paginada com filtros opcionais.
     *
     * <p>Uma unica consulta com parametros nulos evita quatro metodos derivados
     * e mantem o filtro no banco.</p>
     *
     * @param action     nome da acao, ou {@code null}
     * @param resourceId identificador do recurso, ou {@code null}
     * @param start      inicio do periodo, ou {@code null}
     * @param end        fim do periodo, ou {@code null}
     * @param pageable   paginacao e ordenacao
     * @return pagina de eventos
     */
    @Query("""
            select a from AuditLogJpaEntity a
            where (:action is null or a.action = :action)
              and (:resourceId is null or a.resourceId = :resourceId)
              and (:start is null or a.occurredAt >= :start)
              and (:end is null or a.occurredAt <= :end)
            """)
    Page<AuditLogJpaEntity> search(@Param("action") String action,
            @Param("resourceId") UUID resourceId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);
}
