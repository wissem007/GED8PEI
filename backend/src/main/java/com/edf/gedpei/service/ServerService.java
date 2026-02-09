package com.edf.gedpei.service;

import com.edf.gedpei.dto.ServerCreateDTO;
import com.edf.gedpei.dto.ServerDTO;
import com.edf.gedpei.entity.*;
import com.edf.gedpei.repository.EnvironmentRepository;
import com.edf.gedpei.repository.ServerRepository;
import com.edf.gedpei.repository.SiteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service pour la gestion des serveurs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServerService {

    private final ServerRepository serverRepository;
    private final EnvironmentRepository environmentRepository;
    private final SiteRepository siteRepository;

    /**
     * Recupere tous les serveurs avec pagination.
     */
    public Page<ServerDTO> getAllServers(Pageable pageable) {
        return serverRepository.findAll(pageable)
                .map(ServerDTO::fromEntity);
    }

    /**
     * Recupere un serveur par son ID.
     */
    public ServerDTO getServerById(Long id) {
        Server server = serverRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Serveur non trouve: " + id));
        return ServerDTO.fromEntity(server);
    }

    /**
     * Recherche des serveurs.
     */
    public Page<ServerDTO> searchServers(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return getAllServers(pageable);
        }
        return serverRepository.search(query.trim(), pageable)
                .map(ServerDTO::fromEntity);
    }

    /**
     * Filtre les serveurs par environnement.
     */
    public Page<ServerDTO> getServersByEnvironment(String environmentName, Pageable pageable) {
        Environment env = environmentRepository.findByNameIgnoreCase(environmentName)
                .orElseThrow(() -> new EntityNotFoundException("Environnement non trouve: " + environmentName));
        return serverRepository.findByEnvironment(env, pageable)
                .map(ServerDTO::fromEntity);
    }

    /**
     * Filtre les serveurs par site.
     */
    public Page<ServerDTO> getServersBySite(String siteCode, Pageable pageable) {
        Site site = siteRepository.findByCodeIgnoreCase(siteCode)
                .orElseThrow(() -> new EntityNotFoundException("Site non trouve: " + siteCode));
        return serverRepository.findBySite(site, pageable)
                .map(ServerDTO::fromEntity);
    }

    /**
     * Filtre les serveurs par type.
     */
    public Page<ServerDTO> getServersByType(ServerType serverType, Pageable pageable) {
        return serverRepository.findByServerType(serverType, pageable)
                .map(ServerDTO::fromEntity);
    }

    /**
     * Filtre combine.
     */
    public Page<ServerDTO> filterServers(
            String environmentName,
            String siteCode,
            ServerType serverType,
            String query,
            Pageable pageable) {

        // Si une recherche est specifiee
        if (query != null && !query.isBlank()) {
            return searchServers(query, pageable);
        }

        Environment env = null;
        Site site = null;

        if (environmentName != null && !environmentName.isBlank()) {
            env = environmentRepository.findByNameIgnoreCase(environmentName).orElse(null);
        }

        if (siteCode != null && !siteCode.isBlank()) {
            site = siteRepository.findByCodeIgnoreCase(siteCode).orElse(null);
        }

        // Filtres combines
        if (env != null && site != null && serverType != null) {
            return serverRepository.findByEnvironmentAndSiteAndServerType(env, site, serverType, pageable)
                    .map(ServerDTO::fromEntity);
        }
        if (env != null && site != null) {
            return serverRepository.findByEnvironmentAndSite(env, site, pageable)
                    .map(ServerDTO::fromEntity);
        }
        if (env != null && serverType != null) {
            return serverRepository.findByEnvironmentAndServerType(env, serverType, pageable)
                    .map(ServerDTO::fromEntity);
        }
        if (site != null && serverType != null) {
            return serverRepository.findBySiteAndServerType(site, serverType, pageable)
                    .map(ServerDTO::fromEntity);
        }
        if (env != null) {
            return serverRepository.findByEnvironment(env, pageable)
                    .map(ServerDTO::fromEntity);
        }
        if (site != null) {
            return serverRepository.findBySite(site, pageable)
                    .map(ServerDTO::fromEntity);
        }
        if (serverType != null) {
            return serverRepository.findByServerType(serverType, pageable)
                    .map(ServerDTO::fromEntity);
        }

        return getAllServers(pageable);
    }

    /**
     * Cree un nouveau serveur.
     */
    @Transactional
    public ServerDTO createServer(ServerCreateDTO dto, String createdBy) {
        Server server = new Server();
        updateServerFromDTO(server, dto);
        server.setCreatedBy(createdBy);
        server.setActive(true);

        server = serverRepository.save(server);
        log.info("Serveur cree: {} par {}", server.getResourceName(), createdBy);

        return ServerDTO.fromEntity(server);
    }

    /**
     * Met a jour un serveur.
     */
    @Transactional
    public ServerDTO updateServer(Long id, ServerCreateDTO dto, String updatedBy) {
        Server server = serverRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Serveur non trouve: " + id));

        updateServerFromDTO(server, dto);
        server.setUpdatedBy(updatedBy);

        server = serverRepository.save(server);
        log.info("Serveur mis a jour: {} par {}", server.getResourceName(), updatedBy);

        return ServerDTO.fromEntity(server);
    }

    /**
     * Supprime un serveur.
     */
    @Transactional
    public void deleteServer(Long id, String deletedBy) {
        Server server = serverRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Serveur non trouve: " + id));

        log.info("Serveur supprime: {} par {}", server.getResourceName(), deletedBy);
        serverRepository.delete(server);
    }

    /**
     * Met a jour une entite Server depuis un DTO.
     */
    private void updateServerFromDTO(Server server, ServerCreateDTO dto) {
        server.setResourceName(dto.getResourceName());
        server.setHostname(dto.getHostname());
        server.setIpFront(dto.getIpFront());
        server.setIpAdmin(dto.getIpAdmin());
        server.setIpIlo(dto.getIpIlo());
        server.setRefDat(dto.getRefDat());
        server.setDataCenter(dto.getDataCenter());
        server.setLastAdminUpdate(dto.getLastAdminUpdate());
        server.setOs(dto.getOperatingSystem());
        server.setOsVersion(dto.getOsVersion());
        server.setInfrastructureType(dto.getInfrastructureType());
        server.setNotes(dto.getNotes());

        if (dto.getEnabled() != null) {
            server.setActive(dto.getEnabled());
        }

        // Champs DBaaS
        server.setDbInstance(dto.getDbInstance());
        server.setDbVersion(dto.getDbVersion());
        server.setDbType(dto.getDbType());

        // Type
        if (dto.getServerType() != null) {
            server.setServerType(dto.getServerType());
        } else {
            server.setServerType(ServerType.detectFromResourceName(
                    dto.getResourceName(), dto.getEnvironmentName()));
        }

        // Environnement
        if (dto.getEnvironmentName() != null && !dto.getEnvironmentName().isBlank()) {
            String envCode = Environment.normalizeCode(dto.getEnvironmentName());
            Environment env = environmentRepository.findByCodeIgnoreCase(envCode)
                    .orElseGet(() -> {
                        Environment newEnv = new Environment(envCode);
                        return environmentRepository.save(newEnv);
                    });
            server.setEnvironment(env);
        }

        // Site
        if (dto.getSiteCode() != null && !dto.getSiteCode().isBlank()) {
            String siteCode = Site.extractSiteCode(dto.getSiteCode(), dto.getDataCenter());
            Site site = siteRepository.findByCodeIgnoreCase(siteCode)
                    .orElseGet(() -> {
                        Site newSite = new Site(siteCode);
                        double[] coords = Site.getCoordinates(siteCode);
                        newSite.setLatitude(coords[0]);
                        newSite.setLongitude(coords[1]);
                        return siteRepository.save(newSite);
                    });
            server.setSite(site);
        }
    }

    /**
     * Liste tous les environnements.
     */
    public List<String> getAllEnvironments() {
        return environmentRepository.findAll().stream()
                .map(Environment::getName)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Liste tous les sites.
     */
    public List<String> getAllSites() {
        return siteRepository.findAll().stream()
                .map(Site::getCode)
                .sorted()
                .collect(Collectors.toList());
    }
}
