package com.edf.gedpei.dto;

import com.edf.gedpei.entity.Migration;
import com.edf.gedpei.entity.Migration.MigrationStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO pour les migrations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MigrationDTO {

    private Long id;
    private String phase;
    private String description;
    private String environmentName;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String responsable;
    private MigrationStatus status;
    private String statusDisplay;
    private String statusColor;
    private String risques;
    private String commentaires;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Convertit une entite Migration en DTO.
     */
    public static MigrationDTO fromEntity(Migration migration) {
        if (migration == null) return null;

        return MigrationDTO.builder()
                .id(migration.getId())
                .phase(migration.getPhase())
                .description(migration.getDescription())
                .environmentName(migration.getEnvironmentName())
                .dateDebut(migration.getDateDebut())
                .dateFin(migration.getDateFin())
                .responsable(migration.getResponsable())
                .status(migration.getStatus())
                .statusDisplay(migration.getStatus() != null ? migration.getStatus().getDisplayName() : null)
                .statusColor(migration.getStatus() != null ? migration.getStatus().getColor() : null)
                .risques(migration.getRisques())
                .commentaires(migration.getCommentaires())
                .createdAt(migration.getCreatedAt())
                .updatedAt(migration.getUpdatedAt())
                .build();
    }
}
