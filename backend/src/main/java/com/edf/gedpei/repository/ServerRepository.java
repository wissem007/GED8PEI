package com.edf.gedpei.repository;

import com.edf.gedpei.entity.Environment;
import com.edf.gedpei.entity.Server;
import com.edf.gedpei.entity.ServerType;
import com.edf.gedpei.entity.Site;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour les serveurs.
 */
@Repository
public interface ServerRepository extends JpaRepository<Server, Long> {

    // Recherche par hostname
    Optional<Server> findByHostnameIgnoreCase(String hostname);

    List<Server> findByHostnameContainingIgnoreCase(String hostname);

    // Recherche par IP
    Optional<Server> findByIpFront(String ipFront);

    List<Server> findByIpFrontContaining(String ip);

    List<Server> findByIpAdminContaining(String ip);

    // Recherche par environnement
    List<Server> findByEnvironment(Environment environment);

    List<Server> findByEnvironmentName(String environmentName);

    Page<Server> findByEnvironment(Environment environment, Pageable pageable);

    // Recherche par site
    List<Server> findBySite(Site site);

    List<Server> findBySiteCode(String siteCode);

    Page<Server> findBySite(Site site, Pageable pageable);

    // Recherche par type
    List<Server> findByServerType(ServerType serverType);

    Page<Server> findByServerType(ServerType serverType, Pageable pageable);

    // Recherche combinee
    Page<Server> findByEnvironmentAndServerType(Environment environment, ServerType serverType, Pageable pageable);

    Page<Server> findBySiteAndServerType(Site site, ServerType serverType, Pageable pageable);

    Page<Server> findByEnvironmentAndSite(Environment environment, Site site, Pageable pageable);

    Page<Server> findByEnvironmentAndSiteAndServerType(
            Environment environment, Site site, ServerType serverType, Pageable pageable);

    // Recherche full-text
    @Query("SELECT s FROM Server s WHERE " +
           "LOWER(s.resourceName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.hostname) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "s.ipFront LIKE CONCAT('%', :query, '%') OR " +
           "s.ipAdmin LIKE CONCAT('%', :query, '%') OR " +
           "LOWER(s.refDat) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.dbInstance) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Server> search(@Param("query") String query, Pageable pageable);

    @Query("SELECT s FROM Server s WHERE " +
           "(LOWER(s.resourceName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.hostname) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "s.ipFront LIKE CONCAT('%', :query, '%') OR " +
           "s.ipAdmin LIKE CONCAT('%', :query, '%')) AND " +
           "s.environment = :environment")
    Page<Server> searchByEnvironment(
            @Param("query") String query,
            @Param("environment") Environment environment,
            Pageable pageable);

    // Statistiques
    @Query("SELECT COUNT(s) FROM Server s WHERE s.environment = :environment")
    long countByEnvironment(@Param("environment") Environment environment);

    @Query("SELECT COUNT(s) FROM Server s WHERE s.site = :site")
    long countBySite(@Param("site") Site site);

    @Query("SELECT COUNT(s) FROM Server s WHERE s.serverType = :serverType")
    long countByServerType(@Param("serverType") ServerType serverType);

    @Query("SELECT s.environment.name, COUNT(s) FROM Server s GROUP BY s.environment.name")
    List<Object[]> countGroupByEnvironment();

    @Query("SELECT s.site.code, COUNT(s) FROM Server s GROUP BY s.site.code")
    List<Object[]> countGroupBySite();

    @Query("SELECT s.serverType, COUNT(s) FROM Server s GROUP BY s.serverType")
    List<Object[]> countGroupByServerType();

    // Verification existence pour import
    @Query("SELECT COUNT(s) > 0 FROM Server s WHERE " +
           "(s.hostname = :hostname AND s.hostname IS NOT NULL AND s.hostname <> '') OR " +
           "(s.ipFront = :ipFront AND s.ipFront IS NOT NULL AND s.ipFront <> '')")
    boolean existsByHostnameOrIpFront(@Param("hostname") String hostname, @Param("ipFront") String ipFront);

    // Trouver par cle unique
    @Query("SELECT s FROM Server s WHERE " +
           "(s.hostname = :hostname AND s.hostname IS NOT NULL AND s.hostname <> '') OR " +
           "(s.resourceName = :resourceName AND s.ipFront = :ipFront)")
    Optional<Server> findByUniqueKey(
            @Param("hostname") String hostname,
            @Param("resourceName") String resourceName,
            @Param("ipFront") String ipFront);
}
