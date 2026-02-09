package com.edf.gedpei.repository;

import com.edf.gedpei.entity.Migration;
import com.edf.gedpei.entity.Migration.MigrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository pour les migrations.
 */
@Repository
public interface MigrationRepository extends JpaRepository<Migration, Long> {

    /**
     * Trouve les migrations par phase.
     */
    List<Migration> findByPhaseIgnoreCase(String phase);

    /**
     * Trouve les migrations par statut.
     */
    List<Migration> findByStatus(MigrationStatus status);

    /**
     * Trouve les migrations par environnement.
     */
    List<Migration> findByEnvironmentNameIgnoreCase(String environmentName);

    /**
     * Trouve les migrations en cours ou planifiees.
     */
    @Query("SELECT m FROM Migration m WHERE m.status IN ('PLANIFIE', 'EN_COURS', 'EN_ATTENTE') ORDER BY m.dateDebut ASC")
    List<Migration> findActiveMigrations();

    /**
     * Trouve les migrations entre deux dates.
     */
    @Query("SELECT m FROM Migration m WHERE m.dateDebut >= :startDate AND m.dateFin <= :endDate ORDER BY m.dateDebut ASC")
    List<Migration> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Trouve les migrations par phase avec pagination.
     */
    Page<Migration> findByPhaseContainingIgnoreCase(String phase, Pageable pageable);

    /**
     * Recherche dans les migrations.
     */
    @Query("SELECT m FROM Migration m WHERE " +
           "LOWER(m.phase) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.responsable) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.environmentName) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Migration> search(@Param("query") String query, Pageable pageable);

    /**
     * Liste toutes les phases distinctes.
     */
    @Query("SELECT DISTINCT m.phase FROM Migration m ORDER BY m.phase")
    List<String> findAllPhases();

    /**
     * Liste tous les responsables distincts.
     */
    @Query("SELECT DISTINCT m.responsable FROM Migration m WHERE m.responsable IS NOT NULL ORDER BY m.responsable")
    List<String> findAllResponsables();

    /**
     * Compte les migrations par statut.
     */
    long countByStatus(MigrationStatus status);

    /**
     * Trouve une migration par phase, description et environnement (pour deduplication).
     */
    @Query("SELECT m FROM Migration m WHERE " +
           "LOWER(m.phase) = LOWER(:phase) AND " +
           "LOWER(m.description) = LOWER(:description) AND " +
           "(m.environmentName = :env OR (m.environmentName IS NULL AND :env IS NULL))")
    java.util.Optional<Migration> findByPhaseAndDescriptionAndEnvironment(
            @Param("phase") String phase,
            @Param("description") String description,
            @Param("env") String environmentName);
}
