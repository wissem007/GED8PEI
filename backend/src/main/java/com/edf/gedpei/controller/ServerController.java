package com.edf.gedpei.controller;

import com.edf.gedpei.dto.ServerCreateDTO;
import com.edf.gedpei.dto.ServerDTO;
import com.edf.gedpei.entity.ServerType;
import com.edf.gedpei.service.ServerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controleur REST pour les serveurs.
 */
@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
@Tag(name = "Servers", description = "API de gestion des serveurs")
public class ServerController {

    private final ServerService serverService;

    @GetMapping
    @Operation(summary = "Liste tous les serveurs avec pagination et filtres")
    public ResponseEntity<Page<ServerDTO>> getAllServers(
            @RequestParam(required = false) String environment,
            @RequestParam(required = false) String site,
            @RequestParam(required = false) ServerType type,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "resourceName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ServerDTO> servers = serverService.filterServers(
                environment, site, type, search, pageable);

        return ResponseEntity.ok(servers);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Recupere un serveur par son ID")
    public ResponseEntity<ServerDTO> getServerById(@PathVariable Long id) {
        return ResponseEntity.ok(serverService.getServerById(id));
    }

    @GetMapping("/search")
    @Operation(summary = "Recherche des serveurs")
    public ResponseEntity<Page<ServerDTO>> searchServers(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(serverService.searchServers(q, pageable));
    }

    @PostMapping
    @Operation(summary = "Cree un nouveau serveur")
    public ResponseEntity<ServerDTO> createServer(
            @Valid @RequestBody ServerCreateDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        String username = userDetails != null ? userDetails.getUsername() : "system";
        ServerDTO created = serverService.createServer(dto, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Met a jour un serveur")
    public ResponseEntity<ServerDTO> updateServer(
            @PathVariable Long id,
            @Valid @RequestBody ServerCreateDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        String username = userDetails != null ? userDetails.getUsername() : "system";
        return ResponseEntity.ok(serverService.updateServer(id, dto, username));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprime un serveur")
    public ResponseEntity<Void> deleteServer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        String username = userDetails != null ? userDetails.getUsername() : "system";
        serverService.deleteServer(id, username);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/environments")
    @Operation(summary = "Liste tous les environnements")
    public ResponseEntity<List<String>> getEnvironments() {
        return ResponseEntity.ok(serverService.getAllEnvironments());
    }

    @GetMapping("/sites")
    @Operation(summary = "Liste tous les sites")
    public ResponseEntity<List<String>> getSites() {
        return ResponseEntity.ok(serverService.getAllSites());
    }

    @GetMapping("/types")
    @Operation(summary = "Liste tous les types de serveurs")
    public ResponseEntity<List<Map<String, String>>> getServerTypes() {
        List<Map<String, String>> types = List.of(
                Map.of("value", "SERVER", "label", "Serveur"),
                Map.of("value", "NAS", "label", "NAS"),
                Map.of("value", "DBAAS", "label", "Base de donnees DBaaS")
        );
        return ResponseEntity.ok(types);
    }
}
