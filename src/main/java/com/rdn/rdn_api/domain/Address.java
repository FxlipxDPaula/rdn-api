package com.rdn.rdn_api.domain;

import java.util.Objects;

/**
 * Endereco de uma filial ou cliente.
 *
 * <p>Value Object: classe {@code final} e imutavel, sem identificador proprio e
 * comparada pelo conjunto dos seus valores. Nao possui setters — para alterar um
 * endereco, crie uma nova instancia.</p>
 *
 * <p>Diferente dos demais POJOs do pacote, o construtor valida os seus
 * invariantes: {@code street}, {@code number}, {@code neighborhood}, {@code city},
 * {@code state} e {@code zipCode} sao obrigatorios (nao nulos e nao em branco).
 * {@code complement} e o unico componente opcional e e normalizado para string
 * vazia quando ausente.</p>
 */
public final class Address {

    private final String street;

    private final String number;

    private final String complement;

    private final String neighborhood;

    private final String city;

    private final String state;

    private final String zipCode;

    /**
     * Cria um endereco validando os componentes obrigatorios.
     *
     * @param street       logradouro
     * @param number       numero
     * @param complement   complemento, pode ser nulo
     * @param neighborhood bairro
     * @param city         cidade
     * @param state        estado ou unidade federativa
     * @param zipCode      codigo postal
     * @throws IllegalArgumentException quando um componente obrigatorio for nulo
     *                                  ou estiver em branco
     */
    public Address(String street, String number, String complement, String neighborhood,
            String city, String state, String zipCode) {
        this.street = requireNonBlank(street, "street");
        this.number = requireNonBlank(number, "number");
        this.complement = complement == null ? "" : complement.trim();
        this.neighborhood = requireNonBlank(neighborhood, "neighborhood");
        this.city = requireNonBlank(city, "city");
        this.state = requireNonBlank(state, "state");
        this.zipCode = requireNonBlank(zipCode, "zipCode");
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " e obrigatorio");
        }

        return value.trim();
    }

    /**
     * Cria uma copia deste endereco com o complemento informado.
     *
     * @param newComplement novo complemento
     * @return nova instancia de {@code Address}
     */
    public Address withComplement(String newComplement) {
        return new Address(street, number, newComplement, neighborhood, city, state, zipCode);
    }

    /**
     * Retorna o endereco em uma linha, no formato
     * {@code logradouro, numero - bairro, cidade/UF - CEP}.
     *
     * @return representacao do endereco em uma unica linha
     */
    public String singleLine() {
        String complementPart = complement.isEmpty() ? "" : ", " + complement;

        return "%s, %s%s - %s, %s/%s - %s"
                .formatted(street, number, complementPart, neighborhood, city, state, zipCode);
    }

    public String getStreet() {
        return street;
    }

    public String getNumber() {
        return number;
    }

    public String getComplement() {
        return complement;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getZipCode() {
        return zipCode;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Address address)) {
            return false;
        }

        return street.equals(address.street)
                && number.equals(address.number)
                && complement.equals(address.complement)
                && neighborhood.equals(address.neighborhood)
                && city.equals(address.city)
                && state.equals(address.state)
                && zipCode.equals(address.zipCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(street, number, complement, neighborhood, city, state, zipCode);
    }

    @Override
    public String toString() {
        return "Address{street='%s', number='%s', complement='%s', neighborhood='%s', city='%s', state='%s', zipCode='%s'}"
                .formatted(street, number, complement, neighborhood, city, state, zipCode);
    }

}
