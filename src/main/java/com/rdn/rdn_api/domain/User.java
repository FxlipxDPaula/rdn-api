package com.rdn.rdn_api.domain;

import java.util.UUID;

/**
 * Usuario do sistema (cliente ou colaborador).
 *
 * <p>POJO de dominio: apenas campos, construtor padrao, getters/setters e
 * operacoes triviais. Nao contem anotacoes de persistencia nem regras de
 * negocio (sem validacoes e sem excecoes de dominio).</p>
 */
public class User {

    private UUID id;

    private String fullName;

    private String email;

    private String passwordHash;

    private UserRole role;

    private Boolean isActive;

    public User() {
    }

    /**
     * Verifica a senha informada contra o valor armazenado.
     *
     * <p>Comparacao direta de Strings: nao aplica algoritmo de hash. Deve ser
     * trocada por verificacao de hash quando a infraestrutura de seguranca
     * existir.</p>
     *
     * @param password senha informada
     * @return {@code true} quando a senha coincide com o valor armazenado,
     *         {@code false} caso contrario
     */
    public Boolean authenticate(String password) {
        return password != null && password.equals(passwordHash);
    }

    /**
     * Atualiza a senha armazenada.
     *
     * @param newPassword novo valor de senha/hash
     */
    public void updatePassword(String newPassword) {
        this.passwordHash = newPassword;
    }

    /**
     * Altera o perfil de acesso do usuario.
     *
     * @param role novo perfil
     */
    public void assignRole(UserRole role) {
        this.role = role;
    }

    /**
     * Desativa o usuario.
     */
    public void deactivate() {
        this.isActive = Boolean.FALSE;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

}
