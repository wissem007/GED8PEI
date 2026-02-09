package com.edf.gedpei.dto;

import com.edf.gedpei.entity.ServerSoftware;
import com.edf.gedpei.entity.ServerSoftware.UpdateStatus;
import lombok.*;

import java.time.LocalDate;

/**
 * DTO pour les logiciels installes sur un serveur.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServerSoftwareDTO {

    private Long id;
    private Long serverId;
    private String serverName;
    private String softwareName;
    private String softwareType;
    private String currentVersion;
    private String targetVersion;
    private String latestAvailableVersion;
    private UpdateStatus updateStatus;
    private String updateStatusDisplay;
    private LocalDate lastUpdateDate;
    private LocalDate nextUpdateDate;
    private String updatePriority;
    private Integer port;
    private String installPath;
    private String notes;
    private Boolean active;
    private boolean needsUpdate;
    private boolean hasUpdateAvailable;

    /**
     * Convertit une entite en DTO.
     */
    public static ServerSoftwareDTO fromEntity(ServerSoftware software) {
        if (software == null) return null;

        return ServerSoftwareDTO.builder()
                .id(software.getId())
                .serverId(software.getServer() != null ? software.getServer().getId() : null)
                .serverName(software.getServer() != null ? software.getServer().getResourceName() : null)
                .softwareName(software.getSoftwareName())
                .softwareType(software.getSoftwareType())
                .currentVersion(software.getCurrentVersion())
                .targetVersion(software.getTargetVersion())
                .latestAvailableVersion(software.getLatestAvailableVersion())
                .updateStatus(software.getUpdateStatus())
                .updateStatusDisplay(software.getUpdateStatus() != null ? software.getUpdateStatus().getDisplayName() : null)
                .lastUpdateDate(software.getLastUpdateDate())
                .nextUpdateDate(software.getNextUpdateDate())
                .updatePriority(software.getUpdatePriority())
                .port(software.getPort())
                .installPath(software.getInstallPath())
                .notes(software.getNotes())
                .active(software.getActive())
                .needsUpdate(software.needsUpdate())
                .hasUpdateAvailable(software.hasUpdateAvailable())
                .build();
    }
}
