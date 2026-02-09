package com.edf.gedpei.service;

import com.edf.gedpei.dto.SoftwareVersionDTO;
import com.edf.gedpei.entity.SoftwareVersion;
import com.edf.gedpei.entity.SoftwareVersion.VersionStatus;
import com.edf.gedpei.repository.SoftwareVersionRepository;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
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

/**
 * Service pour la gestion des versions de logiciels.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SoftwareVersionService {

    private final SoftwareVersionRepository softwareVersionRepository;

    private static final List<DateTimeFormatter> DATE_FORMATTERS = Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    );

    /**
     * Recupere toutes les versions avec pagination.
     */
    public Page<SoftwareVersionDTO> getAllVersions(Pageable pageable) {
        return softwareVersionRepository.findAll(pageable)
                .map(SoftwareVersionDTO::fromEntity);
    }

    /**
     * Filtre les versions selon les criteres.
     */
    public Page<SoftwareVersionDTO> filterVersions(String softwareName, String version,
                                                    String status, Pageable pageable) {
        VersionStatus versionStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                versionStatus = VersionStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                versionStatus = VersionStatus.fromString(status);
            }
        }

        return softwareVersionRepository.filterVersions(
                softwareName, version, versionStatus, pageable)
                .map(SoftwareVersionDTO::fromEntity);
    }

    /**
     * Recupere une version par son ID.
     */
    public Optional<SoftwareVersionDTO> getVersionById(Long id) {
        return softwareVersionRepository.findById(id)
                .map(SoftwareVersionDTO::fromEntity);
    }

    /**
     * Recupere les versions d'un logiciel specifique.
     */
    public List<SoftwareVersionDTO> getVersionsBySoftware(String softwareName) {
        return softwareVersionRepository.findBySoftwareNameIgnoreCaseOrderByVersionDesc(softwareName)
                .stream()
                .map(SoftwareVersionDTO::fromEntity)
                .toList();
    }

    /**
     * Recupere la liste des noms de logiciels distincts.
     */
    public List<String> getDistinctSoftwareNames() {
        return softwareVersionRepository.findDistinctSoftwareNames();
    }

    /**
     * Cree ou met a jour une version.
     */
    @Transactional
    public SoftwareVersionDTO saveVersion(SoftwareVersionDTO dto) {
        SoftwareVersion entity = dto.toEntity();

        if (dto.getId() == null) {
            // Verifie si la combinaison existe deja
            Optional<SoftwareVersion> existing = softwareVersionRepository
                    .findBySoftwareNameIgnoreCaseAndVersionIgnoreCase(
                            dto.getSoftwareName(), dto.getVersion());
            if (existing.isPresent()) {
                entity = existing.get();
                updateEntityFromDto(entity, dto);
            }
        }

        SoftwareVersion saved = softwareVersionRepository.save(entity);
        return SoftwareVersionDTO.fromEntity(saved);
    }

    /**
     * Supprime une version.
     */
    @Transactional
    public void deleteVersion(Long id) {
        softwareVersionRepository.deleteById(id);
    }

    /**
     * Supprime toutes les versions.
     */
    @Transactional
    public long clearAll() {
        long count = softwareVersionRepository.count();
        softwareVersionRepository.deleteAll();
        log.info("Suppression de {} versions", count);
        return count;
    }

    /**
     * Met a jour le schema de la table (augmente la taille des colonnes).
     */
    @Transactional
    public void updateSchema(jakarta.persistence.EntityManager entityManager) {
        try {
            entityManager.createNativeQuery("ALTER TABLE software_versions ALTER COLUMN version TYPE TEXT").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE software_versions ALTER COLUMN software_name TYPE TEXT").executeUpdate();
            log.info("Schema mis a jour: colonnes version et software_name elargies");
        } catch (Exception e) {
            log.warn("Erreur mise a jour schema: {}", e.getMessage());
        }
    }

    /**
     * Importe les versions depuis un fichier CSV.
     */
    public Map<String, Object> importFromCsv(MultipartFile file) {
        int imported = 0;
        int updated = 0;
        int errors = 0;
        List<String> errorMessages = new ArrayList<>();

        // Detecte l'encodage (UTF-8 ou Windows-1252)
        java.nio.charset.Charset charset = detectCharset(file);
        log.info("Encodage detecte: {}", charset.name());

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), charset))) {

            // Detecte le delimiteur
            String firstLine = reader.readLine();
            char delimiter = detectDelimiter(firstLine);

            // Reconstruit le reader avec le fichier complet
            try (BufferedReader fullReader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), charset))) {

                CSVParser csvParser = new CSVParserBuilder()
                        .withSeparator(delimiter)
                        .build();

                CSVReader csvReader = new CSVReaderBuilder(fullReader)
                        .withCSVParser(csvParser)
                        .build();

                // Lecture des en-tetes
                String[] headers = csvReader.readNext();
                if (headers == null) {
                    errorMessages.add("Fichier CSV vide");
                    return createResult(imported, updated, errors, errorMessages);
                }

                Map<String, Integer> headerMap = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    headerMap.put(headers[i].trim(), i);
                }

                log.info("En-tetes CSV detectes: {}", headerMap.keySet());

                // Mappe les colonnes
                ColumnMapping mapping = detectColumnMapping(headerMap);

                String[] record;
                int lineNum = 1;
                while ((record = csvReader.readNext()) != null) {
                    lineNum++;
                    try {
                        SoftwareVersion version = parseRecord(record, mapping);
                        if (version != null && version.getSoftwareName() != null &&
                            !version.getSoftwareName().isBlank()) {

                            Optional<SoftwareVersion> existing = softwareVersionRepository
                                    .findBySoftwareNameIgnoreCaseAndVersionIgnoreCase(
                                            version.getSoftwareName(), version.getVersion());

                            if (existing.isPresent()) {
                                updateEntity(existing.get(), version);
                                softwareVersionRepository.save(existing.get());
                                updated++;
                            } else {
                                softwareVersionRepository.save(version);
                                imported++;
                            }
                        }
                    } catch (Exception e) {
                        errors++;
                        errorMessages.add("Ligne " + lineNum + ": " + e.getMessage());
                        log.warn("Erreur import ligne {}: {}", lineNum, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'import CSV", e);
            errorMessages.add("Erreur globale: " + e.getMessage());
        }

        return createResult(imported, updated, errors, errorMessages);
    }

    private Map<String, Object> createResult(int imported, int updated, int errors, List<String> errorMessages) {
        Map<String, Object> result = new HashMap<>();
        result.put("imported", imported);
        result.put("updated", updated);
        result.put("errors", errors);
        result.put("errorMessages", errorMessages);
        result.put("total", imported + updated);

        log.info("Import termine: {} importes, {} mis a jour, {} erreurs", imported, updated, errors);
        return result;
    }

    /**
     * Retourne les statistiques par statut.
     */
    public Map<String, Long> getStatsByStatus() {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (VersionStatus status : VersionStatus.values()) {
            stats.put(status.getDisplayName(), softwareVersionRepository.countByStatus(status));
        }
        return stats;
    }

    // === Methodes privees ===

    private java.nio.charset.Charset detectCharset(MultipartFile file) {
        try {
            byte[] bytes = file.getInputStream().readNBytes(1000);
            String content = new String(bytes, StandardCharsets.UTF_8);
            // Si on trouve des caracteres de remplacement, c'est probablement Windows-1252
            if (content.contains("\ufffd") || content.contains("�")) {
                return java.nio.charset.Charset.forName("windows-1252");
            }
            // Verifie si c'est du UTF-8 valide
            boolean hasUtf8Bom = bytes.length >= 3 &&
                (bytes[0] & 0xFF) == 0xEF &&
                (bytes[1] & 0xFF) == 0xBB &&
                (bytes[2] & 0xFF) == 0xBC;
            if (hasUtf8Bom) {
                return StandardCharsets.UTF_8;
            }
            // Cherche des sequences UTF-8 invalides
            for (int i = 0; i < bytes.length - 1; i++) {
                int b = bytes[i] & 0xFF;
                if (b >= 0x80 && b <= 0x9F) {
                    // Ces octets sont des caracteres de controle en UTF-8 mais valides en Windows-1252
                    return java.nio.charset.Charset.forName("windows-1252");
                }
            }
            return StandardCharsets.UTF_8;
        } catch (Exception e) {
            log.warn("Erreur detection encodage, utilisation UTF-8 par defaut", e);
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

    private ColumnMapping detectColumnMapping(Map<String, Integer> headerMap) {
        ColumnMapping mapping = new ColumnMapping();

        for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
            String header = entry.getKey().toLowerCase().trim()
                    .replace("é", "e").replace("è", "e").replace("ê", "e")
                    .replace("à", "a").replace("ô", "o").replace("î", "i");
            int index = entry.getValue();

            // Format Sipedia - CSR Detail des versions (prioritaire)
            if (header.contains("libelle") && header.contains("solution")) {
                mapping.softwareNameIndex = index;
            } else if (header.contains("version") && header.contains("roadmap") && !header.contains("statut")) {
                mapping.versionIndex = index;
            } else if (header.contains("statut") && header.contains("version") && header.contains("roadmap")) {
                mapping.statusIndex = index;
            } else if (header.equals("version package") || header.equals("versionpackage") || header.equals("package")) {
                // Match exact pour "Version package" - prioritaire
                mapping.versionPackageIndex = index;
            } else if (header.contains("version") && header.contains("package") && !header.contains("lien") && !header.contains("commentaire") && !header.contains("os") && !header.contains("identifiant") && mapping.versionPackageIndex == -1) {
                // Match partiel seulement si pas deja trouve et pas une colonne de liens/commentaires
                mapping.versionPackageIndex = index;
            } else if (header.contains("derniere") && header.contains("mise") && header.contains("jour")) {
                mapping.statusDateIndex = index;
            } else if (header.contains("fin") && header.contains("support") && header.contains("init")) {
                mapping.initialSupportIndex = index;
            } else if (header.contains("fin") && header.contains("support") && header.contains("etendu")) {
                mapping.extendedSupportIndex = index;
            } else if (header.contains("fin") && header.contains("support") && header.contains("cyb")) {
                mapping.cyberSupportIndex = index;
            }
            // Format Sipedia - Catalogue des solutions
            // Note: "Identifiant de la solution" ne doit PAS etre utilise pour la version
            // car dans le format CSR, c'est "Version Roadmap" qui contient la version
            else if (header.contains("politique") && header.contains("industrielle")) {
                if (mapping.statusIndex == -1) mapping.statusIndex = index;
            } else if (header.contains("fournisseur") || header.contains("editeur")) {
                mapping.vendorIndex = index;
            } else if (header.contains("descriptif") || header.contains("description")) {
                mapping.notesIndex = index;
            }
            // Format standard versions de logiciels
            else if (header.contains("logiciel") || header.equals("software") || header.equals("nom")) {
                if (mapping.softwareNameIndex == -1) mapping.softwareNameIndex = index;
            } else if (header.equals("version")) {
                if (mapping.versionIndex == -1) mapping.versionIndex = index;
            } else if (header.equals("statut") || header.equals("status")) {
                if (mapping.statusIndex == -1) mapping.statusIndex = index;
            } else if (header.contains("date") && header.contains("statut")) {
                if (mapping.statusDateIndex == -1) mapping.statusDateIndex = index;
            } else if (header.contains("notes") || header.contains("commentaire")) {
                if (mapping.notesIndex == -1) mapping.notesIndex = index;
            } else if (header.contains("liens") && header.contains("utiles")) {
                mapping.usefulLinksIndex = index;
            }
        }

        log.info("Mapping detecte: softwareName={}, version={}, versionPackage={}, status={}, statusDate={}, initialSupport={}, extendedSupport={}, cyberSupport={}",
                mapping.softwareNameIndex, mapping.versionIndex, mapping.versionPackageIndex, mapping.statusIndex,
                mapping.statusDateIndex, mapping.initialSupportIndex, mapping.extendedSupportIndex, mapping.cyberSupportIndex);

        return mapping;
    }

    private SoftwareVersion parseRecord(String[] record, ColumnMapping mapping) {
        SoftwareVersion version = new SoftwareVersion();

        version.setSoftwareName(getField(record, mapping.softwareNameIndex));

        // Utiliser UNIQUEMENT "Version Roadmap" pour le champ version
        String versionStr = getField(record, mapping.versionIndex);

        if (versionStr == null || versionStr.isBlank()) {
            versionStr = "N/A";
        }
        version.setVersion(versionStr);

        // Version Package - extraire la version du package (sans les URLs)
        String versionPackage = getField(record, mapping.versionPackageIndex);
        if (versionPackage != null && !versionPackage.isBlank()) {
            // Extraire la version package (partie avant les URLs)
            String cleanPackageVersion = extractPackageVersion(versionPackage);
            version.setPackageVersion(cleanPackageVersion);
        }

        String statusStr = getField(record, mapping.statusIndex);
        version.setStatus(VersionStatus.fromString(statusStr));

        version.setStatusUpdateDate(parseDate(getField(record, mapping.statusDateIndex)));
        version.setInitialSupportEndDate(parseDate(getField(record, mapping.initialSupportIndex)));
        version.setExtendedSupportEndDate(parseDate(getField(record, mapping.extendedSupportIndex)));
        version.setCyberSupportEndDate(parseDate(getField(record, mapping.cyberSupportIndex)));

        if (mapping.notesIndex >= 0) {
            version.setNotes(getField(record, mapping.notesIndex));
        }

        // Collecter tous les liens utiles
        List<String> allLinks = new ArrayList<>();

        // 1. Liens de la colonne "Liens utiles sur la version package"
        if (mapping.usefulLinksIndex >= 0) {
            String rawLinks = getField(record, mapping.usefulLinksIndex);
            if (rawLinks != null && !rawLinks.isBlank()) {
                allLinks.add(rawLinks);
            }
        }

        // 2. Extraire les URLs de "Version package" si present (colonne separee)
        if (versionPackage != null && versionPackage.contains("http")) {
            allLinks.add(versionPackage);
        }

        // Parser et fusionner tous les liens
        if (!allLinks.isEmpty()) {
            String combinedLinks = String.join(" , ", allLinks);
            version.setUsefulLinks(parseUsefulLinks(combinedLinks));
        }

        return version;
    }

    /**
     * Nettoie une chaine de version en retirant les URLs.
     * Retourne null si la chaine ne contient pas de vraie version (seulement des liens).
     */
    private String cleanVersionString(String versionStr) {
        if (versionStr == null || versionStr.isBlank()) {
            return null;
        }

        // Liste des prefixes qui indiquent que ce n'est PAS une version mais un lien
        String[] linkPrefixes = {
            "deposit", "deposit", "lien", "link", "installation", "migration",
            "document", "doc", "guide", "mise a jour", "mise à jour", "package"
        };

        String lowerStr = versionStr.toLowerCase().trim();

        // Verifier si ca commence par un prefixe de lien
        for (String prefix : linkPrefixes) {
            if (lowerStr.startsWith(prefix)) {
                // Ce n'est pas une version, c'est une description de lien
                return null;
            }
        }

        // Si la version contient une URL, extraire seulement la partie version
        if (versionStr.contains("(http")) {
            // Pattern: "1.7.0.141 (EL7) (https://...)" -> "1.7.0.141 (EL7)"
            int httpIndex = versionStr.indexOf("(http");
            if (httpIndex > 0) {
                versionStr = versionStr.substring(0, httpIndex).trim();
                // Retirer la virgule finale si presente
                if (versionStr.endsWith(",")) {
                    versionStr = versionStr.substring(0, versionStr.length() - 1).trim();
                }
            } else {
                // L'URL est au debut, pas de version
                return null;
            }
        }

        // Verifier que le resultat ressemble a une version (contient au moins un chiffre)
        if (!versionStr.matches(".*\\d.*")) {
            return null;
        }

        return versionStr;
    }

    private String getField(String[] record, int index) {
        if (index < 0 || index >= record.length) return null;
        String value = record[index];
        return (value != null && !value.isBlank()) ? value.trim() : null;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateStr.trim(), formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private void updateEntity(SoftwareVersion existing, SoftwareVersion newData) {
        existing.setStatus(newData.getStatus());
        if (newData.getPackageVersion() != null) {
            existing.setPackageVersion(newData.getPackageVersion());
        }
        if (newData.getStatusUpdateDate() != null) {
            existing.setStatusUpdateDate(newData.getStatusUpdateDate());
        }
        if (newData.getInitialSupportEndDate() != null) {
            existing.setInitialSupportEndDate(newData.getInitialSupportEndDate());
        }
        if (newData.getExtendedSupportEndDate() != null) {
            existing.setExtendedSupportEndDate(newData.getExtendedSupportEndDate());
        }
        if (newData.getCyberSupportEndDate() != null) {
            existing.setCyberSupportEndDate(newData.getCyberSupportEndDate());
        }
        if (newData.getNotes() != null) {
            existing.setNotes(newData.getNotes());
        }
        if (newData.getUsefulLinks() != null) {
            existing.setUsefulLinks(newData.getUsefulLinks());
        }
    }

    /**
     * Extrait la version package depuis une chaine qui peut contenir des URLs.
     * Ex: "8.0.112 (EL7) , Deposit (http://...)" -> "8.0.112 (EL7)"
     */
    private String extractPackageVersion(String versionPackage) {
        if (versionPackage == null || versionPackage.isBlank()) {
            return null;
        }

        // Si la chaine commence par un descripteur de lien, pas de version
        String lowerStr = versionPackage.toLowerCase().trim();
        if (lowerStr.startsWith("deposit") || lowerStr.startsWith("lien") ||
            lowerStr.startsWith("installation") || lowerStr.startsWith("migration") ||
            lowerStr.startsWith("document") || lowerStr.startsWith("guide")) {
            return null;
        }

        // Extraire la partie avant la premiere URL ou avant ", Deposit"
        String result = versionPackage;

        // Couper avant ", Deposit" ou ", Lien" etc.
        int commaIndex = result.indexOf(",");
        if (commaIndex > 0) {
            String afterComma = result.substring(commaIndex + 1).trim().toLowerCase();
            if (afterComma.startsWith("deposit") || afterComma.startsWith("lien") ||
                afterComma.startsWith("installation") || afterComma.startsWith("migration") ||
                afterComma.startsWith("(http")) {
                result = result.substring(0, commaIndex).trim();
            }
        }

        // Couper avant "(http"
        int httpIndex = result.indexOf("(http");
        if (httpIndex > 0) {
            result = result.substring(0, httpIndex).trim();
        }

        // Verifier que le resultat contient au moins un chiffre (vraie version)
        if (!result.matches(".*\\d.*")) {
            return null;
        }

        return result.isEmpty() ? null : result;
    }

    private void updateEntityFromDto(SoftwareVersion entity, SoftwareVersionDTO dto) {
        entity.setStatus(VersionStatus.fromString(dto.getStatus()));
        entity.setPackageVersion(dto.getPackageVersion());
        entity.setStatusUpdateDate(dto.getStatusUpdateDate());
        entity.setInitialSupportEndDate(dto.getInitialSupportEndDate());
        entity.setExtendedSupportEndDate(dto.getExtendedSupportEndDate());
        entity.setCyberSupportEndDate(dto.getCyberSupportEndDate());
        entity.setNotes(dto.getNotes());
    }

    /**
     * Classe interne pour le mapping des colonnes.
     */
    private static class ColumnMapping {
        int softwareNameIndex = -1;
        int versionIndex = -1;
        int versionPackageIndex = -1;
        int statusIndex = -1;
        int statusDateIndex = -1;
        int initialSupportIndex = -1;
        int extendedSupportIndex = -1;
        int cyberSupportIndex = -1;
        int notesIndex = -1;
        int vendorIndex = -1;
        int usefulLinksIndex = -1;
    }

    /**
     * Parse les liens utiles au format "Label (URL) , Label2 (URL2)"
     * et les convertit en JSON [{label, url}]
     */
    private String parseUsefulLinks(String rawLinks) {
        if (rawLinks == null || rawLinks.isBlank()) {
            return null;
        }

        List<Map<String, String>> links = new ArrayList<>();
        // Pattern pour matcher "Label (URL)"
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "([^(,]+?)\\s*\\(\\s*(https?://[^)]+)\\s*\\)"
        );
        java.util.regex.Matcher matcher = pattern.matcher(rawLinks);

        while (matcher.find()) {
            String label = matcher.group(1).trim();
            String url = matcher.group(2).trim();
            Map<String, String> link = new LinkedHashMap<>();
            link.put("label", label);
            link.put("url", url);
            links.add(link);
        }

        if (links.isEmpty()) {
            // Si pas de pattern trouve, verifier si c'est juste une URL
            if (rawLinks.trim().startsWith("http")) {
                Map<String, String> link = new LinkedHashMap<>();
                link.put("label", "Lien");
                link.put("url", rawLinks.trim());
                links.add(link);
            } else {
                return null;
            }
        }

        // Convertir en JSON simple
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < links.size(); i++) {
            if (i > 0) json.append(",");
            Map<String, String> link = links.get(i);
            json.append("{\"label\":\"")
                .append(escapeJson(link.get("label")))
                .append("\",\"url\":\"")
                .append(escapeJson(link.get("url")))
                .append("\"}");
        }
        json.append("]");
        return json.toString();
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
