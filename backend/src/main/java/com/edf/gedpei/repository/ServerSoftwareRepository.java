package com.edf.gedpei.repository;

import com.edf.gedpei.entity.ServerSoftware;
import com.edf.gedpei.entity.ServerSoftware.UpdateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServerSoftwareRepository extends JpaRepository<ServerSoftware, Long> {

    List<ServerSoftware> findByServerId(Long serverId);

    List<ServerSoftware> findByServerIdAndActiveTrue(Long serverId);

    List<ServerSoftware> findBySoftwareNameIgnoreCase(String softwareName);

    List<ServerSoftware> findByUpdateStatus(UpdateStatus status);

    @Query("SELECT DISTINCT ss.softwareName FROM ServerSoftware ss ORDER BY ss.softwareName")
    List<String> findDistinctSoftwareNames();

    @Query("SELECT ss FROM ServerSoftware ss WHERE ss.updateStatus IN ('UPDATE_REQUIRED', 'END_OF_LIFE')")
    List<ServerSoftware> findSoftwareNeedingUpdate();

    @Query("SELECT ss FROM ServerSoftware ss WHERE ss.server.id = :serverId AND ss.softwareName = :softwareName")
    ServerSoftware findByServerIdAndSoftwareName(Long serverId, String softwareName);

    @Query("SELECT COUNT(ss) FROM ServerSoftware ss WHERE ss.server.id = :serverId AND ss.updateStatus = 'UPDATE_REQUIRED'")
    long countUpdatesRequired(Long serverId);
}
