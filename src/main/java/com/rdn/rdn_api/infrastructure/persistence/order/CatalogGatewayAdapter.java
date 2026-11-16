package com.rdn.rdn_api.infrastructure.persistence.order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rdn.rdn_api.application.order.CatalogGateway;
import com.rdn.rdn_api.domain.Branch;
import com.rdn.rdn_api.domain.Menu;
import com.rdn.rdn_api.domain.MenuItem;
import com.rdn.rdn_api.infrastructure.persistence.mapper.BranchMapper;
import com.rdn.rdn_api.infrastructure.persistence.mapper.MenuItemMapper;
import com.rdn.rdn_api.infrastructure.persistence.mapper.MenuMapper;
import com.rdn.rdn_api.infrastructure.repository.BranchJpaRepository;
import com.rdn.rdn_api.infrastructure.repository.MenuItemJpaRepository;
import com.rdn.rdn_api.infrastructure.repository.MenuJpaRepository;

/**
 * Adaptador de {@link CatalogGateway} sobre os repositorios de filial e cardapio.
 */
@Component
public class CatalogGatewayAdapter implements CatalogGateway {

    private final BranchJpaRepository branchRepository;

    private final MenuItemJpaRepository menuItemRepository;

    private final MenuJpaRepository menuRepository;

    public CatalogGatewayAdapter(BranchJpaRepository branchRepository,
            MenuItemJpaRepository menuItemRepository, MenuJpaRepository menuRepository) {
        this.branchRepository = branchRepository;
        this.menuItemRepository = menuItemRepository;
        this.menuRepository = menuRepository;
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

    @Override
    @Transactional(readOnly = true)
    public List<Branch> findOpenBranches() {
        return branchRepository.findByIsOpenTrue().stream()
                .map(BranchMapper::toDomainShallow)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Menu> findMenuByBranchId(UUID branchId) {
        return menuRepository.findByBranchId(branchId).map(MenuMapper::toDomain);
    }
}
