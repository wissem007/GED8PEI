# Documentation Base de Donnees

Ce document decrit le schema de la base de donnees PostgreSQL pour l'application GED-PEI.

## Table des matieres

- [Vue d'ensemble](#vue-densemble)
- [Tables](#tables)
- [Vues](#vues)
- [Fonctions et Triggers](#fonctions-et-triggers)
- [Donnees initiales](#donnees-initiales)

## Vue d'ensemble

Le schema comprend les tables suivantes :

| Table | Description |
|-------|-------------|
| `environments` | Environnements (PROD, PREPROD, DEV, etc.) |
| `sites` | Sites geographiques (Guadeloupe, Martinique, etc.) |
| `servers` | Serveurs, NAS et bases de donnees |
| `users` | Utilisateurs de l'application |
| `import_history` | Historique des imports CSV/Excel |
| `software_versions` | Versions de logiciels avec statut |

## Tables

### environments

Table des environnements d'execution.

```sql
CREATE TABLE environments (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    color VARCHAR(20) DEFAULT '#1976d2',
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

| Colonne | Type | Description |
|---------|------|-------------|
| `id` | BIGSERIAL | Identifiant unique |
| `code` | VARCHAR(50) | Code unique (PROD, DEV, etc.) |
| `name` | VARCHAR(100) | Nom affiche |
| `description` | VARCHAR(500) | Description |
| `color` | VARCHAR(20) | Couleur hexadecimale pour l'interface |
| `display_order` | INTEGER | Ordre d'affichage |

### sites

Table des sites geographiques.

```sql
CREATE TABLE sites (
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
```

| Colonne | Type | Description |
|---------|------|-------------|
| `id` | BIGSERIAL | Identifiant unique |
| `code` | VARCHAR(50) | Code unique du site |
| `name` | VARCHAR(100) | Nom du site |
| `region` | VARCHAR(100) | Region geographique |
| `country` | VARCHAR(100) | Pays (defaut: France) |
| `latitude` | DECIMAL(10,8) | Coordonnee GPS |
| `longitude` | DECIMAL(11,8) | Coordonnee GPS |

### servers

Table principale des ressources (serveurs, NAS, DBaaS).

```sql
CREATE TABLE servers (
    id BIGSERIAL PRIMARY KEY,

    -- Identification
    resource_name VARCHAR(255),
    hostname VARCHAR(255),
    ref_dat VARCHAR(100),

    -- Type de ressource
    server_type VARCHAR(50) NOT NULL DEFAULT 'SERVER',

    -- Reseau
    ip_front VARCHAR(50),
    ip_admin VARCHAR(50),
    ip_ilo VARCHAR(50),
    vlan_front VARCHAR(50),
    vlan_admin VARCHAR(50),

    -- Localisation
    environment_id BIGINT REFERENCES environments(id),
    site_id BIGINT REFERENCES sites(id),
    data_center VARCHAR(100),

    -- Systeme
    os VARCHAR(100),
    os_version VARCHAR(100),
    vcpu INTEGER,
    ram_gb INTEGER,

    -- DBaaS specifique
    db_instance VARCHAR(255),
    db_version VARCHAR(100),
    db_type VARCHAR(50),

    -- NAS specifique
    nas_type VARCHAR(100),
    storage_capacity_gb INTEGER,

    -- Metadonnees
    last_admin_update DATE,
    notes TEXT,
    active BOOLEAN DEFAULT TRUE,

    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),

    CONSTRAINT chk_server_type CHECK (server_type IN ('SERVER', 'NAS', 'DBAAS'))
);
```

**Types de ressources** :
- `SERVER` : Serveur physique ou virtuel
- `NAS` : Stockage reseau
- `DBAAS` : Base de donnees as a Service

**Index** :
- `idx_servers_hostname` : Recherche par hostname
- `idx_servers_ip_front` : Recherche par IP
- `idx_servers_resource_name` : Recherche par nom
- `idx_servers_environment` : Filtre par environnement
- `idx_servers_site` : Filtre par site
- `idx_servers_type` : Filtre par type

### users

Table des utilisateurs.

```sql
CREATE TABLE users (
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
```

**Roles disponibles** : `USER`, `ADMIN`

### import_history

Historique des imports de fichiers.

```sql
CREATE TABLE import_history (
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

    CONSTRAINT chk_import_status CHECK (status IN (
        'PENDING', 'IN_PROGRESS', 'COMPLETED',
        'COMPLETED_WITH_ERRORS', 'FAILED'
    ))
);
```

**Statuts d'import** :
- `PENDING` : En attente
- `IN_PROGRESS` : En cours
- `COMPLETED` : Termine avec succes
- `COMPLETED_WITH_ERRORS` : Termine avec erreurs
- `FAILED` : Echec

### software_versions

Table des versions de logiciels avec leur statut.

```sql
CREATE TABLE software_versions (
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
    CONSTRAINT chk_version_status CHECK (status IN (
        'PRECONISEE', 'TRAJECTOIRE_PRECONISEE',
        'TOLEREE', 'INTERDITE', 'INTERDITE_CYBER'
    ))
);
```

**Statuts des versions** :

| Statut | Description | Couleur UI |
|--------|-------------|------------|
| `PRECONISEE` | Version recommandee | Vert |
| `TRAJECTOIRE_PRECONISEE` | Version future recommandee | Bleu |
| `TOLEREE` | Version acceptee | Orange |
| `INTERDITE` | Version interdite | Rouge |
| `INTERDITE_CYBER` | Version interdite (securite) | Rouge fonce |

**Format des liens utiles** (`useful_links`) :
```json
[
  {"label": "Documentation", "url": "https://..."},
  {"label": "Telechargement", "url": "https://..."}
]
```

## Vues

### v_dashboard_stats

Statistiques agregees pour le tableau de bord.

```sql
CREATE VIEW v_dashboard_stats AS
SELECT
    COUNT(*) as total_resources,
    COUNT(*) FILTER (WHERE server_type = 'SERVER') as total_servers,
    COUNT(*) FILTER (WHERE server_type = 'NAS') as total_nas,
    COUNT(*) FILTER (WHERE server_type = 'DBAAS') as total_dbaas,
    COUNT(*) FILTER (WHERE active = true) as total_active,
    COUNT(*) FILTER (WHERE active = false) as total_inactive
FROM servers;
```

### v_servers_detail

Vue denormalisee des serveurs avec leurs environnements et sites.

```sql
CREATE VIEW v_servers_detail AS
SELECT
    s.*,
    e.code as environment_code,
    e.name as environment_name,
    e.color as environment_color,
    si.code as site_code,
    si.name as site_name,
    si.latitude as site_latitude,
    si.longitude as site_longitude
FROM servers s
LEFT JOIN environments e ON s.environment_id = e.id
LEFT JOIN sites si ON s.site_id = si.id;
```

## Fonctions et Triggers

### update_updated_at_column()

Fonction de mise a jour automatique du champ `updated_at`.

```sql
CREATE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';
```

**Triggers associes** :
- `update_servers_updated_at`
- `update_environments_updated_at`
- `update_sites_updated_at`
- `update_users_updated_at`
- `update_software_versions_updated_at`

## Donnees initiales

### Environnements par defaut

| Code | Nom | Couleur |
|------|-----|---------|
| PROD | Production | #d32f2f (rouge) |
| PREPROD | Pre-production | #f57c00 (orange) |
| RECETTE | Recette | #1976d2 (bleu) |
| DEV | Developpement | #388e3c (vert) |
| INT | Integration | #7b1fa2 (violet) |
| QUALIF | Qualification | #0097a7 (cyan) |

### Sites par defaut

| Code | Nom | Region |
|------|-----|--------|
| GUADELOUPE | Guadeloupe | Antilles |
| MARTINIQUE | Martinique | Antilles |
| GUYANE | Guyane | Amerique du Sud |
| REUNION | La Reunion | Ocean Indien |
| MAYOTTE | Mayotte | Ocean Indien |
| CORSE | Corse | Mediterranee |
| SAINT_PIERRE | Saint-Pierre-et-Miquelon | Atlantique Nord |
| NOUVELLE_CALEDONIE | Nouvelle-Caledonie | Pacifique |
| POLYNESIE | Polynesie francaise | Pacifique |
| WALLIS | Wallis-et-Futuna | Pacifique |

## Migration et mise a jour

Pour ajouter une nouvelle colonne :

```sql
-- Exemple: ajout d'une colonne
ALTER TABLE servers ADD COLUMN new_column VARCHAR(100);

-- Mise a jour du schema existant
ALTER TABLE software_versions ALTER COLUMN notes TYPE TEXT;
```

## Sauvegarde et restauration

```bash
# Sauvegarde
pg_dump -U gedpei -d gedpei -f backup.sql

# Restauration
psql -U gedpei -d gedpei -f backup.sql
```
