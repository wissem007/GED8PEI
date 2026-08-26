package com.edf.gedpei.controller;

import com.edf.gedpei.dto.ComplianceDTO;
import com.edf.gedpei.service.ComplianceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Controller REST pour le rapprochement versions installees / referentiel CSR.
 */
@RestController
@RequestMapping("/api/compliance")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ComplianceController {

    private final ComplianceService service;

    /**
     * Ecart de chaque composant installe par rapport a sa cible CSR,
     * du plus grave au plus sain.
     */
    @GetMapping
    public ResponseEntity<List<ComplianceDTO>> getCompliance() {
        return ResponseEntity.ok(service.getCompliance());
    }

    /**
     * Repartition des composants par verdict.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(service.getStats());
    }
}
