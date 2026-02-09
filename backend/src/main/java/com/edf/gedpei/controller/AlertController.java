package com.edf.gedpei.controller;

import com.edf.gedpei.dto.AlertDTO;
import com.edf.gedpei.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller REST pour la gestion des alertes.
 */
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AlertController {

    private final AlertService alertService;

    /**
     * Recupere toutes les alertes.
     */
    @GetMapping
    public ResponseEntity<List<AlertDTO>> getAll() {
        return ResponseEntity.ok(alertService.getAll());
    }

    /**
     * Recupere les alertes actives triees par severite.
     */
    @GetMapping("/active")
    public ResponseEntity<List<AlertDTO>> getActiveAlerts() {
        return ResponseEntity.ok(alertService.getActiveAlerts());
    }

    /**
     * Filtre les alertes selon les criteres.
     */
    @GetMapping("/filter")
    public ResponseEntity<List<AlertDTO>> filter(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) String serverName,
            @RequestParam(required = false) String environment) {
        return ResponseEntity.ok(alertService.filter(status, severity, alertType, serverName, environment));
    }

    /**
     * Recupere les statistiques des alertes.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(alertService.getStats());
    }

    /**
     * Genere les alertes automatiquement a partir des donnees d'obsolescence.
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateAlerts() {
        return ResponseEntity.ok(alertService.generateAlerts());
    }

    /**
     * Acquitte une alerte.
     */
    @PutMapping("/{id}/acknowledge")
    public ResponseEntity<AlertDTO> acknowledgeAlert(
            @PathVariable Long id,
            @RequestParam(defaultValue = "system") String username) {
        return ResponseEntity.ok(alertService.acknowledgeAlert(id, username));
    }

    /**
     * Resout une alerte.
     */
    @PutMapping("/{id}/resolve")
    public ResponseEntity<AlertDTO> resolveAlert(
            @PathVariable Long id,
            @RequestParam(defaultValue = "system") String username) {
        return ResponseEntity.ok(alertService.resolveAlert(id, username));
    }

    /**
     * Ignore une alerte.
     */
    @PutMapping("/{id}/ignore")
    public ResponseEntity<AlertDTO> ignoreAlert(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.ignoreAlert(id));
    }

    /**
     * Supprime les alertes resolues.
     */
    @DeleteMapping("/clear-resolved")
    public ResponseEntity<Map<String, Object>> clearResolvedAlerts() {
        long count = alertService.clearResolvedAlerts();
        return ResponseEntity.ok(Map.of("deleted", count, "message", "Alertes resolues supprimees"));
    }

    /**
     * Supprime toutes les alertes.
     */
    @DeleteMapping("/clear-all")
    public ResponseEntity<Map<String, Object>> clearAllAlerts() {
        long count = alertService.clearAllAlerts();
        return ResponseEntity.ok(Map.of("deleted", count, "message", "Toutes les alertes supprimees"));
    }

    /**
     * Recupere les types d'alertes disponibles.
     */
    @GetMapping("/types")
    public ResponseEntity<List<Map<String, String>>> getAlertTypes() {
        return ResponseEntity.ok(alertService.getAlertTypes());
    }

    /**
     * Recupere les severites disponibles.
     */
    @GetMapping("/severities")
    public ResponseEntity<List<Map<String, String>>> getSeverities() {
        return ResponseEntity.ok(alertService.getSeverities());
    }

    /**
     * Recupere les statuts disponibles.
     */
    @GetMapping("/statuses")
    public ResponseEntity<List<Map<String, String>>> getStatuses() {
        return ResponseEntity.ok(alertService.getStatuses());
    }
}
