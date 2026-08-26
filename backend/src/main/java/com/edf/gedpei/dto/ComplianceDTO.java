package com.edf.gedpei.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Ecart entre une version installee sur un serveur et la cible du referentiel CSR.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceDTO {

    private String serverName;
    private String environment;
    private String softwareName;

    /** Version relevee par Prevobs sur le serveur. */
    private String installedVersion;

    /** Solution CSR rapprochee, null si le produit n'est pas au catalogue. */
    private String csrSoftwareName;

    /** Version CSR correspondant a la version installee, null si introuvable. */
    private String csrVersion;

    /** Statut CSR de la version installee. */
    private String csrStatus;

    /** Fin de support de la version installee. */
    private LocalDate supportEndDate;

    /** Version preconisee vers laquelle migrer. */
    private String targetVersion;

    /** Fin de support de la version cible. */
    private LocalDate targetSupportEndDate;

    /** Vrai si la fin de support de la version installee est depassee. */
    private boolean supportExpired;

    /** Vrai si la version installee differe de la cible preconisee. */
    private boolean upgradeNeeded;

    private Verdict verdict;

    /**
     * Synthese de la situation d'un composant, du plus grave au plus sain.
     */
    public enum Verdict {
        CRITIQUE("Critique", "#b71c1c"),
        A_CORRIGER("A corriger", "#e53935"),
        A_SURVEILLER("A surveiller", "#fb8c00"),
        CONFORME("Conforme", "#43a047"),
        VERSION_INCONNUE("Version hors referentiel", "#757575"),
        HORS_CSR("Produit hors CSR", "#9e9e9e");

        private final String displayName;
        private final String color;

        Verdict(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getColor() {
            return color;
        }
    }
}
