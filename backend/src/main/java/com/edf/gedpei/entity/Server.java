package com.edf.gedpei.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entite principale representant un serveur, NAS ou base de donnees.
 */
@Entity
@Table(name = "servers", indexes = {
    @Index(name = "idx_server_hostname", columnList = "hostname"),
    @Index(name = "idx_server_ip_front", columnList = "ipFront"),
    @Index(name = "idx_server_environment", columnList = "environment_id"),
    @Index(name = "idx_server_site", columnList = "site_id"),
    @Index(name = "idx_server_type", columnList = "serverType")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Server {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resource_name")
    private String resourceName;

    @Column
    private String hostname;

    @Column(name = "ip_front")
    private String ipFront;

    @Column(name = "ip_admin")
    private String ipAdmin;

    @Column(name = "ip_ilo")
    private String ipIlo;

    @Column(name = "vlan_front")
    private String vlanFront;

    @Column(name = "vlan_admin")
    private String vlanAdmin;

    @Column(name = "ref_dat")
    private String refDat;

    @Enumerated(EnumType.STRING)
    @Column(name = "server_type", nullable = false)
    private ServerType serverType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "environment_id")
    private Environment environment;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "site_id")
    private Site site;

    @Column(name = "data_center")
    private String dataCenter;

    @Column(name = "last_admin_update")
    private LocalDate lastAdminUpdate;

    @Column(name = "os")
    private String os; // Linux, Windows

    @Column(name = "os_version")
    private String osVersion; // Ex: RHEL 8.6, Windows Server 2019

    @Column(name = "os_type")
    @Enumerated(EnumType.STRING)
    private OsType osType; // LINUX, WINDOWS

    @Column(name = "infrastructure_type")
    @Enumerated(EnumType.STRING)
    private InfrastructureType infrastructureType; // PHYSICAL, VIRTUAL

    /**
     * Enum pour le type d'infrastructure (physique ou virtuel).
     */
    public enum InfrastructureType {
        PHYSICAL("Physique"),
        VIRTUAL("Virtuel");

        private final String displayName;

        InfrastructureType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Relation avec les logiciels installes
    @OneToMany(mappedBy = "server", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ServerSoftware> installedSoftware = new ArrayList<>();

    /**
     * Enum pour le type d'OS.
     */
    public enum OsType {
        LINUX("Linux"),
        WINDOWS("Windows"),
        OTHER("Autre");

        private final String displayName;

        OsType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static OsType fromString(String os) {
            if (os == null) return OTHER;
            String lower = os.toLowerCase();
            if (lower.contains("linux") || lower.contains("rhel") || lower.contains("centos") ||
                lower.contains("ubuntu") || lower.contains("debian")) {
                return LINUX;
            }
            if (lower.contains("windows")) {
                return WINDOWS;
            }
            return OTHER;
        }
    }

    @Column
    private Integer vcpu;

    @Column(name = "ram_gb")
    private Integer ramGb;

    // Champs specifiques DBaaS
    @Column(name = "db_instance")
    private String dbInstance;

    @Column(name = "db_version")
    private String dbVersion;

    @Column(name = "db_type")
    private String dbType;

    // Champs specifiques NAS
    @Column(name = "nas_type")
    private String nasType;

    @Column(name = "storage_capacity_gb")
    private Integer storageCapacityGb;

    // Metadonnees
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column
    private Boolean active;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    /**
     * Retourne un identifiant unique pour la deduplication.
     */
    public String getUniqueKey() {
        StringBuilder key = new StringBuilder();

        if (hostname != null && !hostname.isBlank()) {
            key.append(hostname.toLowerCase().trim());
        } else if (resourceName != null) {
            key.append(resourceName.toLowerCase().trim());
        }

        if (ipFront != null && !ipFront.isBlank()) {
            key.append(":").append(ipFront.trim());
        }

        return key.toString();
    }

    /**
     * Verifie si ce serveur correspond a une recherche.
     */
    public boolean matchesSearch(String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        String lowerQuery = query.toLowerCase().trim();

        return (resourceName != null && resourceName.toLowerCase().contains(lowerQuery)) ||
               (hostname != null && hostname.toLowerCase().contains(lowerQuery)) ||
               (ipFront != null && ipFront.contains(lowerQuery)) ||
               (ipAdmin != null && ipAdmin.contains(lowerQuery)) ||
               (refDat != null && refDat.toLowerCase().contains(lowerQuery)) ||
               (dbInstance != null && dbInstance.toLowerCase().contains(lowerQuery));
    }
}
