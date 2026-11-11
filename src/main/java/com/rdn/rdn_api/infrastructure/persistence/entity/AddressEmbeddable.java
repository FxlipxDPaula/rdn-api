package com.rdn.rdn_api.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Mapeamento relacional do value object {@code Address}.
 *
 * <p>Como {@code Address} e imutavel e sem identidade propria, ele nao vira
 * tabela: suas colunas sao embutidas na tabela que o referencia (hoje,
 * {@code branches}).</p>
 */
@Embeddable
public class AddressEmbeddable {

    @Column(name = "street", nullable = false, length = 120)
    private String street;

    @Column(name = "street_number", nullable = false, length = 20)
    private String number;

    @Column(name = "complement", length = 120)
    private String complement;

    @Column(name = "neighborhood", nullable = false, length = 80)
    private String neighborhood;

    @Column(name = "city", nullable = false, length = 80)
    private String city;

    @Column(name = "state", nullable = false, length = 2)
    private String state;

    @Column(name = "zip_code", nullable = false, length = 10)
    private String zipCode;

    public AddressEmbeddable() {
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getComplement() {
        return complement;
    }

    public void setComplement(String complement) {
        this.complement = complement;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

}
