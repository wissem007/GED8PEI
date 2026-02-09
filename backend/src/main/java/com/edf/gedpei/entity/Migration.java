package com.edf.gedpei.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entite representant une migration planifiee.
 */
@Entity
@Table(name = "migrations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Migration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String phase;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "environment_name")
    private String environmentName;

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column
    private String responsable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MigrationStatus status;

    @Column(length = 1000)
    private String risques;

    @Column(length = 1000)
    private String commentaires;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    /**
     * Statuts possibles pour une migration.
     */
    public enum MigrationStatus {
        PLANIFIE("Planifié", "#FFC107"),
        NON_DEMARRE("Non démarré", "#9E9E9E"),
        EN_COURS("En cours", "#2196F3"),
        TERMINE("Terminé", "#4CAF50"),
        ANNULE("Annulé", "#F44336"),
        EN_ATTENTE("En attente", "#FF9800");

        private final String displayName;
        private final String color;

        MigrationStatus(String displayName, String color) {
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
}
