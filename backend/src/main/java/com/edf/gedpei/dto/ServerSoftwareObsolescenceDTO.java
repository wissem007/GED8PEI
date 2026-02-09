package com.edf.gedpei.dto;

import com.edf.gedpei.entity.ServerSoftwareObsolescence;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO pour l'obsolescence logicielle par serveur.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServerSoftwareObsolescenceDTO {

    private Long id;
    private String environment;
    private String instance;
    private String serverName;
    private String status;
    private String statusDisplay;
    private String statusColor;
    private String softwareName;
    private String currentVersion;
    private String supportEndDate;
    private String osTarget;
    private String osActualTargetVersion;
    private String osTargetSolutionVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Convertit une entite en DTO.
     */
    public static ServerSoftwareObsolescenceDTO fromEntity(ServerSoftwareObsolescence entity) {
        if (entity == null) return null;

        return ServerSoftwareObsolescenceDTO.builder()
                .id(entity.getId())
                .environment(entity.getEnvironment())
                .instance(entity.getInstance())
                .serverName(entity.getServerName())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .statusDisplay(entity.getStatus() != null ? entity.getStatus().getDisplayName() : null)
                .statusColor(entity.getStatus() != null ? entity.getStatus().getColor() : null)
                .softwareName(entity.getSoftwareName())
                .currentVersion(entity.getCurrentVersion())
                .supportEndDate(entity.getSupportEndDate())
                .osTarget(entity.getOsTarget())
                .osActualTargetVersion(entity.getOsActualTargetVersion())
                .osTargetSolutionVersion(entity.getOsTargetSolutionVersion())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Convertit un DTO en entite.
     */
    public ServerSoftwareObsolescence toEntity() {
        ServerSoftwareObsolescence entity = new ServerSoftwareObsolescence();
        entity.setId(this.id);
        entity.setEnvironment(this.environment);
        entity.setInstance(this.instance);
        entity.setServerName(this.serverName);
        entity.setStatus(this.status != null ?
                ServerSoftwareObsolescence.ObsolescenceStatus.fromString(this.status) :
                ServerSoftwareObsolescence.ObsolescenceStatus.TOLEREE);
        entity.setSoftwareName(this.softwareName);
        entity.setCurrentVersion(this.currentVersion);
        entity.setSupportEndDate(this.supportEndDate);
        entity.setOsTarget(this.osTarget);
        entity.setOsActualTargetVersion(this.osActualTargetVersion);
        entity.setOsTargetSolutionVersion(this.osTargetSolutionVersion);
        return entity;
    }
}
