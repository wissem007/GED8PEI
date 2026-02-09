package com.edf.gedpei.repository;

import com.edf.gedpei.entity.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour les sites.
 */
@Repository
public interface SiteRepository extends JpaRepository<Site, Long> {

    Optional<Site> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);
}
