package com.edf.gedpei.service;

import com.edf.gedpei.dto.ServerSoftwareCreateDTO;
import com.edf.gedpei.dto.ServerSoftwareDTO;
import com.edf.gedpei.entity.Server;
import com.edf.gedpei.entity.ServerSoftware;
import com.edf.gedpei.entity.ServerSoftware.UpdateStatus;
import com.edf.gedpei.repository.ServerRepository;
import com.edf.gedpei.repository.ServerSoftwareRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service pour la gestion des logiciels installes sur les serveurs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServerSoftwareService {

    private final ServerSoftwareRepository softwareRepository;
    private final ServerRepository serverRepository;

    /**
     * Liste tous les logiciels d'un serveur.
     */
    public List<ServerSoftwareDTO> getSoftwareByServer(Long serverId) {
        return softwareRepository.findByServerId(serverId).stream()
                .map(ServerSoftwareDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Liste les logiciels actifs d'un serveur.
     */
    public List<ServerSoftwareDTO> getActiveSoftwareByServer(Long serverId) {
        return softwareRepository.findByServerIdAndActiveTrue(serverId).stream()
                .map(ServerSoftwareDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Recupere un logiciel par son ID.
     */
    public ServerSoftwareDTO getSoftwareById(Long id) {
        ServerSoftware software = softwareRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Logiciel non trouve: " + id));
        return ServerSoftwareDTO.fromEntity(software);
    }

    /**
     * Cree un nouveau logiciel sur un serveur.
     */
    @Transactional
    public ServerSoftwareDTO createSoftware(ServerSoftwareCreateDTO dto) {
        Server server = serverRepository.findById(dto.getServerId())
                .orElseThrow(() -> new EntityNotFoundException("Serveur non trouve: " + dto.getServerId()));

        ServerSoftware software = new ServerSoftware();
        software.setServer(server);
        updateSoftwareFromDTO(software, dto);
        software.setActive(true);

        software = softwareRepository.save(software);
        log.info("Logiciel {} ajoute au serveur {}", dto.getSoftwareName(), server.getResourceName());

        return ServerSoftwareDTO.fromEntity(software);
    }

    /**
     * Met a jour un logiciel.
     */
    @Transactional
    public ServerSoftwareDTO updateSoftware(Long id, ServerSoftwareCreateDTO dto) {
        ServerSoftware software = softwareRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Logiciel non trouve: " + id));

        updateSoftwareFromDTO(software, dto);
        software = softwareRepository.save(software);

        log.info("Logiciel {} mis a jour", software.getSoftwareName());
        return ServerSoftwareDTO.fromEntity(software);
    }

    /**
     * Supprime un logiciel.
     */
    @Transactional
    public void deleteSoftware(Long id) {
        ServerSoftware software = softwareRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Logiciel non trouve: " + id));

        log.info("Logiciel {} supprime du serveur {}",
                software.getSoftwareName(),
                software.getServer().getResourceName());
        softwareRepository.delete(software);
    }

    /**
     * Liste tous les noms de logiciels distincts.
     */
    public List<String> getAllSoftwareNames() {
        return softwareRepository.findDistinctSoftwareNames();
    }

    /**
     * Liste les logiciels necessitant une MAJ.
     */
    public List<ServerSoftwareDTO> getSoftwareNeedingUpdate() {
        return softwareRepository.findSoftwareNeedingUpdate().stream()
                .map(ServerSoftwareDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Compte les MAJ requises pour un serveur.
     */
    public long countUpdatesRequired(Long serverId) {
        return softwareRepository.countUpdatesRequired(serverId);
    }

    /**
     * Ajoute un logiciel standard a un serveur (helper).
     */
    @Transactional
    public ServerSoftwareDTO addStandardSoftware(Long serverId, String name, String type,
                                                  String currentVersion, UpdateStatus status) {
        ServerSoftwareCreateDTO dto = ServerSoftwareCreateDTO.builder()
                .serverId(serverId)
                .softwareName(name)
                .softwareType(type)
                .currentVersion(currentVersion)
                .updateStatus(status)
                .active(true)
                .build();
        return createSoftware(dto);
    }

    private void updateSoftwareFromDTO(ServerSoftware software, ServerSoftwareCreateDTO dto) {
        software.setSoftwareName(dto.getSoftwareName());
        software.setSoftwareType(dto.getSoftwareType());
        software.setCurrentVersion(dto.getCurrentVersion());
        software.setTargetVersion(dto.getTargetVersion());
        software.setLatestAvailableVersion(dto.getLatestAvailableVersion());
        software.setUpdateStatus(dto.getUpdateStatus() != null ? dto.getUpdateStatus() : UpdateStatus.UNKNOWN);
        software.setLastUpdateDate(dto.getLastUpdateDate());
        software.setNextUpdateDate(dto.getNextUpdateDate());
        software.setUpdatePriority(dto.getUpdatePriority());
        software.setPort(dto.getPort());
        software.setInstallPath(dto.getInstallPath());
        software.setNotes(dto.getNotes());
        if (dto.getActive() != null) {
            software.setActive(dto.getActive());
        }
    }
}
