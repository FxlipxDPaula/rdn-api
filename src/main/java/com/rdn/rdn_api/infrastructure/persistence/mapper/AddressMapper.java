package com.rdn.rdn_api.infrastructure.persistence.mapper;

import com.rdn.rdn_api.domain.Address;
import com.rdn.rdn_api.infrastructure.persistence.entity.AddressEmbeddable;

/**
 * Conversao entre o value object {@link Address} e sua representacao embutida
 * {@link AddressEmbeddable}.
 */
public final class AddressMapper {

    private AddressMapper() {
    }

    public static Address toDomain(AddressEmbeddable embeddable) {
        if (embeddable == null) {
            return null;
        }

        return new Address(
                embeddable.getStreet(),
                embeddable.getNumber(),
                embeddable.getComplement(),
                embeddable.getNeighborhood(),
                embeddable.getCity(),
                embeddable.getState(),
                embeddable.getZipCode());
    }

    public static AddressEmbeddable toEmbeddable(Address address) {
        if (address == null) {
            return null;
        }

        AddressEmbeddable embeddable = new AddressEmbeddable();
        embeddable.setStreet(address.getStreet());
        embeddable.setNumber(address.getNumber());
        embeddable.setComplement(address.getComplement());
        embeddable.setNeighborhood(address.getNeighborhood());
        embeddable.setCity(address.getCity());
        embeddable.setState(address.getState());
        embeddable.setZipCode(address.getZipCode());

        return embeddable;
    }

}
