package com.edf.gedpei.dto;

import com.edf.gedpei.entity.Alert;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AlertDTO {
    private Long id;
    private String alertType;
    private String alertTypeDisplay;
    private String severity;
    private String severityDisplay;
    private String severityColor;
    private String status;
    private String statusDisplay;
    private String serverName;
    private String environment;
    private String softwareName;
    private String currentVersion;
    private String recommendedVersion;
    private String title;
    private String description;
    private String actionRequired;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime acknowledgedAt;
    private String acknowledgedBy;
    private LocalDateTime resolvedAt;
    private String resolvedBy;

    public static AlertDTO fromEntity(Alert alert) {
        return AlertDTO.builder()
                .id(alert.getId())
                .alertType(alert.getAlertType().name())
                .alertTypeDisplay(alert.getAlertType().getDisplayName())
                .severity(alert.getSeverity().name())
                .severityDisplay(alert.getSeverity().getDisplayName())
                .severityColor(alert.getSeverity().getColor())
                .status(alert.getStatus().name())
                .statusDisplay(alert.getStatus().getDisplayName())
                .serverName(alert.getServerName())
                .environment(alert.getEnvironment())
                .softwareName(alert.getSoftwareName())
                .currentVersion(alert.getCurrentVersion())
                .recommendedVersion(alert.getRecommendedVersion())
                .title(alert.getTitle())
                .description(alert.getDescription())
                .actionRequired(alert.getActionRequired())
                .createdAt(alert.getCreatedAt())
                .updatedAt(alert.getUpdatedAt())
                .acknowledgedAt(alert.getAcknowledgedAt())
                .acknowledgedBy(alert.getAcknowledgedBy())
                .resolvedAt(alert.getResolvedAt())
                .resolvedBy(alert.getResolvedBy())
                .build();
    }
}
