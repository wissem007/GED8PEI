package com.edf.gedpei.service;

import com.edf.gedpei.dto.DashboardDTO;
import com.edf.gedpei.dto.ServerDTO;
import com.edf.gedpei.entity.ServerType;
import com.edf.gedpei.repository.ImportHistoryRepository;
import com.edf.gedpei.repository.ServerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service pour le tableau de bord.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final ServerRepository serverRepository;
    private final ImportHistoryRepository importHistoryRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Recupere les donnees du tableau de bord.
     */
    public DashboardDTO getDashboardData() {
        // Compter par type
        long totalServers = serverRepository.countByServerType(ServerType.SERVER);
        long totalNas = serverRepository.countByServerType(ServerType.NAS);
        long totalDbaas = serverRepository.countByServerType(ServerType.DBAAS);
        long totalResources = serverRepository.count();

        // Grouper par environnement
        Map<String, Long> byEnvironment = new HashMap<>();
        serverRepository.countGroupByEnvironment().forEach(row -> {
            String env = row[0] != null ? row[0].toString() : "UNKNOWN";
            Long count = ((Number) row[1]).longValue();
            byEnvironment.put(env, count);
        });

        // Grouper par site
        Map<String, Long> bySite = new HashMap<>();
        serverRepository.countGroupBySite().forEach(row -> {
            String site = row[0] != null ? row[0].toString() : "UNKNOWN";
            Long count = ((Number) row[1]).longValue();
            bySite.put(site, count);
        });

        // Grouper par type
        Map<String, Long> byType = new HashMap<>();
        serverRepository.countGroupByServerType().forEach(row -> {
            ServerType type = (ServerType) row[0];
            Long count = ((Number) row[1]).longValue();
            byType.put(type.getDisplayName(), count);
        });

        // Derniers serveurs ajoutes
        List<ServerDTO> recentServers = serverRepository
                .findAll(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .map(ServerDTO::fromEntity)
                .collect(Collectors.toList());

        // Derniers imports
        List<DashboardDTO.ImportHistoryDTO> recentImports = importHistoryRepository
                .findTop10ByOrderByImportedAtDesc()
                .stream()
                .map(ih -> DashboardDTO.ImportHistoryDTO.builder()
                        .id(ih.getId())
                        .filename(ih.getFilename())
                        .status(ih.getStatus().name())
                        .importedRows(ih.getImportedRows())
                        .importedAt(ih.getImportedAt() != null ?
                                ih.getImportedAt().format(DATE_FORMATTER) : null)
                        .importedBy(ih.getImportedBy())
                        .build())
                .collect(Collectors.toList());

        return DashboardDTO.builder()
                .totalServers(totalServers)
                .totalNas(totalNas)
                .totalDbaas(totalDbaas)
                .totalResources(totalResources)
                .serversByEnvironment(byEnvironment)
                .serversBySite(bySite)
                .serversByType(byType)
                .recentServers(recentServers)
                .recentImports(recentImports)
                .build();
    }

    /**
     * Recupere les donnees pour la cartographie.
     */
    public List<Map<String, Object>> getMapData() {
        Map<String, Long> bySite = new HashMap<>();
        serverRepository.countGroupBySite().forEach(row -> {
            String site = row[0] != null ? row[0].toString() : "UNKNOWN";
            Long count = ((Number) row[1]).longValue();
            bySite.put(site, count);
        });

        return bySite.entrySet().stream()
                .map(entry -> {
                    String siteCode = entry.getKey();
                    double[] coords = com.edf.gedpei.entity.Site.getCoordinates(siteCode);

                    Map<String, Object> siteData = new HashMap<>();
                    siteData.put("code", siteCode);
                    siteData.put("count", entry.getValue());
                    siteData.put("latitude", coords[0]);
                    siteData.put("longitude", coords[1]);
                    return siteData;
                })
                .collect(Collectors.toList());
    }
}
