package com.edf.gedpei.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Entite representant un site geographique.
 */
@Entity
@Table(name = "sites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column
    private String region;

    @Column
    private String country;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column
    private String address;

    @OneToMany(mappedBy = "site", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Server> servers = new HashSet<>();

    public Site(String code) {
        this.code = code;
        this.name = code;
    }

    public Site(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * Normalise et extrait le code du site.
     */
    public static String extractSiteCode(String siteName, String dataCenter) {
        if (siteName != null && !siteName.isBlank()) {
            String cleaned = siteName.trim().toUpperCase();

            // Sites OLM (Outre-Mer)
            if (cleaned.contains("GUADELOUPE")) return "GUADELOUPE";
            if (cleaned.contains("REUNION")) return "REUNION";
            if (cleaned.contains("CORSE")) return "CORSE";
            if (cleaned.contains("MARTINIQUE")) return "MARTINIQUE";
            if (cleaned.contains("GUYANE")) return "GUYANE";

            // Datacenters metropolitains
            if (cleaned.contains("PACY") || cleaned.contains("PCY")) return "PACY";
            if (cleaned.contains("NOE")) return "NOE";

            return cleaned.split("\\s+")[0];  // Premier mot
        }

        if (dataCenter != null && !dataCenter.isBlank()) {
            String dc = dataCenter.trim().toUpperCase();
            if (dc.contains("PACY") || dc.contains("PCY")) return "PACY";
            if (dc.contains("NOE")) return "NOE";
            return dc;
        }

        return "UNKNOWN";
    }

    /**
     * Retourne les coordonnees GPS approximatives pour la cartographie.
     */
    public static double[] getCoordinates(String siteCode) {
        return switch (siteCode.toUpperCase()) {
            case "GUADELOUPE" -> new double[]{16.2650, -61.5510};
            case "MARTINIQUE" -> new double[]{14.6415, -61.0242};
            case "REUNION" -> new double[]{-21.1151, 55.5364};
            case "CORSE" -> new double[]{42.0396, 9.0129};
            case "GUYANE" -> new double[]{4.9372, -52.3260};
            case "PACY", "PCY" -> new double[]{49.0167, 1.3833};
            case "NOE" -> new double[]{48.8566, 2.3522};  // Paris par defaut
            default -> new double[]{46.6034, 1.8883};  // Centre France
        };
    }
}
