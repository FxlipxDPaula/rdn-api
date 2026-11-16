package com.rdn.rdn_api.application.catalog;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rdn.rdn_api.application.order.CatalogGateway;
import com.rdn.rdn_api.application.order.exception.ResourceNotFoundException;
import com.rdn.rdn_api.domain.Branch;
import com.rdn.rdn_api.domain.Menu;

/**
 * Caso de uso de consulta do catalogo por unidade.
 */
@Service
public class CatalogQueryService {

    private final CatalogGateway catalogGateway;

    public CatalogQueryService(CatalogGateway catalogGateway) {
        this.catalogGateway = catalogGateway;
    }

    /**
     * Lista as unidades abertas.
     *
     * @return unidades com endereco
     */
    @Transactional(readOnly = true)
    public List<Branch> listOpenBranches() {
        return catalogGateway.findOpenBranches();
    }

    /**
     * Busca o cardapio da unidade.
     *
     * @param branchId identificador da unidade
     * @return cardapio com itens
     * @throws ResourceNotFoundException quando a unidade nao possui cardapio
     */
    @Transactional(readOnly = true)
    public Menu findMenu(UUID branchId) {
        return catalogGateway.findMenuByBranchId(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Cardapio da unidade", branchId));
    }
}
