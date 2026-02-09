package com.edf.gedpei.dto;

import com.edf.gedpei.entity.Server;
import com.edf.gedpei.entity.Server.InfrastructureType;
import com.edf.gedpei.entity.Server.OsType;
import com.edf.gedpei.entity.ServerType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO pour les serveurs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServerDTO {

    private Long id;
    private String resourceName;
    private String hostname;
    private String ipFront;
    private String ipAdmin;
    private String ipIlo;
    private String refDat;
    private ServerType serverType;
    private String serverTypeDisplay;
    private String environmentName;
    private String siteCode;
    private String siteName;
    private String dataCenter;
    private LocalDate lastAdminUpdate;
    private String os;
    private String osVersion;
    private OsType osType;
    private String osTypeDisplay;
    private InfrastructureType infrastructureType;
    private String infrastructureTypeDisplay;

    // Champs DBaaS
    private String dbInstance;
    private String dbVersion;
    private String dbType;

    // Champs NAS
    private String nasType;
    private Integer storageCapacityGb;

    // Réseau
    private String vlanFront;
    private String vlanAdmin;

    // Ressources
    private Integer vcpu;
    private Integer ramGb;

    private String notes;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Logiciels installes
    private List<ServerSoftwareDTO> installedSoftware;
    private int softwareCount;
    private long softwareNeedingUpdate;

    /**
     * Convertit une entite Server en DTO.
     */
    public static ServerDTO fromEntity(Server server) {
        if (server == null) return null;

        return ServerDTO.builder()
                .id(server.getId())
                .resourceName(server.getResourceName())
                .hostname(server.getHostname())
                .ipFront(server.getIpFront())
                .ipAdmin(server.getIpAdmin())
                .ipIlo(server.getIpIlo())
                .refDat(server.getRefDat())
                .serverType(server.getServerType())
                .serverTypeDisplay(server.getServerType() != null ? server.getServerType().getDisplayName() : null)
                .environmentName(server.getEnvironment() != null ? server.getEnvironment().getName() : null)
                .siteCode(server.getSite() != null ? server.getSite().getCode() : null)
                .siteName(server.getSite() != null ? server.getSite().getName() : null)
                .dataCenter(server.getDataCenter())
                .lastAdminUpdate(server.getLastAdminUpdate())
                .os(server.getOs())
                .osVersion(server.getOsVersion())
                .osType(server.getOsType())
                .osTypeDisplay(server.getOsType() != null ? server.getOsType().getDisplayName() : null)
                .infrastructureType(server.getInfrastructureType())
                .infrastructureTypeDisplay(server.getInfrastructureType() != null ? server.getInfrastructureType().getDisplayName() : null)
                .dbInstance(server.getDbInstance())
                .dbVersion(server.getDbVersion())
                .dbType(server.getDbType())
                .nasType(server.getNasType())
                .storageCapacityGb(server.getStorageCapacityGb())
                .vlanFront(server.getVlanFront())
                .vlanAdmin(server.getVlanAdmin())
                .vcpu(server.getVcpu())
                .ramGb(server.getRamGb())
                .notes(server.getNotes())
                .active(server.getActive())
                .createdAt(server.getCreatedAt())
                .updatedAt(server.getUpdatedAt())
                .installedSoftware(server.getInstalledSoftware() != null ?
                        server.getInstalledSoftware().stream()
                                .map(ServerSoftwareDTO::fromEntity)
                                .collect(Collectors.toList()) : null)
                .softwareCount(server.getInstalledSoftware() != null ?
                        server.getInstalledSoftware().size() : 0)
                .softwareNeedingUpdate(server.getInstalledSoftware() != null ?
                        server.getInstalledSoftware().stream()
                                .filter(s -> s.needsUpdate())
                                .count() : 0)
                .build();
    }
}
