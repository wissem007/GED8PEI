# Documentation Serveurs GED-PEI

Ce document presente la synthese et la structure des serveurs de l'infrastructure GED-PEI.

## Table des matieres

- [Vue d'ensemble](#vue-densemble)
- [Types de ressources](#types-de-ressources)
- [Environnements](#environnements)
- [Sites et Data Centers](#sites-et-data-centers)
- [Inventaire des serveurs](#inventaire-des-serveurs)
- [Import des donnees](#import-des-donnees)

## Vue d'ensemble

L'infrastructure GED-PEI comprend :

| Type | Quantite | Description |
|------|----------|-------------|
| Serveurs Locaux | 4 | Serveurs physiques sur les sites PEI |
| Serveurs Centraux | 5 | VMs dans les Data Centers (PACY, NOE) |
| NAS | 3 | Stockage reseau |
| DBaaS | 2 | Bases de donnees Oracle 19c |

**Total : 14 ressources**

## Types de ressources

### 1. Serveurs (SERVER)

Serveurs physiques ou virtuels hebergeant les applications GED-PEI.

| Champ | Description |
|-------|-------------|
| resourceName | Nom de la ressource (ex: GUADELOUPE-GED-PEI) |
| hostname | FQDN du serveur |
| ipFront | Adresse IP frontale |
| ipAdmin | Adresse IP d'administration |
| ipIlo | Adresse IP de la carte ILO (serveurs physiques) |
| environment | Environnement (PROD, PREPROD) |
| site | Site PEI ou Data Center |

### 2. NAS (NAS)

Systemes de stockage reseau.

| Champ | Description |
|-------|-------------|
| resourceName | Nom du partage NAS |
| hostname | Serveur hebergeant le NAS |
| environment | Environnement (PROD NAS, PREPROD NAS) |

### 3. DBaaS (DBAAS)

Bases de donnees Oracle en mode service.

| Champ | Description |
|-------|-------------|
| resourceName | Reference DAT |
| dbInstance | Nom de l'instance (ex: B6APROD) |
| dbVersion | Version Oracle (19c) |
| dbType | Type : Oracle |
| hostname | Cluster DBaaS |
| notes | Configuration (gabarit, charset, backup) |

## Environnements

### Production (PROD)

| Code | Description | Data Center |
|------|-------------|-------------|
| PROD | Serveurs de production | PACY |
| PROD NAS | Stockage de production | PACY |

### Pre-Production (PREPROD)

| Code | Description | Data Center |
|------|-------------|-------------|
| PREPROD | Serveurs de pre-production | NOE |
| PREPROD NAS | Stockage pre-production | NOE |
| PREPROD NAS OLM | Stockage OLM pre-production | NOE |

## Sites et Data Centers

### Sites PEI (Serveurs Locaux)

| Site | Code | Localisation |
|------|------|--------------|
| GUADELOUPE | GUADELOUPE | Jarry |
| REUNION | REUNION | Port Est |
| CORSE | CORSE | Lucciana |
| MARTINIQUE | MARTINIQUE | Bellefontaine |

### Data Centers (Serveurs Centraux)

| Data Center | Code | Description |
|-------------|------|-------------|
| PACY | PCY | Production |
| NOE | NOE | Pre-production |

## Inventaire des serveurs

### Serveurs Locaux (Sites PEI)

| Resource Name | Hostname | IP Front | IP ILO | Site |
|---------------|----------|----------|--------|------|
| GUADELOUPE-GED-PEI | guadeloupe-ged-pei.adam.adroot.edf.fr | 10.30.108.66 | 10.30.108.67 | GUADELOUPE Jarry |
| REUNION-GED-PEI | reunion-ged-pei.adam.adroot.edf.fr | 10.30.58.71 | 10.30.58.70 | REUNION Port Est |
| CORSE-GED-PEI | corse-ged-pei.adam.adroot.edf.fr | 10.188.16.98 | 10.188.16.99 | CORSE Lucciana |
| MARTINIQUE-GED-PEI | martinique-ged-pei.adam.adroot.edf.fr | 10.30.20.195 | 10.30.20.194 | MARTINIQUE Bellefontaine |

### Serveurs Centraux - Production (PACY)

| Ref. DAT | Resource Name | Hostname | IP Front | IP Admin |
|----------|---------------|----------|----------|----------|
| Central_GED_PEI_8 | DCGYY9XP | dcgyy9xp.pcy.edf.fr | 10.130.116.103 | 10.131.25.246 |
| DIW_GED_PEI | DCGYY9Y9 | dcgyy9y9.adam.adroot.edf.fr | 10.130.116.104 | 10.131.25.247 |

### Serveurs Centraux - Pre-Production (NOE)

| Ref. DAT | Resource Name | Hostname | IP Front | IP Admin |
|----------|---------------|----------|----------|----------|
| Central_GED_PEI_PREPROD_SODA | DCGYY9YJ | dcgyy9yj.noe.edf.fr | 10.130.121.158 | 10.131.25.250 |
| LOCAL_GED_PEI_PREPROD_8 | DCGYY9YI | DCGYY9YI.ADAM.ADROOT.edf.fr | 10.130.121.157 | 10.131.25.249 |
| DIW_GED_PEI_PREPROD_8 | DCGYY9YH | DCGYY9YH.ADAM.ADROOT.edf.fr | 10.130.121.156 | 10.131.25.248 |

### NAS (Stockage)

| Environnement | Ref. DAT | Resource Name | Hostname |
|---------------|----------|---------------|----------|
| PROD | Central_NAS_GED_PEI_PROD_8 | fsm_ge014_nas-prod-new-ged | dcgyy9xp.pcy.edf.fr |
| PREPROD | Central_NAS_GED_PEI_PREPROD_8 | fsm_ge014_nas-preprod-ged | dcgyy9yj.noe.edf.fr |
| PREPROD OLM | - | fsm_ge014_nas-preprod-locged | DCGYY9YI.ADAM.ADROOT.edf.fr |

### DBaaS (Bases de donnees)

| Ref. DAT | Instance | Hostname | Version | Gabarit | Backup | Env | Site |
|----------|----------|----------|---------|---------|--------|-----|------|
| Central_BD_GED_PEI_PREPROD_8 | B6APPROD | B6APPROD.noe.edf.fr | 19c | S-2 (2 CPU, 2Go SGA) | 8_SEM | PREPROD | NOE |
| Central_BD_GED_PEI_PROD_8 | B6APROD | B6APROD.pcy.edf.fr | 19c | S-2 (2 CPU, 2Go SGA) | 8_SEM | PROD | PCY |

**Configuration DBaaS commune :**
- Character_set : AL32UTF8
- Cluster PREPROD : dbaas-std-noe.noe.edf.fr
- Cluster PROD : dbaas-prem-pcy.pcy.edf.fr

## Import des donnees

### Format CSV supporte

Le fichier CSV peut contenir plusieurs sections :

1. **Section Serveurs Locaux** : En-tetes avec "Resource name", "Hostname", "IP front", "Site PEI"
2. **Section Serveurs Centraux** : En-tetes avec "ENV", "ref. DAT", "Resource name", "Hostname", "IP front", "IP admin", "data Center"
3. **Section NAS** : Lignes avec "NAS" dans la colonne ENV
4. **Section DBaaS** : Apres le marqueur "info DBaaS", en-tetes avec "ref. DAT", "Instance", "version", "gabarit", "backup"

### API d'import

```http
POST /api/servers/import
Content-Type: multipart/form-data
Authorization: Bearer <token>

file: [fichier.csv ou fichier.xlsx]
```

### Reponse

```json
{
  "id": 1,
  "filename": "liste_serveurs.csv",
  "status": "COMPLETED",
  "totalRows": 14,
  "importedRows": 14,
  "skippedRows": 0,
  "errorRows": 0,
  "durationMs": 1234
}
```

## Schema de donnees

### Table `servers`

```sql
CREATE TABLE servers (
    id BIGSERIAL PRIMARY KEY,
    resource_name VARCHAR(255),
    hostname VARCHAR(255),
    ip_front VARCHAR(50),
    ip_admin VARCHAR(50),
    ip_ilo VARCHAR(50),
    ref_dat VARCHAR(100),
    server_type VARCHAR(20) NOT NULL,  -- SERVER, NAS, DBAAS
    environment_id BIGINT REFERENCES environments(id),
    site_id BIGINT REFERENCES sites(id),
    data_center VARCHAR(50),
    last_admin_update DATE,
    os VARCHAR(100),
    os_version VARCHAR(50),
    -- Champs DBaaS
    db_instance VARCHAR(100),
    db_version VARCHAR(50),
    db_type VARCHAR(50),
    -- Metadonnees
    notes TEXT,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);
```

## Coherence des donnees

### Verifications automatiques

1. **Unicite** : Combinaison (hostname, resourceName, ipFront) unique
2. **Type auto-detecte** :
   - "NAS" dans ENV ou resourceName → Type NAS
   - "DBaaS" ou "BD" dans refDat → Type DBAAS
   - Sinon → Type SERVER
3. **Environnement normalise** :
   - "PROD NAS" → PROD
   - "PREPROD NAS OLM" → PREPROD

### Points d'attention

- Les serveurs locaux ont une IP ILO pour l'acces IPMI
- Les serveurs centraux n'ont pas d'IP ILO (VMs)
- Les NAS n'ont pas d'IP propre, ils sont heberges sur un serveur
- Les DBaaS sont accessibles via le cluster DBaaS
