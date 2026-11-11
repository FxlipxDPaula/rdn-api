package com.rdn.rdn_api.infrastructure.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rdn.rdn_api.infrastructure.persistence.entity.MenuItemJpaEntity;

/**
 * Acesso a tabela {@code menu_items}.
 */
public interface MenuItemJpaRepository extends JpaRepository<MenuItemJpaEntity, UUID> {

    List<MenuItemJpaEntity> findByMenuId(UUID menuId);

}
