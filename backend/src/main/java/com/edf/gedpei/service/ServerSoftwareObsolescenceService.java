package com.edf.gedpei.service;

import com.edf.gedpei.dto.ServerSoftwareObsolescenceDTO;
import com.edf.gedpei.entity.ServerSoftwareObsolescence;
import com.edf.gedpei.entity.ServerSoftwareObsolescence.ObsolescenceStatus;
import com.edf.gedpei.repository.ServerSoftwareObsolescenceRepository;
import com.edf.gedpei.util.SpreadsheetReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service pour la gestion de l'obsolescence logicielle par serveur.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServerSoftwareObsolescenceService {

    private static final int HEADER_SEARCH_DEPTH = 30;

    private static final String COL_ENVIRONMENT = "ENVIRONNEMENT";
    private static final String COL_INSTANCE = "INSTANCE";
    private static final String COL_SERVER = "SERVEUR";
    private static final String COL_SERVICE_NAME = "NOM SERVICE";
    private static final String COL_STATUS = "CSR STATUT VERSION PACKAGE";
    private static final String COL_SOFTWARE = "INVENTIV NOM COMPOSANT";
    private static final String COL_VERSION = "CSR VERSION PACKAGE";
    private static final String COL_SUPPORT_END = "DATE FIN SUPPORT";
    private static final String COL_SUPPORT_END_ALT = "DATE DE FIN DE SUPPORT";
    private static final String COL_OS_TARGET = "OS CIBLE CSR VERSION PACKAGE";
    private static final String COL_OS_ACTUAL_SOLUTION = "OS ACTUEL SOLUTION CIBLE CSR VERSION PACKAGE";
    private static final String COL_OS_TARGET_SOLUTION = "OS CIBLE SOLUTION CIBLE CSR VERSION PACKAGE";

    private final ServerSoftwareObsolescenceRepository repository;

    /** Service retenu dans l'extraction Prevobs, qui couvre tout le SI. */
    @Value("${gedpei.import.service-filter:GED-PEI}")
    private String serviceFilter;

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
        List<ServerSoftwareObsolescence> entries = new ArrayList<>();

        try {
            List<SpreadsheetReader.Tab> tabs = SpreadsheetReader.read(file);
            String usedTab = null;

            // Un classeur Excel contient plusieurs onglets (DONNEES + TCD divers) :
            // on retient celui qui produit le plus d'entrees.
            for (SpreadsheetReader.Tab tab : tabs) {
                List<ServerSoftwareObsolescence> parsed = parseTab(tab.rows());
                if (parsed.size() > entries.size()) {
                    entries = parsed;
                    usedTab = tab.name();
                }
            }

            if (entries.isEmpty()) {
                errorMessages.add("Aucune donnee d'obsolescence reconnue. Attendu : un onglet a plat "
                        + "(colonnes SERVEUR et INVENTIV NOM COMPOSANT) ou un TCD hierarchique "
                        + "(en-tete ENVIRONNEMENT / INSTANCE / SERVEUR).");
                return buildResult(imported, errors, errorMessages);
            }

            log.info("Onglet retenu: '{}' -> {} entrees", usedTab, entries.size());

            for (ServerSoftwareObsolescence entry : entries) {
                try {
                    repository.save(entry);
                    imported++;
                } catch (Exception e) {
                    errors++;
                    errorMessages.add("Erreur sauvegarde: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'import", e);
            errors++;
            errorMessages.add("Erreur globale: " + e.getMessage());
        }

        log.info("Import termine: {} importes, {} erreurs", imported, errors);
        return buildResult(imported, errors, errorMessages);
    }

    private Map<String, Object> buildResult(int imported, int errors, List<String> errorMessages) {
        Map<String, Object> result = new HashMap<>();
        result.put("imported", imported);
        result.put("errors", errors);
        result.put("errorMessages", errorMessages);
        return result;
    }

    /**
     * Reconnait la forme de l'onglet et delegue au parseur adapte.
     *
     * <p>Deux formes circulent : l'extraction Prevobs a plat (onglet DONNEES, une ligne par
     * couple serveur/composant) et le TCD hierarchique prepare sous Excel. Le TCD se reconnait
     * a ses colonnes ENVIRONNEMENT / INSTANCE / SERVEUR en tete de ligne.</p>
     */
    private List<ServerSoftwareObsolescence> parseTab(List<String[]> rows) {
        for (int i = 0; i < Math.min(rows.size(), HEADER_SEARCH_DEPTH); i++) {
            Map<String, Integer> headers = indexHeaders(rows.get(i));
            if (headers.isEmpty()) continue;

            Integer environment = headers.get(COL_ENVIRONMENT);
            Integer server = headers.get(COL_SERVER);
            boolean hierarchical = environment != null && environment == 0
                    && server != null && server == 2;

            if (hierarchical) {
                return parseHierarchicalCsv(rows);
            }
            if (headers.containsKey(COL_SOFTWARE) && server != null) {
                return parseFlatRows(rows, i, headers);
            }
        }
        return new ArrayList<>();
    }

    /**
     * Parse l'extraction Prevobs a plat (onglet DONNEES) : une ligne par couple
     * serveur/composant, filtree sur le service GED-PEI.
     */
    private List<ServerSoftwareObsolescence> parseFlatRows(List<String[]> rows, int headerRowIndex,
                                                           Map<String, Integer> headers) {
        List<ServerSoftwareObsolescence> entries = new ArrayList<>();

        int softwareIndex = headers.get(COL_SOFTWARE);
        int serverIndex = headers.get(COL_SERVER);
        Integer serviceIndex = headers.get(COL_SERVICE_NAME);
        Integer environmentIndex = headers.get(COL_ENVIRONMENT);
        Integer instanceIndex = headers.get(COL_INSTANCE);
        Integer statusIndex = headers.get(COL_STATUS);
        Integer versionIndex = headers.get(COL_VERSION);
        Integer supportEndIndex = firstPresent(headers, COL_SUPPORT_END, COL_SUPPORT_END_ALT);
        Integer osTargetIndex = headers.get(COL_OS_TARGET);
        Integer osActualSolutionIndex = headers.get(COL_OS_ACTUAL_SOLUTION);
        Integer osTargetSolutionIndex = headers.get(COL_OS_TARGET_SOLUTION);

        String filter = SpreadsheetReader.normalizeHeader(serviceFilter);
        int filtered = 0;

        for (int i = headerRowIndex + 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (SpreadsheetReader.isBlank(row)) continue;

            // L'extraction Prevobs couvre tout le SI : on ne garde que le service GED-PEI.
            if (serviceIndex != null && !filter.isEmpty()) {
                String service = SpreadsheetReader.normalizeHeader(
                        SpreadsheetReader.cell(row, serviceIndex));
                if (!service.contains(filter)) {
                    filtered++;
                    continue;
                }
            }

            String software = SpreadsheetReader.cell(row, softwareIndex);
            String server = SpreadsheetReader.cell(row, serverIndex);
            if (software == null || server == null) continue;

            String status = value(row, statusIndex);
            entries.add(ServerSoftwareObsolescence.builder()
                    .environment(value(row, environmentIndex))
                    .instance(value(row, instanceIndex))
                    .serverName(server)
                    .status(status != null ? ObsolescenceStatus.fromString(status) : ObsolescenceStatus.TOLEREE)
                    .softwareName(software)
                    .currentVersion(value(row, versionIndex))
                    .supportEndDate(value(row, supportEndIndex))
                    .osTarget(value(row, osTargetIndex))
                    .osActualTargetVersion(value(row, osActualSolutionIndex))
                    .osTargetSolutionVersion(value(row, osTargetSolutionIndex))
                    .build());
        }

        log.info("Format a plat: {} entrees retenues, {} lignes hors service '{}'",
                entries.size(), filtered, serviceFilter);
        return entries;
    }

    /**
     * Indexe les en-tetes d'une ligne (normalises) vers leur position.
     */
    private Map<String, Integer> indexHeaders(String[] row) {
        Map<String, Integer> headers = new HashMap<>();
        if (row == null) return headers;

        for (int i = 0; i < row.length; i++) {
            String header = SpreadsheetReader.normalizeHeader(row[i]);
            if (!header.isEmpty()) {
                headers.putIfAbsent(header, i);
            }
        }
        return headers;
    }

    private Integer firstPresent(Map<String, Integer> headers, String... names) {
        for (String name : names) {
            Integer index = headers.get(name);
            if (index != null) return index;
        }
        return null;
    }

    private String value(String[] row, Integer index) {
        return index == null ? null : SpreadsheetReader.cell(row, index);
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
}
