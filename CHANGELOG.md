# Changelog

Toutes les modifications notables de ce projet sont documentees dans ce fichier.

Le format est base sur [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/),
et ce projet adhere au [Versionnement Semantique](https://semver.org/lang/fr/).

## [1.4.0] - 2026-01-23

### Ajoute

- **Systeme d'Alertes de Conformite** : Mecanisme automatique de detection des non-conformites

  **Objectif** : Identifier automatiquement les logiciels et serveurs necessitant une mise a jour ou presentant une incompatibilite de version.

  **Backend (Spring Boot)** :
  - `Alert.java` : Entite JPA avec enums AlertType, AlertSeverity, AlertStatus
  - `AlertRepository.java` : Repository avec methodes de filtrage et comptage
  - `AlertDTO.java` : DTO de transfert avec conversion entity vers DTO
  - `AlertService.java` : Service metier avec generation automatique des alertes
  - `AlertController.java` : API REST complete

  **Endpoints API** :
  - `GET /api/alerts` : Liste toutes les alertes
  - `GET /api/alerts/active` : Alertes actives uniquement
  - `GET /api/alerts/filter` : Filtrage avance (statut, severite, type)
  - `GET /api/alerts/stats` : Statistiques (compteurs par statut/severite)
  - `POST /api/alerts/generate` : Lancer l'analyse et generer les alertes
  - `PUT /api/alerts/{id}/acknowledge` : Prendre en compte une alerte
  - `PUT /api/alerts/{id}/resolve` : Marquer une alerte comme resolue
  - `PUT /api/alerts/{id}/ignore` : Ignorer une alerte

  **Types d'alertes generes** :
  - `VERSION_INTERDITE` : Logiciel avec version interdite (CRITICAL/HIGH)
  - `VERSION_OBSOLETE` : Logiciel avec version obsolete (HIGH)
  - `VERSION_RISQUEE` : Logiciel avec version risquee (MEDIUM)
  - `MISE_A_JOUR_REQUISE` : Mise a jour trajectoire preconisee (INFO)
  - `FIN_SUPPORT_PROCHE` : Version toleree avec expiration proche (LOW)

  **Mapping Severite** :
  - INTERDITE_CYBER → CRITICAL (rouge fonce)
  - INTERDITE → HIGH (rouge)
  - OBSOLETE → HIGH (rouge)
  - RISQUEE → MEDIUM (orange)
  - TOLEREE (expiration proche) → LOW (vert)
  - TRAJECTOIRE_PRECONISEE → INFO (bleu)

  **Frontend (React/Material-UI)** :
  - `Alerts.jsx` : Page complete de gestion des alertes
    - Cartes statistiques (Actives, Prises en compte, Resolues)
    - Filtres par statut, severite, type
    - Tableau des alertes avec actions (acknowledge, resolve, ignore)
    - Dialog de confirmation pour la generation

  **Integration Dashboard** :
  - Section "ALERTES DE CONFORMITE" ajoutee au tableau de bord
  - Badge affichant le nombre d'alertes actives
  - Tableau des 5 dernieres alertes actives
  - Lien direct vers la page des alertes
  - Coloration selon la severite (fond rouge pour CRITICAL)

### Modifie

- `Dashboard.jsx` : Ajout des alertStats, activeAlerts et section alertes
- `Layout.jsx` : Ajout du menu "Alertes" avec icone AlertsIcon rouge
- `App.jsx` : Ajout de la route `/alerts`
- `api.js` : Ajout de l'objet alertsAPI avec toutes les methodes
- `SecurityConfig.java` : Permissions pour `/api/alerts/**`

---

## [1.3.0] - 2026-01-22

### Ajoute

- **Obsolescence Prevobs PMT DISCOVR** : Import et visualisation des donnees d'obsolescence

  **Backend** :
  - `ServerSoftwareObsolescence.java` : Entite pour les donnees PMT DISCOVR
  - `ServerSoftwareObsolescenceRepository.java` : Repository avec filtres
  - `ServerSoftwareObsolescenceDTO.java` : DTO de transfert
  - `ServerSoftwareObsolescenceService.java` : Service avec import CSV et statistiques
  - `ServerSoftwareObsolescenceController.java` : API REST

  **Frontend** :
  - `ServerObsolescence.jsx` : Page avec filtres, stats et regroupement par serveur

  **Statuts d'obsolescence** :
  - PRECONISEE (vert)
  - TRAJECTOIRE_PRECONISEE (bleu)
  - TOLEREE (jaune)
  - RISQUEE (orange)
  - INTERDITE (rouge)
  - INTERDITE_CYBER (rouge fonce)
  - OBSOLETE (gris)

---

## [1.2.1] - 2026-01-22

### Corrige

- **Erreurs PostgreSQL bytea** : Resolution des incompatibilites de types

  **Probleme** :
  - Erreur : `la fonction lower(bytea) n'existe pas`
  - Erreur : `l'operateur n'existe pas : character varying ~~ bytea`
  - PostgreSQL traitait certaines colonnes comme `bytea` au lieu de `VARCHAR`

  **Solutions appliquees** :

  1. **ServerSoftwareObsolescence.java** :
     - Ajout de `columnDefinition = "VARCHAR(x)"` sur toutes les colonnes String
     - Exemple : `@Column(name = "server_name", columnDefinition = "VARCHAR(255)")`

  2. **ServerSoftwareObsolescenceRepository.java** :
     - Conversion des requetes JPQL en requetes natives SQL
     - Ajout de `CAST(... AS VARCHAR)` explicite
     - Ajout d'alias pour ORDER BY avec DISTINCT

     Avant :
     ```java
     @Query("SELECT DISTINCT s.environment FROM ServerSoftwareObsolescence s")
     List<String> findDistinctEnvironments();
     ```

     Apres :
     ```java
     @Query(value = "SELECT DISTINCT CAST(s.environment AS VARCHAR) AS env
            FROM server_software_obsolescence s
            WHERE s.environment IS NOT NULL ORDER BY env", nativeQuery = true)
     List<String> findDistinctEnvironments();
     ```

  3. **ServerSoftwareObsolescenceService.java** :
     - `getStats()` : Gestion des statuts String (requetes natives retournent des String)
     - `filter()` : Passage du statut comme String au lieu d'enum

---

## [1.2.0] - 2026-01-21

### Ajoute

- **Documentation Serveurs** : Nouvelle documentation complete de l'infrastructure
  - `docs/SERVEURS.md` : Inventaire des serveurs, NAS et DBaaS
  - Synthese des environnements (PROD, PREPROD)
  - Mapping des sites PEI et Data Centers

- **ImportService ameliore** : Preparation pour l'import multi-sections
  - Detection des sections DBaaS dans les fichiers CSV
  - Patterns de colonnes pour les bases de donnees

## [1.1.1] - 2026-01-21

### Corrige

- **Import CSV - Colonne Version** : Correction majeure du mapping des colonnes
  - Le champ "Version" utilise maintenant **uniquement** la colonne "Version Roadmap"
  - La colonne "Version Package" n'est plus utilisee pour le champ version (contient des liens)
  - Suppression du mapping incorrect de "Identifiant de la solution" vers version
  - Les URLs ne s'affichent plus dans la colonne Version

### Modifie

- **SoftwareVersionService.parseRecord()** : Simplification du traitement de la version
  - Lecture directe de "Version Roadmap" sans fallback sur "Version Package"
  - Valeur "N/A" si "Version Roadmap" est vide
- **SoftwareVersionService.detectColumnMapping()** : Suppression du mapping "Identifiant solution" → version

## [1.1.0] - 2026-01-21

### Ajoute

- **Gestion des liens utiles** : Nouvelle colonne `useful_links` dans la table `software_versions`
  - Stockage au format JSON : `[{"label": "...", "url": "..."}]`
  - Affichage sous forme d'icones cliquables dans le tableau
  - Tooltips avec le label du lien

- **Import CSV ameliore** pour les versions de logiciels
  - Detection automatique de l'encodage (UTF-8, Windows-1252)
  - Detection automatique du delimiteur (`;`, `,`, tabulation)
  - Support du format Sipedia CSR Detail des versions
  - Support du format Sipedia Catalogue des solutions
  - Extraction automatique des URLs depuis le champ "Liens utiles sur la version package"
  - Conversion des liens au format JSON

- **Documentation complete**
  - `docs/DATABASE.md` : Schema de la base de donnees
  - `docs/IMPORT_CSV.md` : Guide d'import CSV
  - `docs/API.md` : Documentation de l'API REST
  - `docs/ARCHITECTURE.md` : Architecture technique
  - `docs/INSTALL.md` : Guide d'installation
  - `docs/CONTRIBUTING.md` : Guide de contribution

### Modifie

- **SoftwareVersion entity** : Ajout du champ `usefulLinks`
- **SoftwareVersionDTO** : Ajout du champ `usefulLinks` avec mapping
- **SoftwareVersionService** :
  - Nouvelle methode `parseUsefulLinks()` pour parser les liens
  - Nouvelle methode `cleanVersionString()` pour nettoyer les versions
- **SoftwareVersions.jsx** : Nouvelle colonne "Liens" avec icones cliquables

### Corrige

- Encodage des caracteres speciaux lors de l'import CSV

## [1.0.0] - 2026-01-15

### Ajoute

- **Module Serveurs**
  - CRUD complet pour les serveurs, NAS et DBaaS
  - Filtrage par environnement, site et type
  - Import/Export CSV et Excel
  - Pagination et tri

- **Module Versions de Logiciels**
  - CRUD complet pour les versions
  - Filtrage par logiciel, version et statut
  - Import CSV basique
  - Statistiques par statut

- **Module Environnements**
  - Gestion des environnements (PROD, DEV, etc.)
  - Configuration des couleurs pour l'interface

- **Module Sites**
  - Gestion des sites geographiques
  - Coordonnees GPS pour la cartographie

- **Authentification**
  - JWT avec Spring Security
  - Roles USER et ADMIN
  - Gestion des sessions

- **Interface utilisateur**
  - Theme Material-UI personnalise
  - Responsive design
  - Tableau de bord avec statistiques
  - Navigation par sidebar

### Technique

- Backend : Spring Boot 3.2, Java 17
- Frontend : React 18, Vite, Material-UI 5
- Base de donnees : PostgreSQL 15 (H2 en dev)
- Securite : JWT, BCrypt

---

## Types de changements

- `Ajoute` pour les nouvelles fonctionnalites
- `Modifie` pour les changements dans les fonctionnalites existantes
- `Deprecie` pour les fonctionnalites qui seront supprimees prochainement
- `Supprime` pour les fonctionnalites supprimees
- `Corrige` pour les corrections de bugs
- `Securite` pour les vulnerabilites
