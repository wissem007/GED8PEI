package com.edf.gedpei.repository;

import com.edf.gedpei.entity.Alert;
import com.edf.gedpei.entity.Alert.AlertSeverity;
import com.edf.gedpei.entity.Alert.AlertStatus;
import com.edf.gedpei.entity.Alert.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByStatus(AlertStatus status);

    List<Alert> findBySeverity(AlertSeverity severity);

    List<Alert> findByAlertType(AlertType alertType);

    List<Alert> findByServerName(String serverName);

    List<Alert> findByEnvironment(String environment);

    List<Alert> findByStatusOrderBySeverityDescCreatedAtDesc(AlertStatus status);

    List<Alert> findByStatusInOrderBySeverityDescCreatedAtDesc(List<AlertStatus> statuses);

    @Query("SELECT a FROM Alert a WHERE a.status = :status ORDER BY " +
           "CASE a.severity " +
           "WHEN 'CRITICAL' THEN 1 " +
           "WHEN 'HIGH' THEN 2 " +
           "WHEN 'MEDIUM' THEN 3 " +
           "WHEN 'LOW' THEN 4 " +
           "ELSE 5 END, a.createdAt DESC")
    List<Alert> findActiveAlertsSortedBySeverity(@Param("status") AlertStatus status);

    @Query("SELECT a.severity, COUNT(a) FROM Alert a WHERE a.status = :status GROUP BY a.severity")
    List<Object[]> countBySeverity(@Param("status") AlertStatus status);

    @Query("SELECT a.alertType, COUNT(a) FROM Alert a WHERE a.status = :status GROUP BY a.alertType")
    List<Object[]> countByType(@Param("status") AlertStatus status);

    @Query("SELECT a.serverName, COUNT(a) FROM Alert a WHERE a.status = :status AND a.serverName IS NOT NULL GROUP BY a.serverName ORDER BY COUNT(a) DESC")
    List<Object[]> countByServer(@Param("status") AlertStatus status);

    @Query("SELECT a.environment, COUNT(a) FROM Alert a WHERE a.status = :status AND a.environment IS NOT NULL GROUP BY a.environment")
    List<Object[]> countByEnvironment(@Param("status") AlertStatus status);

    long countByStatus(AlertStatus status);

    long countByStatusAndSeverity(AlertStatus status, AlertSeverity severity);

    @Query("SELECT a FROM Alert a WHERE " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:severity IS NULL OR a.severity = :severity) AND " +
           "(:alertType IS NULL OR a.alertType = :alertType) AND " +
           "(:serverName IS NULL OR a.serverName = :serverName) AND " +
           "(:environment IS NULL OR a.environment = :environment) " +
           "ORDER BY CASE a.severity " +
           "WHEN 'CRITICAL' THEN 1 " +
           "WHEN 'HIGH' THEN 2 " +
           "WHEN 'MEDIUM' THEN 3 " +
           "WHEN 'LOW' THEN 4 " +
           "ELSE 5 END, a.createdAt DESC")
    List<Alert> filter(
            @Param("status") AlertStatus status,
            @Param("severity") AlertSeverity severity,
            @Param("alertType") AlertType alertType,
            @Param("serverName") String serverName,
            @Param("environment") String environment);

    // Check if similar alert already exists (to avoid duplicates)
    boolean existsByServerNameAndSoftwareNameAndAlertTypeAndStatus(
            String serverName, String softwareName, AlertType alertType, AlertStatus status);

    void deleteByServerNameAndSoftwareName(String serverName, String softwareName);
}
