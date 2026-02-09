-- =============================================================================
-- Schema GED-PEI - Base de données PostgreSQL
-- =============================================================================
-- Ce script crée le schéma de la base de données pour l'application GED-PEI
-- Exécuter ce script sur la base 'gedpei' avec un utilisateur ayant les droits

-- -----------------------------------------------------------------------------
-- Table: environments (Environnements)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS environments (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    color VARCHAR(20) DEFAULT '#1976d2',
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE environments IS 'Table des environnements (PROD, PREPROD, RECETTE, DEV, etc.)';
COMMENT ON COLUMN environments.code IS 'Code unique de l''environnement';
COMMENT ON COLUMN environments.color IS 'Couleur d''affichage dans l''interface';

-- -----------------------------------------------------------------------------
-- Table: sites (Sites géographiques)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sites (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    region VARCHAR(100),
    country VARCHAR(100) DEFAULT 'France',
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    address VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sites IS 'Table des sites géographiques (Guadeloupe, Martinique, etc.)';
COMMENT ON COLUMN sites.latitude IS 'Latitude GPS pour la cartographie';
COMMENT ON COLUMN sites.longitude IS 'Longitude GPS pour la cartographie';

-- -----------------------------------------------------------------------------
-- Table: servers (Serveurs et ressources)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS servers (
    id BIGSERIAL PRIMARY KEY,

    -- Identification
    resource_name VARCHAR(255),
    hostname VARCHAR(255),
    ref_dat VARCHAR(100),

    -- Type de ressource
    server_type VARCHAR(50) NOT NULL DEFAULT 'SERVER',

    -- Réseau
    ip_front VARCHAR(50),
    ip_admin VARCHAR(50),
    ip_ilo VARCHAR(50),
    vlan_front VARCHAR(50),
    vlan_admin VARCHAR(50),

    -- Localisation
    environment_id BIGINT REFERENCES environments(id),
    site_id BIGINT REFERENCES sites(id),
    data_center VARCHAR(100),

    -- Système
    os VARCHAR(100),
    os_version VARCHAR(100),
    vcpu INTEGER,
    ram_gb INTEGER,

    -- DBaaS spécifique
    db_instance VARCHAR(255),
    db_version VARCHAR(100),
    db_type VARCHAR(50),

    -- NAS spécifique
    nas_type VARCHAR(100),
    storage_capacity_gb INTEGER,

    -- Métadonnées
    last_admin_update DATE,
    notes TEXT,
    active BOOLEAN DEFAULT TRUE,

    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),

    -- Contraintes
    CONSTRAINT chk_server_type CHECK (server_type IN ('SERVER', 'NAS', 'DBAAS'))
);

COMMENT ON TABLE servers IS 'Table principale des serveurs, NAS et bases de données';
COMMENT ON COLUMN servers.server_type IS 'Type: SERVER, NAS ou DBAAS';
COMMENT ON COLUMN servers.ip_front IS 'Adresse IP frontale';
COMMENT ON COLUMN servers.ip_admin IS 'Adresse IP d''administration';
COMMENT ON COLUMN servers.ip_ilo IS 'Adresse IP ILO/IDRAC/BMC';

-- Index pour les recherches fréquentes
CREATE INDEX IF NOT EXISTS idx_servers_hostname ON servers(hostname);
CREATE INDEX IF NOT EXISTS idx_servers_ip_front ON servers(ip_front);
CREATE INDEX IF NOT EXISTS idx_servers_resource_name ON servers(resource_name);
CREATE INDEX IF NOT EXISTS idx_servers_environment ON servers(environment_id);
CREATE INDEX IF NOT EXISTS idx_servers_site ON servers(site_id);
CREATE INDEX IF NOT EXISTS idx_servers_type ON servers(server_type);

-- -----------------------------------------------------------------------------
-- Table: users (Utilisateurs)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    full_name VARCHAR(255),
    roles VARCHAR(500) DEFAULT 'USER',
    enabled BOOLEAN DEFAULT TRUE,
    last_login TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE users IS 'Table des utilisateurs de l''application';
COMMENT ON COLUMN users.roles IS 'Rôles séparés par virgule (USER, ADMIN)';

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- -----------------------------------------------------------------------------
-- Table: import_history (Historique des imports)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS import_history (
    id BIGSERIAL PRIMARY KEY,
    filename VARCHAR(500) NOT NULL,
    file_type VARCHAR(20),
    file_size BIGINT,
    total_rows INTEGER,
    imported_rows INTEGER DEFAULT 0,
    skipped_rows INTEGER DEFAULT 0,
    error_rows INTEGER DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    imported_by VARCHAR(100),
    imported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    duration_ms BIGINT,

    CONSTRAINT chk_import_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'COMPLETED_WITH_ERRORS', 'FAILED'))
);

COMMENT ON TABLE import_history IS 'Historique des imports de fichiers CSV/Excel';

CREATE INDEX IF NOT EXISTS idx_import_history_status ON import_history(status);
CREATE INDEX IF NOT EXISTS idx_import_history_date ON import_history(imported_at DESC);

-- -----------------------------------------------------------------------------
-- Table: software_versions (Versions de logiciels)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS software_versions (
    id BIGSERIAL PRIMARY KEY,
    software_name TEXT NOT NULL,
    version TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'TOLEREE',
    status_update_date DATE,
    initial_support_end_date DATE,
    extended_support_end_date DATE,
    cyber_support_end_date DATE,
    notes TEXT,
    useful_links TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_software_version UNIQUE (software_name, version),
    CONSTRAINT chk_version_status CHECK (status IN ('PRECONISEE', 'TRAJECTOIRE_PRECONISEE', 'TOLEREE', 'INTERDITE', 'INTERDITE_CYBER'))
);

COMMENT ON TABLE software_versions IS 'Table des versions de logiciels avec leur statut';
COMMENT ON COLUMN software_versions.status IS 'Statut: PRECONISEE, TRAJECTOIRE_PRECONISEE, TOLEREE, INTERDITE, INTERDITE_CYBER';
COMMENT ON COLUMN software_versions.useful_links IS 'Liens utiles au format JSON: [{"label": "...", "url": "..."}]';

CREATE INDEX IF NOT EXISTS idx_software_versions_name ON software_versions(software_name);
CREATE INDEX IF NOT EXISTS idx_software_versions_status ON software_versions(status);

-- Trigger pour updated_at sur software_versions
DROP TRIGGER IF EXISTS update_software_versions_updated_at ON software_versions;
CREATE TRIGGER update_software_versions_updated_at
    BEFORE UPDATE ON software_versions
    FOR EACH ROW
    EXECUTE PROCEDURE update_updated_at_column();

-- -----------------------------------------------------------------------------
-- Données initiales: Environnements
-- -----------------------------------------------------------------------------
INSERT INTO environments (code, name, description, color, display_order) VALUES
    ('PROD', 'Production', 'Environnement de production', '#d32f2f', 1),
    ('PREPROD', 'Pré-production', 'Environnement de pré-production', '#f57c00', 2),
    ('RECETTE', 'Recette', 'Environnement de recette/UAT', '#1976d2', 3),
    ('DEV', 'Développement', 'Environnement de développement', '#388e3c', 4),
    ('INT', 'Intégration', 'Environnement d''intégration', '#7b1fa2', 5),
    ('QUALIF', 'Qualification', 'Environnement de qualification', '#0097a7', 6)
ON CONFLICT (code) DO NOTHING;

-- -----------------------------------------------------------------------------
-- Données initiales: Sites
-- -----------------------------------------------------------------------------
INSERT INTO sites (code, name, region, latitude, longitude) VALUES
    ('GUADELOUPE', 'Guadeloupe', 'Antilles', 16.2650, -61.5510),
    ('MARTINIQUE', 'Martinique', 'Antilles', 14.6415, -61.0242),
    ('GUYANE', 'Guyane', 'Amérique du Sud', 4.9224, -52.3135),
    ('REUNION', 'La Réunion', 'Océan Indien', -21.1151, 55.5364),
    ('MAYOTTE', 'Mayotte', 'Océan Indien', -12.8275, 45.1662),
    ('CORSE', 'Corse', 'Méditerranée', 42.0396, 9.0129),
    ('SAINT_PIERRE', 'Saint-Pierre-et-Miquelon', 'Atlantique Nord', 46.8852, -56.3159),
    ('NOUVELLE_CALEDONIE', 'Nouvelle-Calédonie', 'Pacifique', -20.9043, 165.6180),
    ('POLYNESIE', 'Polynésie française', 'Pacifique', -17.6797, -149.4068),
    ('WALLIS', 'Wallis-et-Futuna', 'Pacifique', -13.7688, -177.1561)
ON CONFLICT (code) DO NOTHING;

-- -----------------------------------------------------------------------------
-- Vue: Statistiques du dashboard
-- -----------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_dashboard_stats AS
SELECT
    COUNT(*) as total_resources,
    COUNT(*) FILTER (WHERE server_type = 'SERVER') as total_servers,
    COUNT(*) FILTER (WHERE server_type = 'NAS') as total_nas,
    COUNT(*) FILTER (WHERE server_type = 'DBAAS') as total_dbaas,
    COUNT(*) FILTER (WHERE active = true) as total_active,
    COUNT(*) FILTER (WHERE active = false) as total_inactive
FROM servers;

-- -----------------------------------------------------------------------------
-- Vue: Serveurs avec détails
-- -----------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_servers_detail AS
SELECT
    s.id,
    s.resource_name,
    s.hostname,
    s.ref_dat,
    s.server_type,
    s.ip_front,
    s.ip_admin,
    s.ip_ilo,
    s.vlan_front,
    s.vlan_admin,
    s.data_center,
    s.os,
    s.os_version,
    s.vcpu,
    s.ram_gb,
    s.db_instance,
    s.db_version,
    s.db_type,
    s.nas_type,
    s.storage_capacity_gb,
    s.last_admin_update,
    s.notes,
    s.active,
    s.created_at,
    s.updated_at,
    e.id as environment_id,
    e.code as environment_code,
    e.name as environment_name,
    e.color as environment_color,
    si.id as site_id,
    si.code as site_code,
    si.name as site_name,
    si.latitude as site_latitude,
    si.longitude as site_longitude
FROM servers s
LEFT JOIN environments e ON s.environment_id = e.id
LEFT JOIN sites si ON s.site_id = si.id;

-- -----------------------------------------------------------------------------
-- Fonction: Mise à jour automatique de updated_at
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers pour updated_at
-- Note: Utilisation de EXECUTE PROCEDURE pour compatibilité avec PostgreSQL < 11
DROP TRIGGER IF EXISTS update_servers_updated_at ON servers;
CREATE TRIGGER update_servers_updated_at
    BEFORE UPDATE ON servers
    FOR EACH ROW
    EXECUTE PROCEDURE update_updated_at_column();

DROP TRIGGER IF EXISTS update_environments_updated_at ON environments;
CREATE TRIGGER update_environments_updated_at
    BEFORE UPDATE ON environments
    FOR EACH ROW
    EXECUTE PROCEDURE update_updated_at_column();

DROP TRIGGER IF EXISTS update_sites_updated_at ON sites;
CREATE TRIGGER update_sites_updated_at
    BEFORE UPDATE ON sites
    FOR EACH ROW
    EXECUTE PROCEDURE update_updated_at_column();

DROP TRIGGER IF EXISTS update_users_updated_at ON users;
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE PROCEDURE update_updated_at_column();

-- -----------------------------------------------------------------------------
-- Fin du script
-- -----------------------------------------------------------------------------
