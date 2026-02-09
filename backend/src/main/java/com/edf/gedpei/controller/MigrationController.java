package com.edf.gedpei.controller;

import com.edf.gedpei.dto.MigrationCreateDTO;
import com.edf.gedpei.dto.MigrationDTO;
import com.edf.gedpei.entity.Migration.MigrationStatus;
import com.edf.gedpei.service.MigrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Controleur REST pour la gestion des migrations.
 */
@RestController
@RequestMapping("/api/migrations")
@RequiredArgsConstructor
@Tag(name = "Migrations", description = "API de gestion du planning des migrations")
public class MigrationController {

    private final MigrationService migrationService;

    @GetMapping
    @Operation(summary = "Liste toutes les migrations avec pagination")
    public ResponseEntity<Page<MigrationDTO>> getAllMigrations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "dateDebut") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<MigrationDTO> migrations;
        if (search != null && !search.isBlank()) {
            migrations = migrationService.searchMigrations(search, pageable);
        } else {
            migrations = migrationService.getAllMigrations(pageable);
        }

        return ResponseEntity.ok(migrations);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Recupere une migration par son ID")
    public ResponseEntity<MigrationDTO> getMigrationById(@PathVariable Long id) {
        return ResponseEntity.ok(migrationService.getMigrationById(id));
    }

    @GetMapping("/active")
    @Operation(summary = "Liste les migrations actives")
    public ResponseEntity<List<MigrationDTO>> getActiveMigrations() {
        return ResponseEntity.ok(migrationService.getActiveMigrations());
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Liste les migrations par statut")
    public ResponseEntity<List<MigrationDTO>> getMigrationsByStatus(@PathVariable MigrationStatus status) {
        return ResponseEntity.ok(migrationService.getMigrationsByStatus(status));
    }

    @PostMapping
    @Operation(summary = "Cree une nouvelle migration")
    public ResponseEntity<MigrationDTO> createMigration(
            @Valid @RequestBody MigrationCreateDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : "anonymous";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(migrationService.createMigration(dto, username));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Met a jour une migration")
    public ResponseEntity<MigrationDTO> updateMigration(
            @PathVariable Long id,
            @Valid @RequestBody MigrationCreateDTO dto) {
        return ResponseEntity.ok(migrationService.updateMigration(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprime une migration")
    public ResponseEntity<Void> deleteMigration(@PathVariable Long id) {
        migrationService.deleteMigration(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/phases")
    @Operation(summary = "Liste toutes les phases distinctes")
    public ResponseEntity<List<String>> getAllPhases() {
        return ResponseEntity.ok(migrationService.getAllPhases());
    }

    @GetMapping("/responsables")
    @Operation(summary = "Liste tous les responsables distincts")
    public ResponseEntity<List<String>> getAllResponsables() {
        return ResponseEntity.ok(migrationService.getAllResponsables());
    }

    @GetMapping("/statuses")
    @Operation(summary = "Liste tous les statuts possibles")
    public ResponseEntity<List<Map<String, String>>> getAllStatuses() {
        return ResponseEntity.ok(migrationService.getAllStatuses());
    }

    @GetMapping("/stats")
    @Operation(summary = "Statistiques des migrations")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(migrationService.getStats());
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importe des migrations depuis un fichier CSV")
    public ResponseEntity<Map<String, Object>> importCsv(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : "anonymous";
        Map<String, Object> result = migrationService.importFromCsv(file, username);
        return ResponseEntity.ok(result);
    }
}
