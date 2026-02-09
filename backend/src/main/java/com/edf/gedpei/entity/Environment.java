package com.edf.gedpei.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Entite representant un environnement (PROD, PREPROD, etc.).
 */
@Entity
@Table(name = "environments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Environment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column
    private String color;

    @Column(name = "display_order")
    private Integer displayOrder;

    @OneToMany(mappedBy = "environment", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Server> servers = new HashSet<>();

    public Environment(String code) {
        this.code = code;
        this.name = code;
    }

    /**
     * Normalise le code de l'environnement.
     */
    public static String normalizeCode(String name) {
        if (name == null || name.isBlank()) {
            return "UNKNOWN";
        }

        String normalized = name.trim().toUpperCase();

        if (normalized.startsWith("PROD") && !normalized.equals("PROD")) {
            return "PROD";
        }
        if (normalized.startsWith("PREPROD") || normalized.startsWith("PRE-PROD")) {
            return "PREPROD";
        }
        if (normalized.contains("RECETTE") || normalized.contains("REC") || normalized.contains("UAT")) {
            return "RECETTE";
        }
        if (normalized.contains("DEV")) {
            return "DEV";
        }
        if (normalized.contains("INT")) {
            return "INT";
        }
        if (normalized.contains("QUALIF")) {
            return "QUALIF";
        }

        return normalized;
    }
}
