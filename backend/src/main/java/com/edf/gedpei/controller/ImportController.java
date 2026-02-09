package com.edf.gedpei.controller;

import com.edf.gedpei.entity.ImportHistory;
import com.edf.gedpei.repository.ImportHistoryRepository;
import com.edf.gedpei.service.ImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Controleur REST pour l'import de fichiers.
 */
@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
@Tag(name = "Import", description = "API d'import de fichiers CSV/Excel")
public class ImportController {

    private final ImportService importService;
    private final ImportHistoryRepository importHistoryRepository;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importe un fichier CSV ou Excel")
    public ResponseEntity<Map<String, Object>> importFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Fichier vide"));
        }

        String username = userDetails != null ? userDetails.getUsername() : "anonymous";

        ImportHistory result = importService.importFile(file, username);

        return ResponseEntity.ok(Map.of(
                "id", result.getId(),
                "filename", result.getFilename(),
                "status", result.getStatus().name(),
                "totalRows", result.getTotalRows() != null ? result.getTotalRows() : 0,
                "importedRows", result.getImportedRows() != null ? result.getImportedRows() : 0,
                "skippedRows", result.getSkippedRows() != null ? result.getSkippedRows() : 0,
                "errorRows", result.getErrorRows() != null ? result.getErrorRows() : 0,
                "durationMs", result.getDurationMs() != null ? result.getDurationMs() : 0,
                "errorMessage", result.getErrorMessage() != null ? result.getErrorMessage() : ""
        ));
    }

    @GetMapping("/history")
    @Operation(summary = "Historique des imports")
    public ResponseEntity<Page<ImportHistory>> getImportHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<ImportHistory> history = importHistoryRepository
                .findAllByOrderByImportedAtDesc(PageRequest.of(page, size));

        return ResponseEntity.ok(history);
    }

    @GetMapping("/history/{id}")
    @Operation(summary = "Details d'un import")
    public ResponseEntity<ImportHistory> getImportById(@PathVariable Long id) {
        return importHistoryRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
