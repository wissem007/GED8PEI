package com.edf.gedpei.dto;

import com.edf.gedpei.entity.Migration.MigrationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

/**
 * DTO pour la creation/modification d'une migration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MigrationCreateDTO {

    @NotBlank(message = "La phase est obligatoire")
    private String phase;

    @NotBlank(message = "La description est obligatoire")
    private String description;

    private String environmentName;

    private LocalDate dateDebut;

    private LocalDate dateFin;

    private String responsable;

    @NotNull(message = "Le statut est obligatoire")
    private MigrationStatus status;

    private String risques;

    private String commentaires;
}
