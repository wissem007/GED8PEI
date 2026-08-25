package com.edf.gedpei.util;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Lecture unifiee CSV / Excel pour les imports.
 *
 * <p>Les exports EDF arrivent tantot en CSV (export Power BI Prevobs), tantot en XLSX
 * (fichiers prepares sous Excel, exports Sipedia). Ce lecteur renvoie dans les deux cas
 * une liste d'onglets contenant des lignes brutes, a charge de l'appelant de reperer
 * son en-tete et ses colonnes.</p>
 */
@Slf4j
public final class SpreadsheetReader {

    private SpreadsheetReader() {
    }

    /**
     * Un onglet lu : son nom et ses lignes brutes.
     */
    public record Tab(String name, List<String[]> rows) {
    }

    /**
     * Lit le fichier quel que soit son format. Un CSV renvoie un seul onglet.
     */
    public static List<Tab> read(MultipartFile file) throws Exception {
        if (isExcel(file)) {
            return readExcel(file);
        }
        return List.of(new Tab(file.getOriginalFilename(), readCsv(file)));
    }

    public static boolean isExcel(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null) return false;
        String lower = name.toLowerCase();
        return lower.endsWith(".xlsx") || lower.endsWith(".xls") || lower.endsWith(".xlsm");
    }

    private static List<Tab> readExcel(MultipartFile file) throws Exception {
        List<Tab> tabs = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            for (Sheet sheet : workbook) {
                List<String[]> rows = new ArrayList<>();
                int width = 0;

                for (Row row : sheet) {
                    int last = row.getLastCellNum();
                    if (last < 0) {
                        rows.add(new String[0]);
                        continue;
                    }
                    width = Math.max(width, last);
                    String[] values = new String[last];
                    for (int i = 0; i < last; i++) {
                        Cell cell = row.getCell(i);
                        values[i] = cell != null ? formatter.formatCellValue(cell).trim() : "";
                    }
                    rows.add(values);
                }

                log.debug("Onglet '{}' lu: {} lignes x {} colonnes", sheet.getSheetName(), rows.size(), width);
                tabs.add(new Tab(sheet.getSheetName(), rows));
            }
        }

        return tabs;
    }

    private static List<String[]> readCsv(MultipartFile file) throws Exception {
        Charset charset = detectCharset(file);
        char delimiter;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), charset))) {
            delimiter = detectDelimiter(reader.readLine());
        }

        log.debug("CSV: encodage {}, delimiteur '{}'", charset.name(), delimiter);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), charset))) {
            CSVParser parser = new CSVParserBuilder().withSeparator(delimiter).build();
            try (CSVReader csvReader = new CSVReaderBuilder(reader).withCSVParser(parser).build()) {
                return csvReader.readAll();
            }
        }
    }

    public static Charset detectCharset(MultipartFile file) {
        try {
            byte[] bytes = file.getInputStream().readNBytes(4000);
            String content = new String(bytes, StandardCharsets.UTF_8);
            if (content.contains("\uFFFD")) {
                return Charset.forName("windows-1252");
            }
            return StandardCharsets.UTF_8;
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }

    public static char detectDelimiter(String line) {
        if (line == null) return ';';
        int semicolons = line.length() - line.replace(";", "").length();
        int commas = line.length() - line.replace(",", "").length();
        int tabs = line.length() - line.replace("\t", "").length();

        if (tabs > semicolons && tabs > commas) return '\t';
        if (commas > semicolons) return ',';
        return ';';
    }

    /**
     * Normalise un en-tete pour comparaison : majuscules, sans accents, espaces compactes.
     * Le BOM UTF-8 et les espaces insecables des exports Excel sont retires.
     */
    public static String normalizeHeader(String value) {
        if (value == null) return "";
        String cleaned = value.replace("\uFEFF", "").replace('\u00A0', ' ');
        cleaned = Normalizer.normalize(cleaned, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return cleaned.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    /**
     * Valeur nettoyee d'une cellule, ou null si absente/vide.
     */
    public static String cell(String[] row, int index) {
        if (row == null || index < 0 || index >= row.length) return null;
        String value = row[index];
        if (value == null) return null;
        value = value.replace('\u00A0', ' ').trim();
        return value.isEmpty() ? null : value;
    }

    /**
     * Vrai si la ligne ne contient que des cellules vides.
     */
    public static boolean isBlank(String[] row) {
        if (row == null) return true;
        for (String value : row) {
            if (value != null && !value.trim().isEmpty()) return false;
        }
        return true;
    }
}
