package com.edf.gedpei.entity;

/**
 * Enum pour les types de ressources.
 */
public enum ServerType {
    SERVER("Serveur"),
    NAS("NAS"),
    DBAAS("Base de donnees DBaaS");

    private final String displayName;

    ServerType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Detecte automatiquement le type depuis le nom de la ressource ou l'environnement.
     */
    public static ServerType detectFromResourceName(String resourceName, String environment) {
        if (resourceName == null) {
            resourceName = "";
        }
        if (environment == null) {
            environment = "";
        }

        String lowerResource = resourceName.toLowerCase();
        String lowerEnv = environment.toLowerCase();

        if (lowerResource.contains("nas") || lowerEnv.contains("nas")) {
            return NAS;
        }
        if (lowerResource.contains("dbaas") || lowerResource.contains("b6a") ||
            lowerEnv.contains("dbaas") || lowerResource.contains("_bd_")) {
            return DBAAS;
        }
        return SERVER;
    }
}
