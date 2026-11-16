package com.rdn.rdn_api.api.catalog;

import com.rdn.rdn_api.domain.Address;

/**
 * Endereco de uma unidade na resposta HTTP.
 */
public record AddressResponse(String logradouro, String numero, String complemento, String bairro,
        String cidade, String estado, String cep) {

    public static AddressResponse from(Address address) {
        if (address == null) {
            return null;
        }

        return new AddressResponse(address.getStreet(), address.getNumber(), address.getComplement(),
                address.getNeighborhood(), address.getCity(), address.getState(), address.getZipCode());
    }
}
