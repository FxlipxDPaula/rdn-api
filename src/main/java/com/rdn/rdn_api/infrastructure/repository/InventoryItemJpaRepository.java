package com.rdn.rdn_api.infrastructure.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rdn.rdn_api.infrastructure.persistence.entity.InventoryItemJpaEntity;

import jakarta.persistence.LockModeType;

/**
 * Acesso a tabela {@code inventory_items}.
 */
public interface InventoryItemJpaRepository extends JpaRepository<InventoryItemJpaEntity, UUID> {

    Optional<InventoryItemJpaEntity> findByBranchIdAndMenuItemId(UUID branchId, UUID menuItemId);

    /**
     * Le a linha de estoque com lock pessimista de escrita.
     *
     * <p>Usado apenas na baixa por pedido: duas comandas simultaneas na mesma
     * unidade precisam serializar a leitura para nao vender o mesmo saldo duas
     * vezes (o projeto nao usa {@code @Version}).</p>
     *
     * @param branchId   unidade
     * @param menuItemId item de cardapio
     * @return linha de estoque travada ate o fim da transacao
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryItemJpaEntity i "
            + "where i.branch.id = :branchId and i.menuItem.id = :menuItemId")
    Optional<InventoryItemJpaEntity> findForUpdate(@Param("branchId") UUID branchId,
            @Param("menuItemId") UUID menuItemId);

    List<InventoryItemJpaEntity> findByBranchId(UUID branchId);

}
