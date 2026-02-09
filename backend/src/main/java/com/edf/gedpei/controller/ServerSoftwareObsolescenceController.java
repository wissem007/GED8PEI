package com.edf.gedpei.controller;

import com.edf.gedpei.dto.ServerSoftwareObsolescenceDTO;
import com.edf.gedpei.entity.ServerSoftwareObsolescence.ObsolescenceStatus;
import com.edf.gedpei.service.ServerSoftwareObsolescenceService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller REST pour l'obsolescence logicielle par serveur.
 */
@RestController
@RequestMapping("/api/server-obsolescence")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ServerSoftwareObsolescenceController {

    private final ServerSoftwareObsolescenceService service;
    private final EntityManager entityManager;

    /**
     * Recupere toutes les donnees d'obsolescence.
     */
    @GetMapping
    public ResponseEntity<List<ServerSoftwareObsolescenceDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    /**
     * Filtre les donnees selon les criteres.
     */
    @GetMapping("/filter")
    public ResponseEntity<List<ServerSoftwareObsolescenceDTO>> filter(
            @RequestParam(required = false) String environment,
            @RequestParam(required = false) String serverName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String softwareName) {
        return ResponseEntity.ok(service.filter(environment, serverName, status, softwareName));
    }

    /**
     * Recupere les donnees groupees par serveur (format TCD).
     */
    @GetMapping("/grouped")
    public ResponseEntity<Map<String, Object>> getGroupedByServer() {
        return ResponseEntity.ok(service.getGroupedByServer());
    }

    /**
     * Recupere les statistiques.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(service.getStats());
    }

    /**
     * Recupere les environnements distincts.
     */
    @GetMapping("/environments")
    public ResponseEntity<List<String>> getEnvironments() {
        return ResponseEntity.ok(service.getDistinctEnvironments());
    }

    /**
     * Recupere les noms de serveurs distincts.
     */
    @GetMapping("/servers")
    public ResponseEntity<List<String>> getServers() {
        return ResponseEntity.ok(service.getDistinctServerNames());
    }

    /**
     * Recupere les noms de logiciels distincts.
     */
    @GetMapping("/software-names")
    public ResponseEntity<List<String>> getSoftwareNames() {
        return ResponseEntity.ok(service.getDistinctSoftwareNames());
    }

    /**
     * Recupere les statuts disponibles.
     */
    @GetMapping("/statuses")
    public ResponseEntity<List<Map<String, String>>> getStatuses() {
        List<Map<String, String>> statuses = Arrays.stream(ObsolescenceStatus.values())
                .map(s -> Map.of(
                        "code", s.name(),
                        "label", s.getDisplayName(),
                        "color", s.getColor()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(statuses);
    }

    /**
     * Importe les donnees depuis un fichier CSV.
     */
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importCsv(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Le fichier est vide"));
        }
        return ResponseEntity.ok(service.importFromCsv(file));
    }

    /**
     * Supprime toutes les donnees.
     */
    @DeleteMapping("/clear-all")
    public ResponseEntity<Map<String, Object>> clearAll() {
        long count = service.clearAll();
        return ResponseEntity.ok(Map.of("deleted", count, "message", "Donnees supprimees"));
    }

    /**
     * Recree la table avec les bons types de colonnes.
     */
    @PostMapping("/recreate-table")
    public ResponseEntity<Map<String, Object>> recreateTable() {
        try {
            service.recreateTable(entityManager);
            return ResponseEntity.ok(Map.of("success", true, "message", "Table recree avec succes"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
