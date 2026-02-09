package com.edf.gedpei.service;

import com.edf.gedpei.dto.AlertDTO;
import com.edf.gedpei.entity.Alert;
import com.edf.gedpei.entity.Alert.AlertSeverity;
import com.edf.gedpei.entity.Alert.AlertStatus;
import com.edf.gedpei.entity.Alert.AlertType;
import com.edf.gedpei.entity.ServerSoftwareObsolescence;
import com.edf.gedpei.entity.ServerSoftwareObsolescence.ObsolescenceStatus;
import com.edf.gedpei.repository.AlertRepository;
import com.edf.gedpei.repository.ServerSoftwareObsolescenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service pour la gestion des alertes et la detection automatique des non-conformites.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;
    private final ServerSoftwareObsolescenceRepository obsolescenceRepository;

    /**
     * Recupere toutes les alertes.
     */
    public List<AlertDTO> getAll() {
        return alertRepository.findAll().stream()
                .map(AlertDTO::fromEntity)
                .toList();
    }

    /**
     * Recupere les alertes actives triees par severite.
     */
    public List<AlertDTO> getActiveAlerts() {
        return alertRepository.findActiveAlertsSortedBySeverity(AlertStatus.ACTIVE).stream()
                .map(AlertDTO::fromEntity)
                .toList();
    }

    /**
     * Filtre les alertes.
     */
    public List<AlertDTO> filter(String status, String severity, String alertType,
                                  String serverName, String environment) {
        AlertStatus alertStatus = null;
        AlertSeverity alertSeverity = null;
        AlertType type = null;

        if (status != null && !status.isBlank()) {
            try {
                alertStatus = AlertStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Statut invalide: {}", status);
            }
        }

        if (severity != null && !severity.isBlank()) {
            try {
                alertSeverity = AlertSeverity.valueOf(severity.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Severite invalide: {}", severity);
            }
        }

        if (alertType != null && !alertType.isBlank()) {
            try {
                type = AlertType.valueOf(alertType.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Type d'alerte invalide: {}", alertType);
            }
        }

        return alertRepository.filter(
                alertStatus,
                alertSeverity,
                type,
                serverName != null && !serverName.isBlank() ? serverName : null,
                environment != null && !environment.isBlank() ? environment : null
        ).stream()
                .map(AlertDTO::fromEntity)
                .toList();
    }

    /**
     * Recupere les statistiques des alertes.
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // Comptages par statut
        stats.put("activeCount", alertRepository.countByStatus(AlertStatus.ACTIVE));
        stats.put("acknowledgedCount", alertRepository.countByStatus(AlertStatus.ACKNOWLEDGED));
        stats.put("resolvedCount", alertRepository.countByStatus(AlertStatus.RESOLVED));

        // Comptages par severite (alertes actives uniquement)
        Map<String, Long> bySeverity = new LinkedHashMap<>();
        for (AlertSeverity sev : AlertSeverity.values()) {
            bySeverity.put(sev.getDisplayName(), alertRepository.countByStatusAndSeverity(AlertStatus.ACTIVE, sev));
        }
        stats.put("bySeverity", bySeverity);

        // Comptages par type (alertes actives)
        Map<String, Long> byType = new LinkedHashMap<>();
        for (Object[] row : alertRepository.countByType(AlertStatus.ACTIVE)) {
            AlertType type = (AlertType) row[0];
            Long count = (Long) row[1];
            byType.put(type.getDisplayName(), count);
        }
        stats.put("byType", byType);

        // Comptages par serveur (top 10)
        List<Map<String, Object>> byServer = new ArrayList<>();
        int count = 0;
        for (Object[] row : alertRepository.countByServer(AlertStatus.ACTIVE)) {
            if (count++ >= 10) break;
            byServer.add(Map.of("server", row[0], "count", row[1]));
        }
        stats.put("byServer", byServer);

        // Comptages par environnement
        Map<String, Long> byEnvironment = new LinkedHashMap<>();
        for (Object[] row : alertRepository.countByEnvironment(AlertStatus.ACTIVE)) {
            byEnvironment.put((String) row[0], (Long) row[1]);
        }
        stats.put("byEnvironment", byEnvironment);

        return stats;
    }

    /**
     * Acquitte une alerte.
     */
    @Transactional
    public AlertDTO acknowledgeAlert(Long id, String username) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alerte non trouvee: " + id));

        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedAt(LocalDateTime.now());
        alert.setAcknowledgedBy(username);

        return AlertDTO.fromEntity(alertRepository.save(alert));
    }

    /**
     * Resout une alerte.
     */
    @Transactional
    public AlertDTO resolveAlert(Long id, String username) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alerte non trouvee: " + id));

        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedAt(LocalDateTime.now());
        alert.setResolvedBy(username);

        return AlertDTO.fromEntity(alertRepository.save(alert));
    }

    /**
     * Ignore une alerte.
     */
    @Transactional
    public AlertDTO ignoreAlert(Long id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alerte non trouvee: " + id));

        alert.setStatus(AlertStatus.IGNORED);
        return AlertDTO.fromEntity(alertRepository.save(alert));
    }

    /**
     * Genere les alertes automatiquement en analysant les donnees d'obsolescence.
     * Cette methode compare les versions installees avec les statuts d'obsolescence.
     */
    @Transactional
    public Map<String, Object> generateAlerts() {
        int created = 0;
        int updated = 0;
        int skipped = 0;

        List<ServerSoftwareObsolescence> obsolescenceData = obsolescenceRepository.findAll();

        for (ServerSoftwareObsolescence item : obsolescenceData) {
            ObsolescenceStatus obsStatus = item.getStatus();

            // Determiner le type d'alerte et la severite en fonction du statut d'obsolescence
            AlertType alertType = null;
            AlertSeverity severity = null;
            String title = null;
            String description = null;
            String actionRequired = null;

            switch (obsStatus) {
                case INTERDITE_CYBER:
                    alertType = AlertType.VERSION_INTERDITE;
                    severity = AlertSeverity.CRITICAL;
                    title = "Version INTERDITE CYBER: " + item.getSoftwareName();
                    description = String.format(
                            "Le logiciel %s version %s sur le serveur %s est INTERDIT pour des raisons de cybersecurite.",
                            item.getSoftwareName(), item.getCurrentVersion(), item.getServerName());
                    actionRequired = String.format("Mettre a jour vers la version %s immediatement",
                            item.getOsTargetSolutionVersion() != null ? item.getOsTargetSolutionVersion() : "recommandee");
                    break;

                case INTERDITE:
                    alertType = AlertType.VERSION_INTERDITE;
                    severity = AlertSeverity.HIGH;
                    title = "Version INTERDITE: " + item.getSoftwareName();
                    description = String.format(
                            "Le logiciel %s version %s sur le serveur %s utilise une version interdite.",
                            item.getSoftwareName(), item.getCurrentVersion(), item.getServerName());
                    actionRequired = String.format("Planifier la mise a jour vers %s",
                            item.getOsTargetSolutionVersion() != null ? item.getOsTargetSolutionVersion() : "une version autorisee");
                    break;

                case OBSOLETE:
                    alertType = AlertType.VERSION_OBSOLETE;
                    severity = AlertSeverity.HIGH;
                    title = "Version OBSOLETE: " + item.getSoftwareName();
                    description = String.format(
                            "Le logiciel %s version %s sur le serveur %s est obsolete (fin de support: %s).",
                            item.getSoftwareName(), item.getCurrentVersion(), item.getServerName(),
                            item.getSupportEndDate() != null ? item.getSupportEndDate() : "N/A");
                    actionRequired = "Planifier la migration vers une version supportee";
                    break;

                case RISQUEE:
                    alertType = AlertType.VERSION_RISQUEE;
                    severity = AlertSeverity.MEDIUM;
                    title = "Version RISQUEE: " + item.getSoftwareName();
                    description = String.format(
                            "Le logiciel %s version %s sur le serveur %s presente des risques (fin de support: %s).",
                            item.getSoftwareName(), item.getCurrentVersion(), item.getServerName(),
                            item.getSupportEndDate() != null ? item.getSupportEndDate() : "N/A");
                    actionRequired = String.format("Envisager la mise a jour vers %s",
                            item.getOsActualTargetVersion() != null ? item.getOsActualTargetVersion() : "une version plus recente");
                    break;

                case TOLEREE:
                    // Verifier si la fin de support est proche
                    if (isEndOfSupportSoon(item.getSupportEndDate())) {
                        alertType = AlertType.FIN_SUPPORT_PROCHE;
                        severity = AlertSeverity.LOW;
                        title = "Fin de support proche: " + item.getSoftwareName();
                        description = String.format(
                                "Le logiciel %s version %s sur le serveur %s arrive en fin de support: %s.",
                                item.getSoftwareName(), item.getCurrentVersion(), item.getServerName(),
                                item.getSupportEndDate());
                        actionRequired = "Planifier la mise a jour avant la fin de support";
                    }
                    break;

                case TRAJECTOIRE_PRECONISEE:
                    // Version en trajectoire - alerte informative
                    alertType = AlertType.MISE_A_JOUR_REQUISE;
                    severity = AlertSeverity.INFO;
                    title = "Mise a jour recommandee: " + item.getSoftwareName();
                    description = String.format(
                            "Le logiciel %s version %s sur le serveur %s doit etre mis a jour vers la version preconisee.",
                            item.getSoftwareName(), item.getCurrentVersion(), item.getServerName());
                    actionRequired = String.format("Mettre a jour vers %s selon le planning",
                            item.getOsTargetSolutionVersion() != null ? item.getOsTargetSolutionVersion() : "la version preconisee");
                    break;

                case PRECONISEE:
                    // Version preconisee - pas d'alerte necessaire
                    continue;

                default:
                    continue;
            }

            if (alertType != null) {
                // Verifier si une alerte similaire existe deja
                boolean exists = alertRepository.existsByServerNameAndSoftwareNameAndAlertTypeAndStatus(
                        item.getServerName(), item.getSoftwareName(), alertType, AlertStatus.ACTIVE);

                if (!exists) {
                    // Verifier aussi pour ACKNOWLEDGED
                    exists = alertRepository.existsByServerNameAndSoftwareNameAndAlertTypeAndStatus(
                            item.getServerName(), item.getSoftwareName(), alertType, AlertStatus.ACKNOWLEDGED);
                }

                if (!exists) {
                    Alert alert = Alert.builder()
                            .alertType(alertType)
                            .severity(severity)
                            .status(AlertStatus.ACTIVE)
                            .serverName(item.getServerName())
                            .environment(item.getEnvironment())
                            .softwareName(item.getSoftwareName())
                            .currentVersion(item.getCurrentVersion())
                            .recommendedVersion(item.getOsTargetSolutionVersion())
                            .title(title)
                            .description(description)
                            .actionRequired(actionRequired)
                            .build();

                    alertRepository.save(alert);
                    created++;
                    log.debug("Alerte creee: {} - {} - {}", item.getServerName(), item.getSoftwareName(), alertType);
                } else {
                    skipped++;
                }
            }
        }

        log.info("Generation des alertes terminee: {} creees, {} ignorees (deja existantes)", created, skipped);

        return Map.of(
                "created", created,
                "skipped", skipped,
                "message", String.format("%d alertes creees, %d ignorees", created, skipped)
        );
    }

    /**
     * Verifie si la fin de support est proche (moins de 6 mois).
     */
    private boolean isEndOfSupportSoon(String supportEndDate) {
        if (supportEndDate == null || supportEndDate.isBlank()) {
            return false;
        }

        try {
            // Format attendu: T1-2026, T2-2026, etc.
            String[] parts = supportEndDate.split("-");
            if (parts.length != 2) return false;

            int quarter = Integer.parseInt(parts[0].replace("T", ""));
            int year = Integer.parseInt(parts[1]);

            // Calculer le mois de fin de support
            int endMonth = quarter * 3;
            LocalDateTime endOfSupport = LocalDateTime.of(year, endMonth, 1, 0, 0);

            // Verifier si c'est dans les 6 prochains mois
            LocalDateTime sixMonthsFromNow = LocalDateTime.now().plusMonths(6);
            return endOfSupport.isBefore(sixMonthsFromNow);
        } catch (Exception e) {
            log.debug("Impossible de parser la date de fin de support: {}", supportEndDate);
            return false;
        }
    }

    /**
     * Supprime toutes les alertes resolues.
     */
    @Transactional
    public long clearResolvedAlerts() {
        List<Alert> resolved = alertRepository.findByStatus(AlertStatus.RESOLVED);
        long count = resolved.size();
        alertRepository.deleteAll(resolved);
        log.info("Suppression de {} alertes resolues", count);
        return count;
    }

    /**
     * Supprime toutes les alertes.
     */
    @Transactional
    public long clearAllAlerts() {
        long count = alertRepository.count();
        alertRepository.deleteAll();
        log.info("Suppression de {} alertes", count);
        return count;
    }

    /**
     * Recupere les types d'alertes disponibles.
     */
    public List<Map<String, String>> getAlertTypes() {
        return Arrays.stream(AlertType.values())
                .map(t -> Map.of(
                        "code", t.name(),
                        "label", t.getDisplayName(),
                        "description", t.getDescription()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Recupere les severites disponibles.
     */
    public List<Map<String, String>> getSeverities() {
        return Arrays.stream(AlertSeverity.values())
                .map(s -> Map.of(
                        "code", s.name(),
                        "label", s.getDisplayName(),
                        "color", s.getColor()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Recupere les statuts disponibles.
     */
    public List<Map<String, String>> getStatuses() {
        return Arrays.stream(AlertStatus.values())
                .map(s -> Map.of(
                        "code", s.name(),
                        "label", s.getDisplayName()
                ))
                .collect(Collectors.toList());
    }
}
