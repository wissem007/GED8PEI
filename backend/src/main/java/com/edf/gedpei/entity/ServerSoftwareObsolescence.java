package com.edf.gedpei.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entite representant l'obsolescence logicielle par serveur.
 * Importee depuis PMT DISCOVR / Obsolescence Prevobs.
 */
@Entity
@Table(name = "server_software_obsolescence", indexes = {
    @Index(name = "idx_sso_server", columnList = "server_name"),
    @Index(name = "idx_sso_environment", columnList = "environment"),
    @Index(name = "idx_sso_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServerSoftwareObsolescence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "environment", columnDefinition = "VARCHAR(50)")
    private String environment;

    @Column(name = "instance", columnDefinition = "VARCHAR(100)")
    private String instance;

    @Column(name = "server_name", nullable = false, columnDefinition = "VARCHAR(100)")
    private String serverName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(50)")
    private ObsolescenceStatus status;

    @Column(name = "software_name", nullable = false, columnDefinition = "VARCHAR(255)")
    private String softwareName;

    @Column(name = "current_version", columnDefinition = "VARCHAR(100)")
    private String currentVersion;

    @Column(name = "support_end_date", columnDefinition = "VARCHAR(20)")
    private String supportEndDate;

    @Column(name = "os_target", columnDefinition = "VARCHAR(100)")
    private String osTarget;

    @Column(name = "os_actual_target_version", columnDefinition = "VARCHAR(100)")
    private String osActualTargetVersion;

    @Column(name = "os_target_solution_version", columnDefinition = "VARCHAR(100)")
    private String osTargetSolutionVersion;

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
     * Statuts d'obsolescence possibles.
     */
    public enum ObsolescenceStatus {
        PRECONISEE("Preconisee", "#4caf50"),
        TRAJECTOIRE_PRECONISEE("Trajectoire Preconisee", "#2196f3"),
        TOLEREE("Toleree", "#ff9800"),
        RISQUEE("Risquee", "#ff5722"),
        INTERDITE("Interdite", "#f44336"),
        INTERDITE_CYBER("Interdite Cyber", "#d32f2f"),
        OBSOLETE("Obsolete", "#9e9e9e");

        private final String displayName;
        private final String color;

        ObsolescenceStatus(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getColor() {
            return color;
        }

        /**
         * Parse un statut depuis une chaine.
         */
        public static ObsolescenceStatus fromString(String text) {
            if (text == null || text.isBlank()) {
                return TOLEREE;
            }
            String normalized = text.trim().toUpperCase()
                    .replace("É", "E").replace("È", "E")
                    .replace("Ê", "E").replace("Ë", "E");

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
            if (normalized.contains("RISQUEE")) {
                return RISQUEE;
            }
            if (normalized.contains("TOLEREE")) {
                return TOLEREE;
            }
            if (normalized.contains("OBSOLETE")) {
                return OBSOLETE;
            }

            return TOLEREE;
        }
    }
}
