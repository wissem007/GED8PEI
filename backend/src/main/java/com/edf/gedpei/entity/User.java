package com.edf.gedpei.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entite representant un utilisateur de l'application.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column
    private String email;

    @Column(name = "full_name")
    private String fullName;

    @Column(length = 500)
    private String roles;

    @Column
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Retourne les rôles sous forme de Set.
     */
    public Set<String> getRolesSet() {
        if (roles == null || roles.isBlank()) {
            return new HashSet<>();
        }
        return new HashSet<>(java.util.Arrays.asList(roles.split(",")));
    }

    /**
     * Définit les rôles depuis un Set.
     */
    public void setRolesFromSet(Set<String> rolesSet) {
        if (rolesSet == null || rolesSet.isEmpty()) {
            this.roles = "USER";
        } else {
            this.roles = String.join(",", rolesSet);
        }
    }

    /**
     * Verifie si l'utilisateur a un role specifique.
     */
    public boolean hasRole(String role) {
        return getRolesSet().contains(role.toUpperCase());
    }

    /**
     * Verifie si l'utilisateur est administrateur.
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
}
