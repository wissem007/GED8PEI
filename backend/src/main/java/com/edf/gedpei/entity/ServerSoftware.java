package com.edf.gedpei.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entite representant un logiciel installe sur un serveur.
 * Permet de tracer les versions de Tomcat, Java, OIDIS, etc.
 */
@Entity
@Table(name = "server_software", indexes = {
    @Index(name = "idx_server_software_server", columnList = "server_id"),
    @Index(name = "idx_server_software_name", columnList = "software_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServerSoftware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;

    @Column(name = "software_name", nullable = false)
    private String softwareName; // Ex: Tomcat, Java, OIDIS, Apache, PostgreSQL

    @Column(name = "software_type")
    private String softwareType; // Ex: MIDDLEWARE, RUNTIME, DATABASE, WEBSERVER

    @Column(name = "current_version")
    private String currentVersion; // Version actuellement installee

    @Column(name = "target_version")
    private String targetVersion; // Version cible si MAJ necessaire

    @Column(name = "latest_available_version")
    private String latestAvailableVersion; // Derniere version disponible

    @Enumerated(EnumType.STRING)
    @Column(name = "update_status")
    private UpdateStatus updateStatus; // Statut de mise a jour

    @Column(name = "last_update_date")
    private LocalDate lastUpdateDate; // Date de derniere MAJ

    @Column(name = "next_update_date")
    private LocalDate nextUpdateDate; // Date prevue prochaine MAJ

    @Column(name = "update_priority")
    private String updatePriority; // Haute, Moyenne, Basse

    @Column(name = "port")
    private Integer port; // Port d'ecoute si applicable

    @Column(name = "install_path")
    private String installPath; // Chemin d'installation

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column
    private Boolean active;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Enum pour le statut de mise a jour.
     */
    public enum UpdateStatus {
        UP_TO_DATE("A jour"),
        UPDATE_AVAILABLE("MAJ disponible"),
        UPDATE_REQUIRED("MAJ requise"),
        UPDATE_PLANNED("MAJ planifiee"),
        END_OF_LIFE("Fin de vie"),
        UNKNOWN("Inconnu");

        private final String displayName;

        UpdateStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Verifie si une MAJ est necessaire.
     */
    public boolean needsUpdate() {
        return updateStatus == UpdateStatus.UPDATE_REQUIRED ||
               updateStatus == UpdateStatus.END_OF_LIFE;
    }

    /**
     * Verifie si une MAJ est disponible.
     */
    public boolean hasUpdateAvailable() {
        return updateStatus == UpdateStatus.UPDATE_AVAILABLE ||
               updateStatus == UpdateStatus.UPDATE_REQUIRED ||
               updateStatus == UpdateStatus.UPDATE_PLANNED;
    }
}
