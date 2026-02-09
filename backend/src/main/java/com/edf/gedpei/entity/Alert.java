package com.edf.gedpei.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entite representant une alerte de conformite.
 */
@Entity
@Table(name = "alerts", indexes = {
    @Index(name = "idx_alert_type", columnList = "alert_type"),
    @Index(name = "idx_alert_severity", columnList = "severity"),
    @Index(name = "idx_alert_status", columnList = "status"),
    @Index(name = "idx_alert_server", columnList = "server_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AlertStatus status;

    @Column(name = "server_name")
    private String serverName;

    @Column(name = "environment")
    private String environment;

    @Column(name = "software_name")
    private String softwareName;

    @Column(name = "current_version")
    private String currentVersion;

    @Column(name = "recommended_version")
    private String recommendedVersion;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "action_required", length = 500)
    private String actionRequired;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "acknowledged_by")
    private String acknowledgedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by")
    private String resolvedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = AlertStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Types d'alertes.
     */
    public enum AlertType {
        VERSION_INTERDITE("Version Interdite", "Un logiciel utilise une version interdite"),
        VERSION_OBSOLETE("Version Obsolete", "Un logiciel utilise une version obsolete"),
        VERSION_RISQUEE("Version Risquee", "Un logiciel utilise une version risquee"),
        MISE_A_JOUR_REQUISE("Mise a jour requise", "Une mise a jour est necessaire"),
        FIN_SUPPORT_PROCHE("Fin de support proche", "La fin de support approche"),
        FIN_SUPPORT_DEPASSEE("Fin de support depassee", "La date de fin de support est depassee"),
        INCOMPATIBILITE("Incompatibilite", "Incompatibilite de version detectee");

        private final String displayName;
        private final String description;

        AlertType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Niveaux de severite.
     */
    public enum AlertSeverity {
        CRITICAL("Critique", "#d32f2f"),
        HIGH("Elevee", "#f44336"),
        MEDIUM("Moyenne", "#ff9800"),
        LOW("Basse", "#4caf50"),
        INFO("Information", "#2196f3");

        private final String displayName;
        private final String color;

        AlertSeverity(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getColor() {
            return color;
        }
    }

    /**
     * Statuts des alertes.
     */
    public enum AlertStatus {
        ACTIVE("Active"),
        ACKNOWLEDGED("Prise en compte"),
        RESOLVED("Resolue"),
        IGNORED("Ignoree");

        private final String displayName;

        AlertStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
