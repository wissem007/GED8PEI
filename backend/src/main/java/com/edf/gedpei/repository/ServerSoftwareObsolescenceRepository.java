package com.edf.gedpei.repository;

import com.edf.gedpei.entity.ServerSoftwareObsolescence;
import com.edf.gedpei.entity.ServerSoftwareObsolescence.ObsolescenceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServerSoftwareObsolescenceRepository extends JpaRepository<ServerSoftwareObsolescence, Long> {

    List<ServerSoftwareObsolescence> findByServerNameIgnoreCase(String serverName);

    List<ServerSoftwareObsolescence> findByEnvironmentIgnoreCase(String environment);

    List<ServerSoftwareObsolescence> findByStatus(ObsolescenceStatus status);

    @Query(value = "SELECT DISTINCT CAST(s.environment AS VARCHAR) AS env FROM server_software_obsolescence s WHERE s.environment IS NOT NULL ORDER BY env", nativeQuery = true)
    List<String> findDistinctEnvironments();

    @Query(value = "SELECT DISTINCT CAST(s.server_name AS VARCHAR) AS srv FROM server_software_obsolescence s ORDER BY srv", nativeQuery = true)
    List<String> findDistinctServerNames();

    @Query(value = "SELECT DISTINCT CAST(s.software_name AS VARCHAR) AS sw FROM server_software_obsolescence s ORDER BY sw", nativeQuery = true)
    List<String> findDistinctSoftwareNames();

    @Query(value = "SELECT CAST(s.status AS VARCHAR), COUNT(*) FROM server_software_obsolescence s GROUP BY s.status", nativeQuery = true)
    List<Object[]> countByStatus();

    @Query(value = "SELECT CAST(s.server_name AS VARCHAR), CAST(s.status AS VARCHAR), COUNT(*) FROM server_software_obsolescence s GROUP BY s.server_name, s.status ORDER BY s.server_name", nativeQuery = true)
    List<Object[]> countByServerAndStatus();

    @Query(value = "SELECT * FROM server_software_obsolescence s WHERE " +
           "(:environment IS NULL OR s.environment = CAST(:environment AS VARCHAR)) AND " +
           "(:serverName IS NULL OR s.server_name = CAST(:serverName AS VARCHAR)) AND " +
           "(:status IS NULL OR s.status = CAST(:status AS VARCHAR)) AND " +
           "(:softwareName IS NULL OR s.software_name LIKE CONCAT('%', CAST(:softwareName AS VARCHAR), '%')) " +
           "ORDER BY s.environment, s.server_name, s.status, s.software_name",
           nativeQuery = true)
    List<ServerSoftwareObsolescence> filter(
            @Param("environment") String environment,
            @Param("serverName") String serverName,
            @Param("status") String status,
            @Param("softwareName") String softwareName);

    void deleteByServerName(String serverName);

    void deleteByEnvironment(String environment);
}
