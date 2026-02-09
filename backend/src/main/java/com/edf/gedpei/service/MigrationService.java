package com.edf.gedpei.service;

import com.edf.gedpei.dto.MigrationCreateDTO;
import com.edf.gedpei.dto.MigrationDTO;
import com.edf.gedpei.entity.Migration;
import com.edf.gedpei.entity.Migration.MigrationStatus;
import com.edf.gedpei.repository.MigrationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service pour la gestion des migrations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MigrationService {

    private final MigrationRepository migrationRepository;

    /**
     * Recupere toutes les migrations avec pagination.
     */
    public Page<MigrationDTO> getAllMigrations(Pageable pageable) {
        return migrationRepository.findAll(pageable)
                .map(MigrationDTO::fromEntity);
    }

    /**
     * Recupere une migration par son ID.
     */
    public MigrationDTO getMigrationById(Long id) {
        Migration migration = migrationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Migration non trouvee: " + id));
        return MigrationDTO.fromEntity(migration);
    }

    /**
     * Recherche des migrations.
     */
    public Page<MigrationDTO> searchMigrations(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return getAllMigrations(pageable);
        }
        return migrationRepository.search(query.trim(), pageable)
                .map(MigrationDTO::fromEntity);
    }

    /**
     * Recupere les migrations actives (planifiees, en cours, en attente).
     */
    public List<MigrationDTO> getActiveMigrations() {
        return migrationRepository.findActiveMigrations().stream()
                .map(MigrationDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Recupere les migrations par statut.
     */
    public List<MigrationDTO> getMigrationsByStatus(MigrationStatus status) {
        return migrationRepository.findByStatus(status).stream()
                .map(MigrationDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Cree une nouvelle migration.
     */
    @Transactional
    public MigrationDTO createMigration(MigrationCreateDTO dto, String createdBy) {
        Migration migration = Migration.builder()
                .phase(dto.getPhase())
                .description(dto.getDescription())
                .environmentName(dto.getEnvironmentName())
                .dateDebut(dto.getDateDebut())
                .dateFin(dto.getDateFin())
                .responsable(dto.getResponsable())
                .status(dto.getStatus())
                .risques(dto.getRisques())
                .commentaires(dto.getCommentaires())
                .createdBy(createdBy)
                .build();

        migration = migrationRepository.save(migration);
        log.info("Migration creee: {} par {}", migration.getDescription(), createdBy);

        return MigrationDTO.fromEntity(migration);
    }

    /**
     * Met a jour une migration.
     */
    @Transactional
    public MigrationDTO updateMigration(Long id, MigrationCreateDTO dto) {
        Migration migration = migrationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Migration non trouvee: " + id));

        migration.setPhase(dto.getPhase());
        migration.setDescription(dto.getDescription());
        migration.setEnvironmentName(dto.getEnvironmentName());
        migration.setDateDebut(dto.getDateDebut());
        migration.setDateFin(dto.getDateFin());
        migration.setResponsable(dto.getResponsable());
        migration.setStatus(dto.getStatus());
        migration.setRisques(dto.getRisques());
        migration.setCommentaires(dto.getCommentaires());

        migration = migrationRepository.save(migration);
        log.info("Migration mise a jour: {}", migration.getDescription());

        return MigrationDTO.fromEntity(migration);
    }

    /**
     * Supprime une migration.
     */
    @Transactional
    public void deleteMigration(Long id) {
        Migration migration = migrationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Migration non trouvee: " + id));

        log.info("Migration supprimee: {}", migration.getDescription());
        migrationRepository.delete(migration);
    }

    /**
     * Liste toutes les phases distinctes.
     */
    public List<String> getAllPhases() {
        return migrationRepository.findAllPhases();
    }

    /**
     * Liste tous les responsables distincts.
     */
    public List<String> getAllResponsables() {
        return migrationRepository.findAllResponsables();
    }

    /**
     * Liste tous les statuts possibles.
     */
    public List<Map<String, String>> getAllStatuses() {
        return Arrays.stream(MigrationStatus.values())
                .map(s -> Map.of(
                        "value", s.name(),
                        "label", s.getDisplayName(),
                        "color", s.getColor()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Statistiques des migrations.
     */
    public Map<String, Long> getStats() {
        return Arrays.stream(MigrationStatus.values())
                .collect(Collectors.toMap(
                        MigrationStatus::name,
                        migrationRepository::countByStatus
                ));
    }

    /**
     * Importe des migrations depuis un fichier CSV.
     * Detecte automatiquement si la migration existe deja (mise a jour) ou non (creation).
     */
    @Transactional
    public Map<String, Object> importFromCsv(MultipartFile file, String importedBy) {
        int created = 0;
        int updated = 0;
        int errors = 0;
        List<String> errorMessages = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            int lineNumber = 0;
            String[] headers = null;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Ignorer les lignes vides
                if (line.trim().isEmpty()) continue;

                // Ignorer le titre "PLANNING DES MIGRATIONS..."
                if (line.toUpperCase().contains("PLANNING DES MIGRATIONS")) continue;

                String[] values = parseCsvLine(line);

                // Detecter la ligne d'en-tete
                if (headers == null && isHeaderLine(values)) {
                    headers = values;
                    continue;
                }

                // Si pas d'en-tete detecte, utiliser l'en-tete par defaut
                if (headers == null) {
                    headers = new String[]{"Phase", "Description", "Environnement", "Date début",
                                           "Date fin", "Responsable", "Statut", "Risques/Commentaires"};
                }

                // Ignorer les lignes avec moins de 2 colonnes
                if (values.length < 2) continue;

                try {
                    Map<String, String> row = mapRowToHeaders(headers, values);

                    String phase = row.getOrDefault("Phase", "").trim();
                    String description = row.getOrDefault("Description", "").trim();
                    String env = row.getOrDefault("Environnement", "").trim();

                    // Ignorer les lignes sans phase ou description
                    if (phase.isEmpty() || description.isEmpty()) continue;

                    // Chercher si la migration existe deja
                    Optional<Migration> existing = migrationRepository
                            .findByPhaseAndDescriptionAndEnvironment(phase, description, env.isEmpty() ? null : env);

                    Migration migration;
                    boolean isUpdate = existing.isPresent();

                    if (isUpdate) {
                        migration = existing.get();
                    } else {
                        migration = new Migration();
                        migration.setCreatedBy(importedBy);
                    }

                    // Mettre a jour les champs
                    migration.setPhase(phase);
                    migration.setDescription(description);
                    migration.setEnvironmentName(env.isEmpty() ? null : env);
                    migration.setDateDebut(parseDate(row.getOrDefault("Date début", "")));
                    migration.setDateFin(parseDate(row.getOrDefault("Date fin", "")));
                    migration.setResponsable(row.getOrDefault("Responsable", "").trim());
                    migration.setStatus(parseStatus(row.getOrDefault("Statut", "")));

                    String risques = row.getOrDefault("Risques/Commentaires", "").trim();
                    if (risques.isEmpty()) {
                        risques = row.getOrDefault("Risques", "").trim();
                    }
                    if (risques.isEmpty()) {
                        risques = row.getOrDefault("Commentaires", "").trim();
                    }
                    migration.setRisques(risques);

                    migrationRepository.save(migration);

                    if (isUpdate) {
                        updated++;
                        log.debug("Migration mise a jour: {} - {}", phase, description);
                    } else {
                        created++;
                        log.debug("Migration creee: {} - {}", phase, description);
                    }

                } catch (Exception e) {
                    errors++;
                    errorMessages.add("Ligne " + lineNumber + ": " + e.getMessage());
                    log.warn("Erreur import ligne {}: {}", lineNumber, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Erreur lecture fichier CSV: {}", e.getMessage());
            throw new RuntimeException("Erreur lecture fichier: " + e.getMessage());
        }

        log.info("Import termine: {} creees, {} mises a jour, {} erreurs", created, updated, errors);

        Map<String, Object> result = new HashMap<>();
        result.put("created", created);
        result.put("updated", updated);
        result.put("errors", errors);
        result.put("errorMessages", errorMessages);
        result.put("total", created + updated);
        return result;
    }

    /**
     * Parse une ligne CSV (gere les points-virgules).
     */
    private String[] parseCsvLine(String line) {
        // Utiliser le point-virgule comme separateur
        return line.split(";", -1);
    }

    /**
     * Verifie si c'est une ligne d'en-tete.
     */
    private boolean isHeaderLine(String[] values) {
        if (values.length < 2) return false;
        String first = values[0].toLowerCase().trim();
        return first.equals("phase") || first.contains("phase");
    }

    /**
     * Mappe les valeurs aux en-tetes.
     */
    private Map<String, String> mapRowToHeaders(String[] headers, String[] values) {
        Map<String, String> row = new HashMap<>();
        for (int i = 0; i < headers.length && i < values.length; i++) {
            String header = headers[i].trim();
            String value = values[i].trim();
            row.put(header, value);

            // Alias pour les en-tetes
            if (header.equalsIgnoreCase("Date début") || header.equalsIgnoreCase("Date debut")) {
                row.put("Date début", value);
            }
            if (header.equalsIgnoreCase("Risques/Commentaires") || header.equalsIgnoreCase("Risques")) {
                row.put("Risques/Commentaires", value);
            }
        }
        return row;
    }

    /**
     * Parse une date depuis une chaine.
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        dateStr = dateStr.trim();

        // Valeurs speciales
        if (dateStr.equalsIgnoreCase("Continu") || dateStr.equalsIgnoreCase("Mensuel")) {
            return null; // Ces valeurs seront stockees dans les commentaires
        }

        // Essayer differents formats
        String[] patterns = {"dd/MM/yyyy", "yyyy-MM-dd", "d/M/yyyy", "dd-MM-yyyy"};
        for (String pattern : patterns) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern));
            } catch (DateTimeParseException ignored) {}
        }

        return null;
    }

    /**
     * Parse un statut depuis une chaine.
     */
    private MigrationStatus parseStatus(String statusStr) {
        if (statusStr == null || statusStr.trim().isEmpty()) {
            return MigrationStatus.PLANIFIE;
        }
        statusStr = statusStr.trim().toLowerCase();

        if (statusStr.contains("planifi")) return MigrationStatus.PLANIFIE;
        if (statusStr.contains("non") && statusStr.contains("démarr")) return MigrationStatus.NON_DEMARRE;
        if (statusStr.contains("non") && statusStr.contains("demarr")) return MigrationStatus.NON_DEMARRE;
        if (statusStr.contains("en cours")) return MigrationStatus.EN_COURS;
        if (statusStr.contains("termin")) return MigrationStatus.TERMINE;
        if (statusStr.contains("annul")) return MigrationStatus.ANNULE;
        if (statusStr.contains("attente")) return MigrationStatus.EN_ATTENTE;

        return MigrationStatus.PLANIFIE;
    }
}
