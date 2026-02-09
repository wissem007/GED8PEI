package com.edf.gedpei.dto;

import com.edf.gedpei.entity.Server.InfrastructureType;
import com.edf.gedpei.entity.ServerType;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;

/**
 * DTO pour la creation/modification d'un serveur.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServerCreateDTO {

    @NotBlank(message = "Le nom de la ressource est obligatoire")
    private String resourceName;

    private String hostname;

    private String ipFront;

    private String ipAdmin;

    private String ipIlo;

    private String refDat;

    private ServerType serverType;

    private String environmentName;

    private String siteCode;

    private String dataCenter;

    private LocalDate lastAdminUpdate;

    private String operatingSystem;

    private String osVersion;

    private InfrastructureType infrastructureType;

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
    private Boolean enabled;
}
