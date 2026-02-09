package com.edf.gedpei.dto;

import com.edf.gedpei.entity.SoftwareVersion;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO pour les versions de logiciels.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoftwareVersionDTO {

    private Long id;
    private String softwareName;
    private String version;
    private String packageVersion;
    private String status;
    private String statusDisplay;
    private LocalDate statusUpdateDate;
    private LocalDate initialSupportEndDate;
    private LocalDate extendedSupportEndDate;
    private LocalDate cyberSupportEndDate;
    private String notes;
    private String usefulLinks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Convertit une entite en DTO.
     */
    public static SoftwareVersionDTO fromEntity(SoftwareVersion entity) {
        if (entity == null) return null;

        return SoftwareVersionDTO.builder()
                .id(entity.getId())
                .softwareName(entity.getSoftwareName())
                .version(entity.getVersion())
                .packageVersion(entity.getPackageVersion())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .statusDisplay(entity.getStatus() != null ? entity.getStatus().getDisplayName() : null)
                .statusUpdateDate(entity.getStatusUpdateDate())
                .initialSupportEndDate(entity.getInitialSupportEndDate())
                .extendedSupportEndDate(entity.getExtendedSupportEndDate())
                .cyberSupportEndDate(entity.getCyberSupportEndDate())
                .notes(entity.getNotes())
                .usefulLinks(entity.getUsefulLinks())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Convertit un DTO en entite.
     */
    public SoftwareVersion toEntity() {
        SoftwareVersion entity = new SoftwareVersion();
        entity.setId(this.id);
        entity.setSoftwareName(this.softwareName);
        entity.setVersion(this.version);
        entity.setPackageVersion(this.packageVersion);
        entity.setStatus(this.status != null ?
                SoftwareVersion.VersionStatus.fromString(this.status) :
                SoftwareVersion.VersionStatus.TOLEREE);
        entity.setStatusUpdateDate(this.statusUpdateDate);
        entity.setInitialSupportEndDate(this.initialSupportEndDate);
        entity.setExtendedSupportEndDate(this.extendedSupportEndDate);
        entity.setCyberSupportEndDate(this.cyberSupportEndDate);
        entity.setNotes(this.notes);
        entity.setUsefulLinks(this.usefulLinks);
        return entity;
    }
}
