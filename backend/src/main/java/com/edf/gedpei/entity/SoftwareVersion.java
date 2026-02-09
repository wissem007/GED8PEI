package com.edf.gedpei.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entite representant une version de logiciel avec son statut.
 */
@Entity
@Table(name = "software_versions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"software_name", "version"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoftwareVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "software_name", nullable = false, length = 255)
    private String softwareName;

    @Column(nullable = false, length = 255)
    private String version;

    @Column(name = "package_version", length = 255)
    private String packageVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private VersionStatus status;

    @Column(name = "status_update_date")
    private LocalDate statusUpdateDate;

    @Column(name = "initial_support_end_date")
    private LocalDate initialSupportEndDate;

    @Column(name = "extended_support_end_date")
    private LocalDate extendedSupportEndDate;

    @Column(name = "cyber_support_end_date")
    private LocalDate cyberSupportEndDate;

    @Column(length = 2000)
    private String notes;

    /**
     * Liens utiles au format JSON: [{"label": "...", "url": "..."}]
     */
    @Column(name = "useful_links", columnDefinition = "TEXT")
    private String usefulLinks;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Enum des statuts possibles pour une version de logiciel.
     */
    public enum VersionStatus {
        PRECONISEE("Préconisée"),
        TRAJECTOIRE_PRECONISEE("Trajectoire préconisée"),
        TOLEREE("Tolérée"),
        INTERDITE("Interdite"),
        INTERDITE_CYBER("Interdite Cyber");

        private final String displayName;

        VersionStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        /**
         * Parse un statut depuis une chaine de caracteres.
         * Supporte les formats GED-PEI, Sipedia catalogue et CSR Detail des versions.
         */
        public static VersionStatus fromString(String text) {
            if (text == null || text.isBlank()) {
                return TOLEREE;
            }
            String normalized = text.trim().toUpperCase()
                    .replace("É", "E")
                    .replace("È", "E")
                    .replace("Í", "I");

            // Format GED-PEI et CSR Detail des versions
            if (normalized.contains("TRAJECTOIRE") && normalized.contains("PRECONISEE")) {
                return TRAJECTOIRE_PRECONISEE;
            }
            if (normalized.contains("PRECONISEE")) {
                return PRECONISEE;
            }
            if (normalized.contains("INTERDITE") && normalized.contains("CYBER")) {
                return INTERDITE_CYBER;
            }
            if (normalized.contains("INTERDITE")) {
                return INTERDITE;
            }
            if (normalized.contains("TOLEREE")) {
                return TOLEREE;
            }
            // "Prochaine" dans CSR Detail des versions = version future preconisee
            if (normalized.contains("PROCHAINE")) {
                return TRAJECTOIRE_PRECONISEE;
            }

            // Format Sipedia - Politique industrielle
            if (normalized.contains("PRIVILEGIEE") || normalized.contains("PRIVILEGIE")) {
                return PRECONISEE;
            }
            if (normalized.contains("AUTORISEE") || normalized.contains("AUTORISE")) {
                return TOLEREE;
            }
            if (normalized.contains("ALTERNATIVE")) {
                return TOLEREE;
            }
            if (normalized.contains("DECONSEILLE") || normalized.contains("DECONSEILLEE")) {
                return INTERDITE;
            }

            return TOLEREE;
        }
    }
}
