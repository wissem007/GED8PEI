package com.edf.gedpei.repository;

import com.edf.gedpei.entity.ImportHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour l'historique des imports.
 */
@Repository
public interface ImportHistoryRepository extends JpaRepository<ImportHistory, Long> {

    List<ImportHistory> findByStatusOrderByImportedAtDesc(ImportHistory.ImportStatus status);

    Page<ImportHistory> findAllByOrderByImportedAtDesc(Pageable pageable);

    List<ImportHistory> findTop10ByOrderByImportedAtDesc();
}
