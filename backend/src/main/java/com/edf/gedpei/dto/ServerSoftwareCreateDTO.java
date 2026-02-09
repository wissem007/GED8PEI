package com.edf.gedpei.dto;

import com.edf.gedpei.entity.ServerSoftware.UpdateStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

/**
 * DTO pour la creation/modification d'un logiciel serveur.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServerSoftwareCreateDTO {

    @NotNull(message = "L'ID du serveur est obligatoire")
    private Long serverId;

    @NotBlank(message = "Le nom du logiciel est obligatoire")
    private String softwareName;

    private String softwareType; // MIDDLEWARE, RUNTIME, DATABASE, WEBSERVER

    private String currentVersion;

    private String targetVersion;

    private String latestAvailableVersion;

    private UpdateStatus updateStatus;

    private LocalDate lastUpdateDate;

    private LocalDate nextUpdateDate;

    private String updatePriority; // Haute, Moyenne, Basse

    private Integer port;

    private String installPath;

    private String notes;

    private Boolean active;
}
