package com.edf.gedpei.service;

import com.edf.gedpei.dto.ServerCreateDTO;
import com.edf.gedpei.entity.*;
import com.edf.gedpei.repository.*;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service d'import de fichiers CSV/Excel.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImportService {

    private final ServerRepository serverRepository;
    private final EnvironmentRepository environmentRepository;
    private final SiteRepository siteRepository;
    private final ImportHistoryRepository importHistoryRepository;

    // Patterns pour detecter les colonnes - Serveurs
    private static final Map<String, List<String>> COLUMN_PATTERNS = Map.ofEntries(
            Map.entry("ENV", List.of("env", "environment", "environnement")),
            Map.entry("REF_DAT", List.of("ref", "dat", "reference")),
            Map.entry("RESOURCE_NAME", List.of("resource", "ressource", "name", "nom")),
            Map.entry("HOSTNAME", List.of("hostname", "host")),
            Map.entry("IP_FRONT", List.of("ip front", "ip_front", "ipfront")),
            Map.entry("IP_ADMIN", List.of("ip admin", "ip_admin", "ipadmin")),
            Map.entry("IP_ILO", List.of("ilo", "ip ilo", "ip_ilo", "carte ilo")),
            Map.entry("SITE_PEI", List.of("site pei", "site", "sitepei")),
            Map.entry("DATACENTER", List.of("datacenter", "data center", "dc")),
            Map.entry("MAJ_DATE", List.of("maj", "update", "mise a jour", "date"))
    );

    // Patterns pour detecter les colonnes - DBaaS
    private static final Map<String, List<String>> DBAAS_COLUMN_PATTERNS = Map.ofEntries(
            Map.entry("REF_DAT", List.of("ref", "dat", "reference")),
            Map.entry("DB_INSTANCE", List.of("instance")),
            Map.entry("DB_CHARSET", List.of("charset", "character")),
            Map.entry("DB_GABARIT", List.of("gabarit", "cpu", "sga")),
            Map.entry("DB_VERSION", List.of("version")),
            Map.entry("DB_BACKUP", List.of("backup", "sauvegarde"))
    );

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy")
    );

    /**
     * Importe un fichier CSV ou Excel.
     */
    @Transactional
    public ImportHistory importFile(MultipartFile file, String importedBy) {
        long startTime = System.currentTimeMillis();

        ImportHistory history = ImportHistory.builder()
                .filename(file.getOriginalFilename())
                .fileSize(file.getSize())
                .importedBy(importedBy)
                .status(ImportHistory.ImportStatus.IN_PROGRESS)
                .build();
        history = importHistoryRepository.save(history);

        try {
            String filename = file.getOriginalFilename();
            List<Map<String, String>> records;

            if (filename != null && (filename.endsWith(".xlsx") || filename.endsWith(".xls"))) {
                history.setFileType("XLSX");
                records = parseExcel(file.getInputStream());
            } else {
                history.setFileType("CSV");
                records = parseCsv(file.getInputStream());
            }

            history.setTotalRows(records.size());

            // Importer les enregistrements
            ImportResult result = importRecords(records, importedBy);

            history.setImportedRows(result.imported);
            history.setSkippedRows(result.skipped);
            history.setErrorRows(result.errors);

            if (result.errors > 0) {
                history.setStatus(ImportHistory.ImportStatus.COMPLETED_WITH_ERRORS);
                history.setErrorMessage(String.join("\n", result.errorMessages.subList(0,
                        Math.min(10, result.errorMessages.size()))));
            } else {
                history.setStatus(ImportHistory.ImportStatus.COMPLETED);
            }

            log.info("Import termine: {} lignes importees, {} ignorees, {} erreurs",
                    result.imported, result.skipped, result.errors);

        } catch (Exception e) {
            log.error("Erreur lors de l'import", e);
            history.setStatus(ImportHistory.ImportStatus.FAILED);
            history.setErrorMessage(e.getMessage());
        }

        history.setDurationMs(System.currentTimeMillis() - startTime);
        return importHistoryRepository.save(history);
    }

    /**
     * Parse un fichier CSV.
     */
    private List<Map<String, String>> parseCsv(InputStream inputStream) throws IOException {
        List<Map<String, String>> records = new ArrayList<>();

        // Detecter le delimiteur
        BufferedReader br = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        List<String> lines = br.lines().toList();

        if (lines.isEmpty()) {
            return records;
        }

        char delimiter = detectDelimiter(lines.get(0));
        String[] headers = null;
        Map<String, Integer> columnMapping = new HashMap<>();

        for (String line : lines) {
            // Ignorer les lignes vides
            if (line == null || line.isBlank()) {
                continue;
            }

            String[] values = parseCsvLine(line, delimiter);

            // Chercher la ligne d'entete
            if (headers == null) {
                if (isHeaderLine(values)) {
                    headers = values;
                    columnMapping = detectColumns(headers);
                    log.debug("En-tetes detectes: {}", Arrays.toString(headers));
                    continue;
                }
                continue;
            }

            // Parser les donnees
            Map<String, String> record = extractRecord(values, columnMapping);
            if (!record.isEmpty() && hasValidData(record)) {
                records.add(record);
            }
        }

        return records;
    }

    /**
     * Parse un fichier Excel.
     */
    private List<Map<String, String>> parseExcel(InputStream inputStream) throws IOException {
        List<Map<String, String>> records = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            String[] headers = null;
            Map<String, Integer> columnMapping = new HashMap<>();

            for (Row row : sheet) {
                String[] values = extractRowValues(row);

                // Ignorer les lignes vides
                if (values == null || Arrays.stream(values).allMatch(v -> v == null || v.isBlank())) {
                    continue;
                }

                // Chercher la ligne d'entete
                if (headers == null) {
                    if (isHeaderLine(values)) {
                        headers = values;
                        columnMapping = detectColumns(headers);
                        log.debug("En-tetes detectes: {}", Arrays.toString(headers));
                        continue;
                    }
                    continue;
                }

                // Parser les donnees
                Map<String, String> record = extractRecord(values, columnMapping);
                if (!record.isEmpty() && hasValidData(record)) {
                    records.add(record);
                }
            }
        }

        return records;
    }

    /**
     * Extrait les valeurs d'une ligne Excel.
     */
    private String[] extractRowValues(Row row) {
        if (row == null) return null;

        int lastCell = row.getLastCellNum();
        if (lastCell < 0) return null;

        String[] values = new String[lastCell];
        DataFormatter formatter = new DataFormatter();

        for (int i = 0; i < lastCell; i++) {
            Cell cell = row.getCell(i);
            values[i] = cell != null ? formatter.formatCellValue(cell).trim() : "";
        }

        return values;
    }

    /**
     * Detecte le delimiteur CSV.
     */
    private char detectDelimiter(String line) {
        int semicolons = line.length() - line.replace(";", "").length();
        int commas = line.length() - line.replace(",", "").length();
        int tabs = line.length() - line.replace("\t", "").length();

        if (semicolons > commas && semicolons > tabs) return ';';
        if (tabs > commas) return '\t';
        return ',';
    }

    /**
     * Parse une ligne CSV.
     */
    private String[] parseCsvLine(String line, char delimiter) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == delimiter && !inQuotes) {
                values.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        values.add(current.toString().trim());

        return values.toArray(new String[0]);
    }

    /**
     * Verifie si une ligne est une ligne d'entete pour serveurs.
     */
    private boolean isHeaderLine(String[] values) {
        if (values == null || values.length < 3) return false;

        String combined = String.join(" ", values).toLowerCase();
        return combined.contains("hostname") ||
               combined.contains("ip front") ||
               combined.contains("resource") ||
               combined.contains("environnement") ||
               (combined.contains("env") && combined.contains("name"));
    }

    /**
     * Verifie si une ligne est une ligne d'entete pour DBaaS.
     */
    private boolean isDbaaHeaderLine(String[] values) {
        if (values == null || values.length < 3) return false;

        String combined = String.join(" ", values).toLowerCase();
        return combined.contains("instance") &&
               (combined.contains("version") || combined.contains("gabarit") || combined.contains("backup"));
    }

    /**
     * Verifie si c'est une section DBaaS (ligne "info DBaaS").
     */
    private boolean isDbaasSectionMarker(String[] values) {
        if (values == null || values.length < 1) return false;
        String first = values[0].toLowerCase().trim();
        return first.contains("dbaas") || first.contains("info dbaas");
    }

    /**
     * Detecte automatiquement les colonnes.
     */
    private Map<String, Integer> detectColumns(String[] headers) {
        Map<String, Integer> mapping = new HashMap<>();

        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].toLowerCase().trim();

            // Supprimer les caracteres speciaux
            header = header.replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ");

            for (Map.Entry<String, List<String>> entry : COLUMN_PATTERNS.entrySet()) {
                for (String pattern : entry.getValue()) {
                    if (header.contains(pattern) && !mapping.containsKey(entry.getKey())) {
                        mapping.put(entry.getKey(), i);
                        break;
                    }
                }
            }
        }

        log.debug("Mapping colonnes: {}", mapping);
        return mapping;
    }

    /**
     * Extrait un enregistrement depuis les valeurs.
     */
    private Map<String, String> extractRecord(String[] values, Map<String, Integer> columnMapping) {
        Map<String, String> record = new HashMap<>();

        for (Map.Entry<String, Integer> entry : columnMapping.entrySet()) {
            int index = entry.getValue();
            if (index < values.length) {
                String value = values[index];
                if (value != null && !value.isBlank()) {
                    // Nettoyer les caracteres speciaux
                    value = cleanValue(value);
                    record.put(entry.getKey(), value);
                }
            }
        }

        return record;
    }

    /**
     * Nettoie une valeur.
     */
    private String cleanValue(String value) {
        if (value == null) return null;

        // Supprimer BOM et caracteres invisibles
        value = value.replaceAll("[\uFEFF\u00A0]", "");

        // Supprimer les sauts de ligne
        value = value.replaceAll("[\r\n]+", " ");

        // Nettoyer les espaces multiples
        value = value.replaceAll("\\s+", " ").trim();

        return value;
    }

    /**
     * Verifie si un enregistrement contient des donnees valides.
     */
    private boolean hasValidData(Map<String, String> record) {
        return record.containsKey("RESOURCE_NAME") ||
               record.containsKey("HOSTNAME") ||
               record.containsKey("IP_FRONT");
    }

    /**
     * Importe les enregistrements dans la base.
     */
    private ImportResult importRecords(List<Map<String, String>> records, String importedBy) {
        ImportResult result = new ImportResult();

        for (Map<String, String> record : records) {
            try {
                // Creer ou mettre a jour le serveur
                Server server = createOrUpdateServer(record, importedBy);

                if (server != null) {
                    result.imported++;
                } else {
                    result.skipped++;
                }

            } catch (Exception e) {
                result.errors++;
                result.errorMessages.add("Ligne " + (result.imported + result.skipped + result.errors) +
                        ": " + e.getMessage());
                log.warn("Erreur import ligne: {}", e.getMessage());
            }
        }

        return result;
    }

    /**
     * Cree ou met a jour un serveur depuis un enregistrement.
     */
    private Server createOrUpdateServer(Map<String, String> record, String importedBy) {
        String resourceName = record.getOrDefault("RESOURCE_NAME", "");
        String hostname = record.getOrDefault("HOSTNAME", "");
        String ipFront = record.getOrDefault("IP_FRONT", "");

        // Ignorer si pas assez d'info
        if (resourceName.isBlank() && hostname.isBlank() && ipFront.isBlank()) {
            return null;
        }

        // Chercher serveur existant
        Optional<Server> existing = serverRepository.findByUniqueKey(hostname, resourceName, ipFront);

        Server server = existing.orElse(new Server());
        boolean isNew = server.getId() == null;

        // Mettre a jour les champs
        if (!resourceName.isBlank()) server.setResourceName(resourceName);
        if (!hostname.isBlank()) server.setHostname(hostname);
        if (!ipFront.isBlank()) server.setIpFront(ipFront);

        server.setIpAdmin(record.getOrDefault("IP_ADMIN", server.getIpAdmin()));
        server.setIpIlo(record.getOrDefault("IP_ILO", server.getIpIlo()));
        server.setRefDat(record.getOrDefault("REF_DAT", server.getRefDat()));
        server.setDataCenter(record.getOrDefault("DATACENTER", server.getDataCenter()));

        // Date MAJ
        String majDate = record.get("MAJ_DATE");
        if (majDate != null && !majDate.isBlank()) {
            LocalDate date = parseDate(majDate);
            if (date != null) {
                server.setLastAdminUpdate(date);
            }
        }

        // Environnement
        String envName = record.get("ENV");
        if (envName != null && !envName.isBlank()) {
            String normalizedEnv = Environment.normalizeCode(envName);
            Environment env = environmentRepository.findByCodeIgnoreCase(normalizedEnv)
                    .orElseGet(() -> environmentRepository.save(new Environment(normalizedEnv)));
            server.setEnvironment(env);
        }

        // Site
        String sitePei = record.get("SITE_PEI");
        String dataCenter = record.get("DATACENTER");
        String siteCode = Site.extractSiteCode(sitePei, dataCenter);

        if (!siteCode.equals("UNKNOWN")) {
            Site site = siteRepository.findByCodeIgnoreCase(siteCode)
                    .orElseGet(() -> {
                        Site newSite = new Site(siteCode);
                        double[] coords = Site.getCoordinates(siteCode);
                        newSite.setLatitude(coords[0]);
                        newSite.setLongitude(coords[1]);
                        return siteRepository.save(newSite);
                    });
            server.setSite(site);
        }

        // Type (auto-detect)
        if (server.getServerType() == null) {
            server.setServerType(ServerType.detectFromResourceName(
                    resourceName, envName));
        }

        // Metadonnees
        if (isNew) {
            server.setCreatedBy(importedBy);
            server.setActive(true);
        }
        server.setUpdatedBy(importedBy);

        return serverRepository.save(server);
    }

    /**
     * Parse une date depuis differents formats.
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;

        dateStr = dateStr.trim();

        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        log.warn("Impossible de parser la date: {}", dateStr);
        return null;
    }

    /**
     * Resultat d'import.
     */
    private static class ImportResult {
        int imported = 0;
        int skipped = 0;
        int errors = 0;
        List<String> errorMessages = new ArrayList<>();
    }
}
