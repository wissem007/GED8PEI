package com.edf.gedpei.controller;

import com.edf.gedpei.dto.SoftwareVersionDTO;
import com.edf.gedpei.entity.SoftwareVersion.VersionStatus;
import com.edf.gedpei.service.SoftwareVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityManager;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Controleur REST pour la gestion des versions de logiciels.
 */
@RestController
@RequestMapping("/api/software-versions")
@RequiredArgsConstructor
@Tag(name = "Software Versions", description = "API de gestion des versions de logiciels")
public class SoftwareVersionController {

    private final SoftwareVersionService softwareVersionService;
    private final EntityManager entityManager;

    @GetMapping
    @Operation(summary = "Liste les versions avec pagination et filtres")
    public ResponseEntity<Page<SoftwareVersionDTO>> getVersions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String softwareName,
            @RequestParam(required = false) String version,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "softwareName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<SoftwareVersionDTO> versions = softwareVersionService.filterVersions(
                softwareName, version, status, pageRequest);

        return ResponseEntity.ok(versions);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Recupere une version par son ID")
    public ResponseEntity<SoftwareVersionDTO> getVersionById(@PathVariable Long id) {
        return softwareVersionService.getVersionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/software/{softwareName}")
    @Operation(summary = "Liste les versions d'un logiciel specifique")
    public ResponseEntity<List<SoftwareVersionDTO>> getVersionsBySoftware(
            @PathVariable String softwareName) {
        return ResponseEntity.ok(softwareVersionService.getVersionsBySoftware(softwareName));
    }

    @GetMapping("/software-names")
    @Operation(summary = "Liste les noms de logiciels distincts")
    public ResponseEntity<List<String>> getSoftwareNames() {
        return ResponseEntity.ok(softwareVersionService.getDistinctSoftwareNames());
    }

    @GetMapping("/statuses")
    @Operation(summary = "Liste les statuts disponibles")
    public ResponseEntity<List<Map<String, String>>> getStatuses() {
        List<Map<String, String>> statuses = Arrays.stream(VersionStatus.values())
                .map(s -> Map.of(
                        "code", s.name(),
                        "label", s.getDisplayName()))
                .toList();
        return ResponseEntity.ok(statuses);
    }

    @GetMapping("/stats")
    @Operation(summary = "Statistiques par statut")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(softwareVersionService.getStatsByStatus());
    }

    @PostMapping
    @Operation(summary = "Cree ou met a jour une version")
    public ResponseEntity<SoftwareVersionDTO> createVersion(@RequestBody SoftwareVersionDTO dto) {
        return ResponseEntity.ok(softwareVersionService.saveVersion(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Met a jour une version existante")
    public ResponseEntity<SoftwareVersionDTO> updateVersion(
            @PathVariable Long id,
            @RequestBody SoftwareVersionDTO dto) {
        dto.setId(id);
        return ResponseEntity.ok(softwareVersionService.saveVersion(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprime une version")
    public ResponseEntity<Void> deleteVersion(@PathVariable Long id) {
        softwareVersionService.deleteVersion(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import")
    @Operation(summary = "Importe des versions depuis un fichier CSV")
    public ResponseEntity<Map<String, Object>> importCsv(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Le fichier est vide"));
        }

        try {
            Map<String, Object> result = softwareVersionService.importFromCsv(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getClass().getSimpleName(),
                                 "message", e.getMessage() != null ? e.getMessage() : "Unknown error",
                                 "trace", e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "No trace"));
        }
    }

    @DeleteMapping("/clear-all")
    @Operation(summary = "Supprime toutes les versions (admin)")
    public ResponseEntity<Map<String, Object>> clearAll() {
        long count = softwareVersionService.clearAll();
        return ResponseEntity.ok(Map.of("deleted", count, "message", "Toutes les versions ont ete supprimees"));
    }

    @PostMapping("/clear-all")
    @Operation(summary = "Supprime toutes les versions via POST (admin)")
    public ResponseEntity<Map<String, Object>> clearAllPost() {
        long count = softwareVersionService.clearAll();
        return ResponseEntity.ok(Map.of("deleted", count, "message", "Toutes les versions ont ete supprimees"));
    }

    @PostMapping("/update-schema")
    @Operation(summary = "Met a jour le schema de la table (admin)")
    public ResponseEntity<Map<String, Object>> updateSchema() {
        softwareVersionService.updateSchema(entityManager);
        return ResponseEntity.ok(Map.of("message", "Schema mis a jour"));
    }
}
