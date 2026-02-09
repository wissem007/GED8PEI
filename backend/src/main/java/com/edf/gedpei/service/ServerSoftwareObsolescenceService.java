package com.edf.gedpei.service;

import com.edf.gedpei.dto.ServerSoftwareObsolescenceDTO;
import com.edf.gedpei.entity.ServerSoftwareObsolescence;
import com.edf.gedpei.entity.ServerSoftwareObsolescence.ObsolescenceStatus;
import com.edf.gedpei.repository.ServerSoftwareObsolescenceRepository;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service pour la gestion de l'obsolescence logicielle par serveur.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServerSoftwareObsolescenceService {

    private final ServerSoftwareObsolescenceRepository repository;

    /**
     * Recupere toutes les donnees d'obsolescence.
     */
    public List<ServerSoftwareObsolescenceDTO> getAll() {
        return repository.findAll().stream()
                .map(ServerSoftwareObsolescenceDTO::fromEntity)
                .toList();
    }

    /**
     * Filtre les donnees selon les criteres.
     */
    public List<ServerSoftwareObsolescenceDTO> filter(String environment, String serverName,
                                                       String status, String softwareName) {
        String statusValue = null;
        if (status != null && !status.isBlank()) {
            try {
                // Convert to enum name for database lookup
                ObsolescenceStatus obsStatus = ObsolescenceStatus.valueOf(status.toUpperCase());
                statusValue = obsStatus.name();
            } catch (IllegalArgumentException e) {
                ObsolescenceStatus obsStatus = ObsolescenceStatus.fromString(status);
                statusValue = obsStatus.name();
            }
        }

        return repository.filter(
                environment != null && !environment.isBlank() ? environment : null,
                serverName != null && !serverName.isBlank() ? serverName : null,
                statusValue,
                softwareName != null && !softwareName.isBlank() ? softwareName : null
        ).stream()
                .map(ServerSoftwareObsolescenceDTO::fromEntity)
                .toList();
    }

    /**
     * Recupere les environnements distincts.
     */
    public List<String> getDistinctEnvironments() {
        return repository.findDistinctEnvironments();
    }

    /**
     * Recupere les noms de serveurs distincts.
     */
    public List<String> getDistinctServerNames() {
        return repository.findDistinctServerNames();
    }

    /**
     * Recupere les noms de logiciels distincts.
     */
    public List<String> getDistinctSoftwareNames() {
        return repository.findDistinctSoftwareNames();
    }

    /**
     * Retourne les statistiques par statut.
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // Comptage par statut
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ObsolescenceStatus status : ObsolescenceStatus.values()) {
            byStatus.put(status.getDisplayName(), 0L);
        }
        for (Object[] row : repository.countByStatus()) {
            String statusStr = (String) row[0];
            ObsolescenceStatus status = ObsolescenceStatus.valueOf(statusStr);
            Long count = ((Number) row[1]).longValue();
            byStatus.put(status.getDisplayName(), count);
        }
        stats.put("byStatus", byStatus);

        // Comptage par serveur et statut
        List<Object[]> serverStats = repository.countByServerAndStatus();
        Map<String, Map<String, Long>> byServer = new LinkedHashMap<>();
        for (Object[] row : serverStats) {
            String serverName = (String) row[0];
            String statusStr = (String) row[1];
            ObsolescenceStatus status = ObsolescenceStatus.valueOf(statusStr);
            Long count = ((Number) row[2]).longValue();

            byServer.computeIfAbsent(serverName, k -> new LinkedHashMap<>())
                    .put(status.getDisplayName(), count);
        }
        stats.put("byServer", byServer);

        // Total
        stats.put("total", repository.count());
        stats.put("serverCount", repository.findDistinctServerNames().size());

        return stats;
    }

    /**
     * Retourne les donnees groupees par serveur pour l'affichage TCD.
     */
    public Map<String, Object> getGroupedByServer() {
        List<ServerSoftwareObsolescence> all = repository.findAll();

        // Grouper par environnement > serveur > statut > logiciels
        Map<String, Map<String, Map<String, List<ServerSoftwareObsolescenceDTO>>>> grouped = new LinkedHashMap<>();

        for (ServerSoftwareObsolescence item : all) {
            String env = item.getEnvironment() != null ? item.getEnvironment() : "N/A";
            String server = item.getServerName();
            String status = item.getStatus().getDisplayName();

            grouped.computeIfAbsent(env, k -> new LinkedHashMap<>())
                    .computeIfAbsent(server, k -> new LinkedHashMap<>())
                    .computeIfAbsent(status, k -> new ArrayList<>())
                    .add(ServerSoftwareObsolescenceDTO.fromEntity(item));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", grouped);
        result.put("environments", repository.findDistinctEnvironments());
        result.put("servers", repository.findDistinctServerNames());

        return result;
    }

    /**
     * Supprime toutes les donnees.
     */
    @Transactional
    public long clearAll() {
        long count = repository.count();
        repository.deleteAll();
        log.info("Suppression de {} enregistrements d'obsolescence", count);
        return count;
    }

    /**
     * Recreer la table avec les bons types de colonnes.
     */
    @Transactional
    public void recreateTable(jakarta.persistence.EntityManager entityManager) {
        try {
            // Supprime et recree la table
            entityManager.createNativeQuery("DROP TABLE IF EXISTS server_software_obsolescence CASCADE").executeUpdate();
            log.info("Table server_software_obsolescence supprimee");

            // La table sera recree automatiquement par Hibernate au prochain acces
            entityManager.createNativeQuery(
                "CREATE TABLE server_software_obsolescence (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "environment VARCHAR(50), " +
                "instance VARCHAR(100), " +
                "server_name VARCHAR(100) NOT NULL, " +
                "status VARCHAR(50) NOT NULL, " +
                "software_name VARCHAR(255) NOT NULL, " +
                "current_version VARCHAR(100), " +
                "support_end_date VARCHAR(20), " +
                "os_target VARCHAR(100), " +
                "os_actual_target_version VARCHAR(100), " +
                "os_target_solution_version VARCHAR(100), " +
                "created_at TIMESTAMP, " +
                "updated_at TIMESTAMP)"
            ).executeUpdate();

            // Creer les index
            entityManager.createNativeQuery("CREATE INDEX idx_sso_server ON server_software_obsolescence(server_name)").executeUpdate();
            entityManager.createNativeQuery("CREATE INDEX idx_sso_environment ON server_software_obsolescence(environment)").executeUpdate();
            entityManager.createNativeQuery("CREATE INDEX idx_sso_status ON server_software_obsolescence(status)").executeUpdate();

            log.info("Table server_software_obsolescence recree avec les bons types");
        } catch (Exception e) {
            log.error("Erreur lors de la recreation de la table: {}", e.getMessage());
            throw new RuntimeException("Erreur recreation table: " + e.getMessage());
        }
    }

    /**
     * Importe les donnees depuis un fichier CSV PMT DISCOVR.
     */
    @Transactional
    public Map<String, Object> importFromCsv(MultipartFile file) {
        int imported = 0;
        int errors = 0;
        List<String> errorMessages = new ArrayList<>();

        // Detecte l'encodage
        Charset charset = detectCharset(file);
        log.info("Encodage detecte: {}", charset.name());

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), charset))) {

            // Detecte le delimiteur
            String firstLine = reader.readLine();
            char delimiter = detectDelimiter(firstLine);
            log.info("Delimiteur detecte: '{}'", delimiter);

            // Lit tout le fichier
            try (BufferedReader fullReader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), charset))) {

                CSVParser csvParser = new CSVParserBuilder()
                        .withSeparator(delimiter)
                        .build();

                CSVReader csvReader = new CSVReaderBuilder(fullReader)
                        .withCSVParser(csvParser)
                        .build();

                List<String[]> allRows = csvReader.readAll();

                // Parse le fichier hierarchique
                List<ServerSoftwareObsolescence> entries = parseHierarchicalCsv(allRows);

                // Sauvegarde en base
                for (ServerSoftwareObsolescence entry : entries) {
                    try {
                        repository.save(entry);
                        imported++;
                    } catch (Exception e) {
                        errors++;
                        errorMessages.add("Erreur sauvegarde: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'import CSV", e);
            errorMessages.add("Erreur globale: " + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("imported", imported);
        result.put("errors", errors);
        result.put("errorMessages", errorMessages);

        log.info("Import termine: {} importes, {} erreurs", imported, errors);
        return result;
    }

    /**
     * Parse le fichier CSV hierarchique PMT DISCOVR.
     */
    private List<ServerSoftwareObsolescence> parseHierarchicalCsv(List<String[]> rows) {
        List<ServerSoftwareObsolescence> entries = new ArrayList<>();

        String currentEnvironment = null;
        String currentInstance = null;
        String currentServer = null;
        ObsolescenceStatus currentStatus = null;

        int headerRowIndex = -1;

        // Trouve la ligne d'en-tete
        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row.length > 0) {
                String firstCol = row[0] != null ? row[0].trim().toUpperCase() : "";
                if (firstCol.contains("ENVIRONNEMENT") || firstCol.equals("ENVIRONNEMENT")) {
                    headerRowIndex = i;
                    break;
                }
            }
        }

        if (headerRowIndex == -1) {
            log.warn("En-tete non trouve, utilisation de la ligne 6 par defaut");
            headerRowIndex = 5; // Ligne 6 (0-indexed)
        }

        log.info("En-tete trouve a la ligne {}", headerRowIndex + 1);

        // Parse les donnees apres l'en-tete
        for (int i = headerRowIndex + 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row.length < 4) continue;

            String col0 = getCleanValue(row, 0); // ENVIRONNEMENT
            String col1 = getCleanValue(row, 1); // INSTANCE
            String col2 = getCleanValue(row, 2); // SERVEUR
            String col3 = getCleanValue(row, 3); // CSR STATUT VERSION PACKAGE
            String col4 = getCleanValue(row, 4); // INVENTIV NOM COMPOSANT
            String col5 = getCleanValue(row, 5); // CSR VERSION PACKAGE
            String col6 = getCleanValue(row, 6); // Date de Fin de Support
            String col7 = getCleanValue(row, 7); // OS CIBLE CSR VERSION PACKAGE
            String col8 = getCleanValue(row, 8); // OS ACTUEL SOLUTION CIBLE CSR VERSION PACKAGE
            String col9 = getCleanValue(row, 9); // OS CIBLE SOLUTION CIBLE CSR VERSION PACKAGE

            // Nouvelle ligne d'environnement
            if (col0 != null && !col0.isEmpty() &&
                (col0.equals("DEV") || col0.equals("PREPROD") || col0.equals("PROD") ||
                 col0.equals("RECETTE") || col0.equals("QUALIF"))) {
                currentEnvironment = col0;
                currentInstance = col1;
                log.debug("Nouvel environnement: {}", currentEnvironment);
                continue;
            }

            // Nouvelle ligne de serveur (col2 non vide, col4 vide = ligne de titre serveur)
            if (col2 != null && !col2.isEmpty() && (col4 == null || col4.isEmpty())) {
                currentServer = col2;
                // Le statut global du serveur est dans col3
                if (col3 != null && !col3.isEmpty()) {
                    currentStatus = ObsolescenceStatus.fromString(col3);
                }
                log.debug("Nouveau serveur: {} avec statut: {}", currentServer, currentStatus);
                continue;
            }

            // Ligne de statut seul (col3 non vide, col4 vide)
            if ((col3 != null && !col3.isEmpty()) && (col4 == null || col4.isEmpty())) {
                currentStatus = ObsolescenceStatus.fromString(col3);
                log.debug("Nouveau statut: {}", currentStatus);
                continue;
            }

            // Ligne de logiciel (col4 = nom du composant)
            if (col4 != null && !col4.isEmpty() && currentServer != null) {
                ServerSoftwareObsolescence entry = ServerSoftwareObsolescence.builder()
                        .environment(currentEnvironment)
                        .instance(currentInstance)
                        .serverName(currentServer)
                        .status(currentStatus != null ? currentStatus : ObsolescenceStatus.TOLEREE)
                        .softwareName(col4)
                        .currentVersion(col5)
                        .supportEndDate(col6)
                        .osTarget(col7)
                        .osActualTargetVersion(col8)
                        .osTargetSolutionVersion(col9)
                        .build();

                entries.add(entry);
                log.debug("Ajout: {} - {} - {} - {}", currentServer, col4, col5, currentStatus);
            }
        }

        log.info("Parse termine: {} entrees trouvees", entries.size());
        return entries;
    }

    private String getCleanValue(String[] row, int index) {
        if (index >= row.length) return null;
        String value = row[index];
        if (value == null) return null;
        value = value.trim();
        // Nettoie les caracteres speciaux d'encodage
        value = value.replace("�", "e").replace("", "");
        return value.isEmpty() ? null : value;
    }

    private Charset detectCharset(MultipartFile file) {
        try {
            byte[] bytes = file.getInputStream().readNBytes(1000);
            String content = new String(bytes, StandardCharsets.UTF_8);
            if (content.contains("\ufffd") || content.contains("�")) {
                return Charset.forName("windows-1252");
            }
            return StandardCharsets.UTF_8;
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }

    private char detectDelimiter(String line) {
        if (line == null) return ';';
        int semicolons = line.length() - line.replace(";", "").length();
        int commas = line.length() - line.replace(",", "").length();
        int tabs = line.length() - line.replace("\t", "").length();

        if (tabs > semicolons && tabs > commas) return '\t';
        if (commas > semicolons) return ',';
        return ';';
    }
}
