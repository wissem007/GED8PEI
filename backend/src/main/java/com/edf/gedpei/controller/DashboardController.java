package com.edf.gedpei.controller;

import com.edf.gedpei.dto.DashboardDTO;
import com.edf.gedpei.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controleur REST pour le tableau de bord.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "API du tableau de bord")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Recupere les donnees du tableau de bord")
    public ResponseEntity<DashboardDTO> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboardData());
    }

    @GetMapping("/map")
    @Operation(summary = "Recupere les donnees pour la cartographie")
    public ResponseEntity<List<Map<String, Object>>> getMapData() {
        return ResponseEntity.ok(dashboardService.getMapData());
    }
}
