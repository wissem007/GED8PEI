package com.edf.gedpei.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Historique des imports de fichiers.
 */
@Entity
@Table(name = "import_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    @Column
    private String fileType;  // CSV, XLSX

    @Column
    private Long fileSize;

    @Column
    private Integer totalRows;

    @Column
    private Integer importedRows;

    @Column
    private Integer skippedRows;

    @Column
    private Integer errorRows;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportStatus status;

    @Column(length = 2000)
    private String errorMessage;

    @Column
    private String importedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime importedAt;

    @Column
    private Long durationMs;

    public enum ImportStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        COMPLETED_WITH_ERRORS,
        FAILED
    }
}
