package com.edf.gedpei.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * DTO pour le tableau de bord.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardDTO {

    private long totalServers;
    private long totalNas;
    private long totalDbaas;
    private long totalResources;

    private Map<String, Long> serversByEnvironment;
    private Map<String, Long> serversBySite;
    private Map<String, Long> serversByType;

    private List<ServerDTO> recentServers;
    private List<ImportHistoryDTO> recentImports;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImportHistoryDTO {
        private Long id;
        private String filename;
        private String status;
        private Integer importedRows;
        private String importedAt;
        private String importedBy;
    }
}
