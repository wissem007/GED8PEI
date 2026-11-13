package com.edf.gedpei.service;

import com.edf.gedpei.entity.Server;
import com.edf.gedpei.entity.ServerSoftwareObsolescence;
import com.edf.gedpei.entity.ServerSoftwareObsolescence.ObsolescenceStatus;
import com.edf.gedpei.entity.SoftwareVersion;
import com.edf.gedpei.repository.ServerRepository;
import com.edf.gedpei.repository.ServerSoftwareObsolescenceRepository;
import com.edf.gedpei.util.WindowsCollectParser;
import com.edf.gedpei.util.WindowsCollectParser.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Importe les rapports du script de collecte Windows en lecture seule dans la table
 * d'obsolescence, a cote des lignes Prevobs (source = COLLECTE_WINDOWS).
 *
 * <p>Chaque import d'un serveur remplace sa collecte precedente : la table reflete
 * toujours le dernier etat releve, et les lignes Prevobs ne sont jamais touchees.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WindowsCollectImportService {

    public static final String SOURCE = "COLLECTE_WINDOWS";

    /** Gravite croissante : un constat de la collecte ne peut qu'aggraver le statut CSR. */
    private static final List<ObsolescenceStatus> SEVERITY = List.of(
            ObsolescenceStatus.PRECONISEE, ObsolescenceStatus.TRAJECTOIRE_PRECONISEE,
            ObsolescenceStatus.TOLEREE, ObsolescenceStatus.NON_REFERENCEE, ObsolescenceStatus.RISQUEE,
            ObsolescenceStatus.OBSOLETE, ObsolescenceStatus.INTERDITE, ObsolescenceStatus.INTERDITE_CYBER);

    private static final DateTimeFormatter FR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ServerSoftwareObsolescenceRepository repository;
    private final ServerRepository serverRepository;
    private final ComplianceService complianceService;

    @Transactional
    public Map<String, Object> importFiles(List<MultipartFile> files) {
        List<Map<String, Object>> servers = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int total = 0;

        for (MultipartFile file : files) {
            try {
                String text = new String(file.getBytes(), StandardCharsets.UTF_8);
                WindowsCollectParser.Result result = WindowsCollectParser.parse(text);
                String serverName = resolveServerName(result.hostname());
                String environment = resolveEnvironment(serverName);

                int removed = repository.deleteByServerNameAndSource(serverName, SOURCE);
                int saved = 0;
                Map<String, Integer> byStatus = new TreeMap<>();
                for (Component c : result.components()) {
                    ServerSoftwareObsolescence row = toRow(c, serverName, environment, result);
                    repository.save(row);
                    saved++;
                    byStatus.merge(row.getStatus().name(), 1, Integer::sum);
                }
                total += saved;

                Map<String, Object> summary = new LinkedHashMap<>();
                summary.put("file", file.getOriginalFilename());
                summary.put("server", serverName);
                summary.put("environment", environment);
                summary.put("collectedAt", result.collectedAt());
                summary.put("os", result.os());
                summary.put("components", saved);
                summary.put("replaced", removed);
                summary.put("byStatus", byStatus);
                servers.add(summary);
                log.info("Collecte Windows {} importee : {} lignes ({} remplacees)", serverName, saved, removed);
            } catch (Exception e) {
                log.warn("Collecte Windows {} rejetee : {}", file.getOriginalFilename(), e.getMessage());
                errors.add(file.getOriginalFilename() + " : " + e.getMessage());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imported", total);
        result.put("servers", servers);
        result.put("errors", errors.size());
        result.put("errorMessages", errors);
        return result;
    }

    private ServerSoftwareObsolescence toRow(Component c, String serverName, String environment,
                                             WindowsCollectParser.Result result) {
        ObsolescenceStatus status = ObsolescenceStatus.NON_REFERENCEE;
        String supportEnd = c.supportEndDate();
        String target = null;

        if (!"constat".equals(c.role())) {
            Optional<SoftwareVersion> csr = complianceService.findCatalogueVersion(c.softwareName(), c.version());
            if (csr.isPresent()) {
                SoftwareVersion v = csr.get();
                status = complianceService.matchPackage(v, c.version())
                        .map(m -> ObsolescenceStatus.fromString(m.status()))
                        .orElse(ObsolescenceStatus.fromString(v.getStatus().name()));
                if (v.getCyberSupportEndDate() != null) supportEnd = v.getCyberSupportEndDate().format(FR_DATE);
                else if (v.getInitialSupportEndDate() != null) supportEnd = v.getInitialSupportEndDate().format(FR_DATE);
                target = v.getVersion();
            }
        }
        if (c.forcedStatus() != null) {
            ObsolescenceStatus forced = ObsolescenceStatus.valueOf(c.forcedStatus());
            // Pour un constat, le statut calcule fait foi ; sinon on retient le plus grave.
            if ("constat".equals(c.role()) || SEVERITY.indexOf(forced) > SEVERITY.indexOf(status)) {
                status = forced;
            }
        }

        return ServerSoftwareObsolescence.builder()
                .environment(environment)
                .instance(c.role())
                .serverName(serverName)
                .status(status)
                .softwareName(c.softwareName())
                .currentVersion(truncate(c.version(), 100))
                .supportEndDate(truncate(supportEnd, 20))
                .osActualTargetVersion(truncate(target, 100))
                .source(SOURCE)
                .installPath(c.paths().isEmpty() ? null : String.join("\n", c.paths()))
                .details(c.details())
                .collectedAt(result.collectedAt())
                .build();
    }

    /**
     * Le nom NetBIOS est tronque a 15 caracteres (GUADELOUPE-GED-, GED-PEI-MARTINI) :
     * on retrouve le nom complet parmi les serveurs deja connus (Prevobs ou inventaire).
     */
    private String resolveServerName(String hostname) {
        Set<String> known = new LinkedHashSet<>(repository.findDistinctServerNames());
        serverRepository.findAll().stream().map(Server::getHostname).filter(Objects::nonNull).forEach(known::add);

        // Un nom plus long qui commence par le nom NetBIOS l'emporte (GED-PEI-MARTINI -> GED-PEI-MARTINIQUE).
        List<String> longer = known.stream()
                .filter(k -> k.length() > hostname.length() && k.toUpperCase().startsWith(hostname.toUpperCase()))
                .toList();
        if (longer.size() == 1) return longer.get(0);
        return known.stream().filter(k -> k.equalsIgnoreCase(hostname)).findFirst().orElse(hostname);
    }

    private String resolveEnvironment(String serverName) {
        Optional<String> fromPrevobs = repository.findByServerNameIgnoreCase(serverName).stream()
                .map(ServerSoftwareObsolescence::getEnvironment)
                .filter(Objects::nonNull)
                .findFirst();
        if (fromPrevobs.isPresent()) return fromPrevobs.get();
        return serverRepository.findAll().stream()
                .filter(s -> serverName.equalsIgnoreCase(s.getHostname()) && s.getEnvironment() != null)
                .map(s -> s.getEnvironment().getCode())
                .findFirst()
                .orElse(null);
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
