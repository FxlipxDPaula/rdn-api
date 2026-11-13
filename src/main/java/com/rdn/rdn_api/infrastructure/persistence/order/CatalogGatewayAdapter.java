package com.rdn.rdn_api.infrastructure.persistence.order;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rdn.rdn_api.application.order.CatalogGateway;
import com.rdn.rdn_api.domain.Branch;
import com.rdn.rdn_api.domain.MenuItem;
import com.rdn.rdn_api.infrastructure.persistence.mapper.BranchMapper;
import com.rdn.rdn_api.infrastructure.persistence.mapper.MenuItemMapper;
import com.rdn.rdn_api.infrastructure.repository.BranchJpaRepository;
import com.rdn.rdn_api.infrastructure.repository.MenuItemJpaRepository;

/**
 * Adaptador de {@link CatalogGateway} sobre os repositorios de filial e cardapio.
 */
@Component
public class CatalogGatewayAdapter implements CatalogGateway {

    private final BranchJpaRepository branchRepository;

    private final MenuItemJpaRepository menuItemRepository;

    public CatalogGatewayAdapter(BranchJpaRepository branchRepository,
            MenuItemJpaRepository menuItemRepository) {
        this.branchRepository = branchRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Branch> findBranch(UUID branchId) {
        return branchRepository.findById(branchId).map(BranchMapper::toDomainShallow);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MenuItem> findMenuItem(UUID menuItemId) {
        return menuItemRepository.findById(menuItemId).map(MenuItemMapper::toDomain);
    }
}
