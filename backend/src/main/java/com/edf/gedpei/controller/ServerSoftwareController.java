package com.edf.gedpei.controller;

import com.edf.gedpei.dto.ServerSoftwareCreateDTO;
import com.edf.gedpei.dto.ServerSoftwareDTO;
import com.edf.gedpei.service.ServerSoftwareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controleur REST pour les logiciels installes sur les serveurs.
 */
@RestController
@RequestMapping("/api/server-software")
@RequiredArgsConstructor
@Tag(name = "Server Software", description = "API de gestion des logiciels serveurs")
public class ServerSoftwareController {

    private final ServerSoftwareService softwareService;

    @GetMapping("/server/{serverId}")
    @Operation(summary = "Liste les logiciels d'un serveur")
    public ResponseEntity<List<ServerSoftwareDTO>> getSoftwareByServer(@PathVariable Long serverId) {
        return ResponseEntity.ok(softwareService.getSoftwareByServer(serverId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Recupere un logiciel par son ID")
    public ResponseEntity<ServerSoftwareDTO> getSoftwareById(@PathVariable Long id) {
        return ResponseEntity.ok(softwareService.getSoftwareById(id));
    }

    @PostMapping
    @Operation(summary = "Ajoute un logiciel a un serveur")
    public ResponseEntity<ServerSoftwareDTO> createSoftware(@Valid @RequestBody ServerSoftwareCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(softwareService.createSoftware(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Met a jour un logiciel")
    public ResponseEntity<ServerSoftwareDTO> updateSoftware(
            @PathVariable Long id,
            @Valid @RequestBody ServerSoftwareCreateDTO dto) {
        return ResponseEntity.ok(softwareService.updateSoftware(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprime un logiciel")
    public ResponseEntity<Void> deleteSoftware(@PathVariable Long id) {
        softwareService.deleteSoftware(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/names")
    @Operation(summary = "Liste tous les noms de logiciels distincts")
    public ResponseEntity<List<String>> getAllSoftwareNames() {
        return ResponseEntity.ok(softwareService.getAllSoftwareNames());
    }

    @GetMapping("/needs-update")
    @Operation(summary = "Liste les logiciels necessitant une MAJ")
    public ResponseEntity<List<ServerSoftwareDTO>> getSoftwareNeedingUpdate() {
        return ResponseEntity.ok(softwareService.getSoftwareNeedingUpdate());
    }

    @GetMapping("/server/{serverId}/updates-count")
    @Operation(summary = "Compte les MAJ requises pour un serveur")
    public ResponseEntity<Long> countUpdatesRequired(@PathVariable Long serverId) {
        return ResponseEntity.ok(softwareService.countUpdatesRequired(serverId));
    }
}
