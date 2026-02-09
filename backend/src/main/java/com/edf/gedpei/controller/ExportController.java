package com.edf.gedpei.controller;

import com.edf.gedpei.dto.ServerDTO;
import com.edf.gedpei.service.ServerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controleur REST pour l'export de donnees.
 */
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@Tag(name = "Export", description = "API d'export de donnees")
public class ExportController {

    private final ServerService serverService;

    @GetMapping("/csv")
    @Operation(summary = "Exporte les serveurs en CSV")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) String environment,
            @RequestParam(required = false) String site) {

        Page<ServerDTO> servers = serverService.filterServers(
                environment, site, null, null, PageRequest.of(0, 10000));

        StringBuilder csv = new StringBuilder();

        // En-tetes
        csv.append("ID;Nom Ressource;Hostname;IP Front;IP Admin;IP ILO;Ref DAT;")
           .append("Type;Environnement;Site;Datacenter;Date MAJ;OS;Version OS;")
           .append("Instance DB;Version DB;Notes\n");

        // Donnees
        for (ServerDTO server : servers.getContent()) {
            csv.append(nullSafe(server.getId())).append(";");
            csv.append(escapeCsv(server.getResourceName())).append(";");
            csv.append(escapeCsv(server.getHostname())).append(";");
            csv.append(escapeCsv(server.getIpFront())).append(";");
            csv.append(escapeCsv(server.getIpAdmin())).append(";");
            csv.append(escapeCsv(server.getIpIlo())).append(";");
            csv.append(escapeCsv(server.getRefDat())).append(";");
            csv.append(nullSafe(server.getServerTypeDisplay())).append(";");
            csv.append(nullSafe(server.getEnvironmentName())).append(";");
            csv.append(nullSafe(server.getSiteCode())).append(";");
            csv.append(nullSafe(server.getDataCenter())).append(";");
            csv.append(server.getLastAdminUpdate() != null ?
                    server.getLastAdminUpdate().toString() : "").append(";");
            csv.append(nullSafe(server.getOs())).append(";");
            csv.append(nullSafe(server.getOsVersion())).append(";");
            csv.append(nullSafe(server.getDbInstance())).append(";");
            csv.append(nullSafe(server.getDbVersion())).append(";");
            csv.append(escapeCsv(server.getNotes())).append("\n");
        }

        byte[] data = csv.toString().getBytes();
        String filename = "export_serveurs_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(data);
    }

    @GetMapping("/excel")
    @Operation(summary = "Exporte les serveurs en Excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) String environment,
            @RequestParam(required = false) String site) throws Exception {

        Page<ServerDTO> servers = serverService.filterServers(
                environment, site, null, null, PageRequest.of(0, 10000));

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Serveurs");

            // Style en-tetes
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // En-tetes
            String[] headers = {
                "ID", "Nom Ressource", "Hostname", "IP Front", "IP Admin", "IP ILO",
                "Ref DAT", "Type", "Environnement", "Site", "Datacenter",
                "Date MAJ", "OS", "Version OS", "Instance DB", "Version DB", "Notes"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Donnees
            int rowNum = 1;
            for (ServerDTO server : servers.getContent()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(server.getId() != null ? server.getId() : 0);
                row.createCell(1).setCellValue(nullSafe(server.getResourceName()));
                row.createCell(2).setCellValue(nullSafe(server.getHostname()));
                row.createCell(3).setCellValue(nullSafe(server.getIpFront()));
                row.createCell(4).setCellValue(nullSafe(server.getIpAdmin()));
                row.createCell(5).setCellValue(nullSafe(server.getIpIlo()));
                row.createCell(6).setCellValue(nullSafe(server.getRefDat()));
                row.createCell(7).setCellValue(nullSafe(server.getServerTypeDisplay()));
                row.createCell(8).setCellValue(nullSafe(server.getEnvironmentName()));
                row.createCell(9).setCellValue(nullSafe(server.getSiteCode()));
                row.createCell(10).setCellValue(nullSafe(server.getDataCenter()));
                row.createCell(11).setCellValue(server.getLastAdminUpdate() != null ?
                        server.getLastAdminUpdate().toString() : "");
                row.createCell(12).setCellValue(nullSafe(server.getOs()));
                row.createCell(13).setCellValue(nullSafe(server.getOsVersion()));
                row.createCell(14).setCellValue(nullSafe(server.getDbInstance()));
                row.createCell(15).setCellValue(nullSafe(server.getDbVersion()));
                row.createCell(16).setCellValue(nullSafe(server.getNotes()));
            }

            // Auto-size colonnes
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);

            String filename = "export_serveurs_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(baos.toByteArray());
        }
    }

    private String nullSafe(Object value) {
        return value != null ? value.toString() : "";
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
