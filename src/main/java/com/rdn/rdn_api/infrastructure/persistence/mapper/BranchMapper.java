package com.rdn.rdn_api.infrastructure.persistence.mapper;

import com.rdn.rdn_api.domain.Branch;
import com.rdn.rdn_api.infrastructure.persistence.entity.BranchJpaEntity;

/**
 * Conversao entre {@link Branch} e {@link BranchJpaEntity}.
 *
 * <p>{@code Branch} e {@code Menu} se referenciam mutuamente no dominio. Para
 * nao entrar em recursao, {@link #toDomainShallow(BranchJpaEntity)} monta a
 * filial sem carregar o cardapio e e o metodo usado quando a filial aparece como
 * referencia de outro agregado.</p>
 */
public final class BranchMapper {

    private BranchMapper() {
    }

    public static Branch toDomain(BranchJpaEntity entity) {
        Branch branch = toDomainShallow(entity);
        if (branch != null) {
            branch.setMenu(MenuMapper.toDomain(entity.getMenu()));
        }

        return branch;
    }

    public static Branch toDomainShallow(BranchJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        Branch branch = new Branch();
        branch.setId(entity.getId());
        branch.setName(entity.getName());
        branch.setIsOpen(entity.getIsOpen());
        branch.setAddress(AddressMapper.toDomain(entity.getAddress()));

        return branch;
    }

    public static BranchJpaEntity toEntity(Branch domain) {
        if (domain == null) {
            return null;
        }

        BranchJpaEntity entity = new BranchJpaEntity();
        copyScalarsToEntity(domain, entity);

        return entity;
    }

    public static void copyScalarsToEntity(Branch domain, BranchJpaEntity entity) {
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setIsOpen(domain.getIsOpen());
        entity.setAddress(AddressMapper.toEmbeddable(domain.getAddress()));
    }

}
