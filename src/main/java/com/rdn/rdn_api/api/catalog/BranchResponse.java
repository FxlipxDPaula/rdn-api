package com.rdn.rdn_api.api.catalog;

import java.util.UUID;

import com.rdn.rdn_api.domain.Branch;

/**
 * Unidade da rede na resposta HTTP.
 */
public record BranchResponse(UUID unidadeId, String nome, boolean aberta, AddressResponse endereco) {

    public static BranchResponse from(Branch branch) {
        return new BranchResponse(branch.getId(), branch.getName(), branch.isOpen(),
                AddressResponse.from(branch.getAddress()));
    }
}
