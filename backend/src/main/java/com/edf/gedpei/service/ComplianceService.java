package com.edf.gedpei.service;

import com.edf.gedpei.dto.ComplianceDTO;
import com.edf.gedpei.dto.ComplianceDTO.Verdict;
import com.edf.gedpei.entity.ServerSoftwareObsolescence;
import com.edf.gedpei.entity.SoftwareVersion;
import com.edf.gedpei.entity.SoftwareVersion.VersionStatus;
import com.edf.gedpei.repository.ServerSoftwareObsolescenceRepository;
import com.edf.gedpei.repository.SoftwareVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Rapproche les versions relevees sur les serveurs (Prevobs) du referentiel CSR
 * (Sipedia) pour faire ressortir l'ecart avec la cible preconisee.
 *
 * <p>Le rapprochement est fait en memoire : quelques dizaines de composants face a
 * quelques centaines de versions de reference, aucune requete SQL ne se justifie.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComplianceService {

    private final ServerSoftwareObsolescenceRepository obsolescenceRepository;
    private final SoftwareVersionRepository softwareVersionRepository;

    /**
     * Statuts consideres comme une cible acceptable pour une migration.
     */
    private static final List<VersionStatus> TARGET_STATUSES =
            List.of(VersionStatus.PRECONISEE, VersionStatus.TRAJECTOIRE_PRECONISEE);

    @Transactional(readOnly = true)
    public List<ComplianceDTO> getCompliance() {
        // Le catalogue CSR est indexe une fois par nom de solution normalise.
        Map<String, List<SoftwareVersion>> catalogue = softwareVersionRepository.findAll().stream()
                .filter(v -> v.getSoftwareName() != null)
                .collect(Collectors.groupingBy(v -> normalizeName(v.getSoftwareName())));

        List<ComplianceDTO> rows = obsolescenceRepository.findAll().stream()
                .map(installed -> evaluate(installed, catalogue))
                .sorted(Comparator
                        .comparing((ComplianceDTO r) -> r.getVerdict().ordinal())
                        .thenComparing(ComplianceDTO::getServerName, Comparator.nullsLast(String::compareTo))
                        .thenComparing(ComplianceDTO::getSoftwareName, Comparator.nullsLast(String::compareTo)))
                .toList();

        long unmatched = rows.stream().filter(r -> r.getVerdict() == Verdict.HORS_CSR).count();
        if (unmatched > 0) {
            log.info("Rapprochement CSR: {} composants sur {} sans solution correspondante",
                    unmatched, rows.size());
        }
        return rows;
    }

    /**
     * Repartition par verdict, dans l'ordre de gravite.
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getStats() {
        Map<String, Long> counts = getCompliance().stream()
                .collect(Collectors.groupingBy(r -> r.getVerdict().name(), Collectors.counting()));

        Map<String, Long> stats = new LinkedHashMap<>();
        for (Verdict verdict : Verdict.values()) {
            stats.put(verdict.name(), counts.getOrDefault(verdict.name(), 0L));
        }
        return stats;
    }

    private ComplianceDTO evaluate(ServerSoftwareObsolescence installed,
                                   Map<String, List<SoftwareVersion>> catalogue) {
        ComplianceDTO.ComplianceDTOBuilder row = ComplianceDTO.builder()
                .serverName(installed.getServerName())
                .environment(installed.getEnvironment())
                .softwareName(installed.getSoftwareName())
                .installedVersion(installed.getCurrentVersion());

        List<SoftwareVersion> versions = catalogue.get(normalizeName(installed.getSoftwareName()));
        if (versions == null || versions.isEmpty()) {
            return row.verdict(Verdict.HORS_CSR).build();
        }

        row.csrSoftwareName(versions.get(0).getSoftwareName());

        SoftwareVersion target = versions.stream()
                .filter(v -> TARGET_STATUSES.contains(v.getStatus()))
                // Une version preconisee prime sur une trajectoire, puis la fin de
                // support la plus lointaine.
                .max(Comparator.comparingInt((SoftwareVersion v) -> -TARGET_STATUSES.indexOf(v.getStatus()))
                        .thenComparing(SoftwareVersion::getInitialSupportEndDate,
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);

        if (target != null) {
            row.targetVersion(target.getVersion())
                    .targetSupportEndDate(target.getInitialSupportEndDate());
        }

        SoftwareVersion current = matchVersion(installed.getCurrentVersion(), versions);
        if (current == null) {
            return row.verdict(Verdict.VERSION_INCONNUE).build();
        }

        boolean expired = current.getInitialSupportEndDate() != null
                && current.getInitialSupportEndDate().isBefore(LocalDate.now());
        boolean upgradeNeeded = target != null && !target.getVersion().equals(current.getVersion());

        return row.csrVersion(current.getVersion())
                .csrStatus(current.getStatus() != null ? current.getStatus().getDisplayName() : null)
                .supportEndDate(current.getInitialSupportEndDate())
                .supportExpired(expired)
                .upgradeNeeded(upgradeNeeded)
                .verdict(verdictOf(current.getStatus(), expired))
                .build();
    }

    private Verdict verdictOf(VersionStatus status, boolean supportExpired) {
        if (status == null) {
            return Verdict.VERSION_INCONNUE;
        }
        return switch (status) {
            case INTERDITE_CYBER -> Verdict.CRITIQUE;
            case INTERDITE -> Verdict.A_CORRIGER;
            case TOLEREE, TRAJECTOIRE_PRECONISEE -> supportExpired ? Verdict.A_CORRIGER : Verdict.A_SURVEILLER;
            case PRECONISEE -> supportExpired ? Verdict.A_SURVEILLER : Verdict.CONFORME;
        };
    }

    /**
     * Rapproche une version relevee d'une version du referentiel.
     *
     * <p>Les deux sources n'ecrivent pas les versions de la meme facon : Prevobs
     * remonte la version precise du package (<code>V10.1.48</code>,
     * <code>3.2.1</code>, <code>SERVER 2019 STANDARD</code>) la ou le CSR ne porte
     * que la version de roadmap (<code>10.1</code>, <code>3.2</code>,
     * <code>2019</code>). On tente donc, dans l'ordre : egalite, prefixe sur une
     * frontiere de version, puis presence de la version CSR comme fragment.</p>
     */
    private SoftwareVersion matchVersion(String installedVersion, List<SoftwareVersion> versions) {
        String installed = normalizeVersion(installedVersion);
        if (installed.isEmpty()) {
            return null;
        }

        List<SoftwareVersion> candidates = versions.stream()
                .filter(v -> v.getVersion() != null && !normalizeVersion(v.getVersion()).isEmpty())
                // La version CSR la plus longue d'abord : 10.1 doit primer sur 10.
                .sorted(Comparator.comparingInt((SoftwareVersion v) -> normalizeVersion(v.getVersion()).length())
                        .reversed())
                .toList();

        for (SoftwareVersion candidate : candidates) {
            if (normalizeVersion(candidate.getVersion()).equals(installed)) {
                return candidate;
            }
        }

        for (SoftwareVersion candidate : candidates) {
            String reference = normalizeVersion(candidate.getVersion());
            if (isVersionPrefix(reference, installed)) {
                return candidate;
            }
            // V19C au catalogue face a V19.12.0.0 releve : la lettre de suffixe
            // Oracle ne fait pas partie du numero.
            String withoutSuffix = reference.replaceAll("[A-Z]+$", "");
            if (!withoutSuffix.equals(reference) && isVersionPrefix(withoutSuffix, installed)) {
                return candidate;
            }
        }

        for (SoftwareVersion candidate : candidates) {
            String reference = normalizeVersion(candidate.getVersion());
            // SERVER 2019 STANDARD releve face a 2019 au catalogue.
            if (reference.length() >= 3 && containsAsToken(installed, reference)) {
                return candidate;
            }
        }

        return null;
    }

    /**
     * Vrai si la reference est un prefixe de la version installee sur une frontiere
     * de composant : 10.1 est un prefixe de 10.1.48 mais pas de 10.14.
     */
    private boolean isVersionPrefix(String reference, String installed) {
        if (!installed.startsWith(reference)) {
            return false;
        }
        String remainder = installed.substring(reference.length());
        if (remainder.isEmpty()) {
            return true;
        }
        // Openssl numerote ses correctifs par une lettre collee : 1.1.1U face a 1.1.1.
        if (remainder.matches("[A-Z]+")) {
            return true;
        }
        char next = remainder.charAt(0);
        return next == '.' || next == '-' || next == '_' || next == ' ';
    }

    private boolean containsAsToken(String installed, String reference) {
        List<String> tokens = new ArrayList<>(List.of(installed.split("[ .\\-_]")));
        return tokens.contains(reference);
    }

    /**
     * Normalise un nom de solution : majuscules, sans accents ni ponctuation.
     * Rapproche ainsi JAVA OPENJDK (Prevobs) de Java OpenJDK (CSR).
     */
    private String normalizeName(String name) {
        if (name == null) return "";
        String cleaned = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return cleaned.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    /**
     * Normalise une version : majuscules, sans espaces superflus, sans le V de tete
     * que Prevobs ajoute (V10.1.48 face a 10.1 au catalogue).
     */
    private String normalizeVersion(String version) {
        if (version == null) return "";
        String cleaned = version.trim().toUpperCase().replaceAll("\\s+", " ");
        if (cleaned.matches("V[0-9].*")) {
            cleaned = cleaned.substring(1);
        }
        return cleaned;
    }
}
