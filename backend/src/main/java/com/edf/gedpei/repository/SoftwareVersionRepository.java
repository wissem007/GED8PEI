package com.edf.gedpei.repository;

import com.edf.gedpei.entity.SoftwareVersion;
import com.edf.gedpei.entity.SoftwareVersion.VersionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour les versions de logiciels.
 */
@Repository
public interface SoftwareVersionRepository extends JpaRepository<SoftwareVersion, Long> {

    Optional<SoftwareVersion> findBySoftwareNameIgnoreCaseAndVersionIgnoreCase(String softwareName, String version);

    List<SoftwareVersion> findBySoftwareNameIgnoreCaseOrderByVersionDesc(String softwareName);

    List<SoftwareVersion> findByStatus(VersionStatus status);

    @Query("SELECT DISTINCT sv.softwareName FROM SoftwareVersion sv ORDER BY sv.softwareName")
    List<String> findDistinctSoftwareNames();

    @Query("SELECT sv FROM SoftwareVersion sv WHERE " +
           "(:softwareName IS NULL OR LOWER(CAST(sv.softwareName AS string)) LIKE LOWER(CONCAT('%', CAST(:softwareName AS string), '%'))) AND " +
           "(:version IS NULL OR LOWER(CAST(sv.version AS string)) LIKE LOWER(CONCAT('%', CAST(:version AS string), '%'))) AND " +
           "(:status IS NULL OR sv.status = :status)")
    Page<SoftwareVersion> filterVersions(
            @Param("softwareName") String softwareName,
            @Param("version") String version,
            @Param("status") VersionStatus status,
            Pageable pageable);

    @Query("SELECT COUNT(sv) FROM SoftwareVersion sv WHERE sv.status = :status")
    long countByStatus(@Param("status") VersionStatus status);

    boolean existsBySoftwareNameIgnoreCaseAndVersionIgnoreCase(String softwareName, String version);
}
