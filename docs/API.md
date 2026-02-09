# Documentation API

Documentation des endpoints REST de l'API GED-PEI.

## Table des matières

- [Informations générales](#informations-générales)
- [Authentification](#authentification)
- [Endpoints](#endpoints)
  - [Auth](#auth)
  - [Servers](#servers)
  - [Software Versions](#software-versions)
  - [Dashboard](#dashboard)
  - [Import](#import)
  - [Export](#export)
- [Codes d'erreur](#codes-derreur)
- [Exemples](#exemples)

## Informations générales

### Base URL

| Environnement | URL |
|---------------|-----|
| Développement | `http://localhost:8080/api` |
| Production | `http://localhost:8082/api` |

### Format des données

- **Content-Type** : `application/json`
- **Encodage** : UTF-8
- **Format de date** : ISO 8601 (`2024-01-15T10:30:00Z`)

### Documentation interactive

Swagger UI est disponible à l'adresse :
- Développement : http://localhost:8080/swagger-ui.html
- Production : http://localhost:8082/swagger-ui.html

## Authentification

L'API utilise l'authentification JWT (JSON Web Token).

### Obtenir un token

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password"
}
```

### Utiliser le token

Inclure le token dans le header `Authorization` de chaque requête :

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Durée de validité

Le token expire après **24 heures**. Un nouveau token doit être obtenu via `/api/auth/login`.

## Endpoints

### Auth

#### POST /api/auth/login

Authentifie un utilisateur et retourne un token JWT.

**Requête :**

```json
{
  "username": "string",
  "password": "string"
}
```

**Réponse (200) :**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "username": "admin",
  "roles": ["ROLE_ADMIN"]
}
```

**Erreurs :**

| Code | Description |
|------|-------------|
| 401 | Identifiants invalides |

---

### Servers

#### GET /api/servers

Récupère la liste de tous les serveurs.

**Headers :** `Authorization: Bearer <token>`

**Paramètres de requête :**

| Paramètre | Type | Description |
|-----------|------|-------------|
| `page` | integer | Numéro de page (défaut: 0) |
| `size` | integer | Taille de page (défaut: 20) |
| `sort` | string | Champ de tri (ex: `name,asc`) |
| `search` | string | Recherche par nom |
| `siteId` | long | Filtrer par site |
| `environmentId` | long | Filtrer par environnement |
| `serverTypeId` | long | Filtrer par type |

**Réponse (200) :**

```json
{
  "content": [
    {
      "id": 1,
      "name": "SRV-PROD-01",
      "hostname": "srv-prod-01.edf.fr",
      "ipAddress": "192.168.1.10",
      "operatingSystem": "RHEL 8",
      "site": {
        "id": 1,
        "name": "Paris"
      },
      "environment": {
        "id": 1,
        "name": "Production"
      },
      "serverType": {
        "id": 1,
        "name": "Linux"
      },
      "status": "ACTIVE",
      "createdAt": "2024-01-15T10:30:00Z",
      "updatedAt": "2024-01-15T10:30:00Z"
    }
  ],
  "totalElements": 100,
  "totalPages": 5,
  "number": 0,
  "size": 20
}
```

#### GET /api/servers/{id}

Récupère un serveur par son ID.

**Réponse (200) :**

```json
{
  "id": 1,
  "name": "SRV-PROD-01",
  "hostname": "srv-prod-01.edf.fr",
  "ipAddress": "192.168.1.10",
  "operatingSystem": "RHEL 8",
  "description": "Serveur de production principal",
  "site": {
    "id": 1,
    "name": "Paris"
  },
  "environment": {
    "id": 1,
    "name": "Production"
  },
  "serverType": {
    "id": 1,
    "name": "Linux"
  },
  "softwareVersions": [
    {
      "id": 1,
      "softwareName": "Apache",
      "version": "2.4.52"
    }
  ],
  "status": "ACTIVE",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

**Erreurs :**

| Code | Description |
|------|-------------|
| 404 | Serveur non trouvé |

#### POST /api/servers

Crée un nouveau serveur.

**Requête :**

```json
{
  "name": "SRV-PROD-02",
  "hostname": "srv-prod-02.edf.fr",
  "ipAddress": "192.168.1.11",
  "operatingSystem": "RHEL 8",
  "description": "Serveur de production secondaire",
  "siteId": 1,
  "environmentId": 1,
  "serverTypeId": 1
}
```

**Réponse (201) :**

```json
{
  "id": 2,
  "name": "SRV-PROD-02",
  ...
}
```

**Erreurs :**

| Code | Description |
|------|-------------|
| 400 | Données invalides |
| 409 | Serveur déjà existant |

#### PUT /api/servers/{id}

Met à jour un serveur existant.

**Requête :**

```json
{
  "name": "SRV-PROD-02-UPDATED",
  "hostname": "srv-prod-02.edf.fr",
  "ipAddress": "192.168.1.11",
  "operatingSystem": "RHEL 9",
  "description": "Serveur mis à jour",
  "siteId": 1,
  "environmentId": 1,
  "serverTypeId": 1
}
```

**Réponse (200) :** Serveur mis à jour

**Erreurs :**

| Code | Description |
|------|-------------|
| 404 | Serveur non trouvé |
| 400 | Données invalides |

#### DELETE /api/servers/{id}

Supprime un serveur.

**Réponse (204) :** Pas de contenu

**Erreurs :**

| Code | Description |
|------|-------------|
| 404 | Serveur non trouvé |

---

### Software Versions

#### GET /api/software-versions

Récupère toutes les versions logicielles.

**Réponse (200) :**

```json
[
  {
    "id": 1,
    "softwareName": "Apache",
    "version": "2.4.52",
    "serverId": 1,
    "serverName": "SRV-PROD-01",
    "installedAt": "2024-01-10T08:00:00Z"
  }
]
```

#### GET /api/software-versions/server/{serverId}

Récupère les versions logicielles d'un serveur.

#### POST /api/software-versions

Ajoute une version logicielle.

**Requête :**

```json
{
  "softwareName": "Nginx",
  "version": "1.24.0",
  "serverId": 1
}
```

#### PUT /api/software-versions/{id}

Met à jour une version logicielle.

#### DELETE /api/software-versions/{id}

Supprime une version logicielle.

---

### Dashboard

#### GET /api/dashboard/stats

Récupère les statistiques du dashboard.

**Réponse (200) :**

```json
{
  "totalServers": 150,
  "serversByType": {
    "Linux": 100,
    "Windows": 50
  },
  "serversByEnvironment": {
    "Production": 60,
    "Développement": 50,
    "Recette": 40
  },
  "serversBySite": {
    "Paris": 80,
    "Lyon": 70
  },
  "recentServers": [
    {
      "id": 1,
      "name": "SRV-PROD-01",
      "createdAt": "2024-01-15T10:30:00Z"
    }
  ],
  "softwareVersionStats": {
    "Apache": 45,
    "Nginx": 30,
    "Tomcat": 25
  }
}
```

#### GET /api/dashboard/metrics

Récupère les métriques détaillées.

---

### Import

#### POST /api/import/csv

Importe des serveurs depuis un fichier CSV.

**Headers :**
- `Content-Type: multipart/form-data`
- `Authorization: Bearer <token>`

**Paramètres :**

| Paramètre | Type | Description |
|-----------|------|-------------|
| `file` | file | Fichier CSV |

**Format CSV attendu :**

```csv
name,hostname,ipAddress,operatingSystem,site,environment,type
SRV-01,srv-01.edf.fr,192.168.1.1,RHEL 8,Paris,Production,Linux
```

**Réponse (200) :**

```json
{
  "success": true,
  "imported": 10,
  "errors": 2,
  "errorDetails": [
    {
      "line": 5,
      "message": "IP address already exists"
    }
  ]
}
```

#### POST /api/import/excel

Importe des serveurs depuis un fichier Excel (.xlsx).

**Format identique au CSV.**

#### GET /api/import/history

Récupère l'historique des imports.

**Réponse (200) :**

```json
[
  {
    "id": 1,
    "filename": "servers.csv",
    "importedAt": "2024-01-15T10:30:00Z",
    "totalRecords": 100,
    "successCount": 98,
    "errorCount": 2,
    "status": "COMPLETED"
  }
]
```

#### GET /api/import/template

Télécharge un template CSV/Excel.

---

### Export

#### GET /api/export/servers/csv

Exporte tous les serveurs en CSV.

**Paramètres :**

| Paramètre | Type | Description |
|-----------|------|-------------|
| `siteId` | long | Filtrer par site |
| `environmentId` | long | Filtrer par environnement |

**Réponse :** Fichier CSV en téléchargement

#### GET /api/export/servers/excel

Exporte tous les serveurs en Excel.

#### GET /api/export/software-versions/csv

Exporte les versions logicielles en CSV.

---

## Codes d'erreur

### Codes HTTP

| Code | Signification |
|------|---------------|
| 200 | Succès |
| 201 | Créé |
| 204 | Pas de contenu |
| 400 | Requête invalide |
| 401 | Non authentifié |
| 403 | Non autorisé |
| 404 | Ressource non trouvée |
| 409 | Conflit (doublon) |
| 500 | Erreur serveur |

### Format des erreurs

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Le champ 'name' est obligatoire",
  "path": "/api/servers"
}
```

## Exemples

### Exemple complet avec cURL

```bash
# 1. Authentification
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' | jq -r '.token')

# 2. Lister les serveurs
curl -X GET http://localhost:8080/api/servers \
  -H "Authorization: Bearer $TOKEN"

# 3. Créer un serveur
curl -X POST http://localhost:8080/api/servers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "SRV-NEW",
    "hostname": "srv-new.edf.fr",
    "ipAddress": "192.168.1.100",
    "operatingSystem": "RHEL 8",
    "siteId": 1,
    "environmentId": 1,
    "serverTypeId": 1
  }'

# 4. Importer un fichier CSV
curl -X POST http://localhost:8080/api/import/csv \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@servers.csv"

# 5. Exporter en CSV
curl -X GET http://localhost:8080/api/export/servers/csv \
  -H "Authorization: Bearer $TOKEN" \
  -o servers_export.csv
```

### Exemple avec JavaScript (Axios)

```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api'
});

// Authentification
const login = async (username, password) => {
  const response = await api.post('/auth/login', { username, password });
  api.defaults.headers.common['Authorization'] = `Bearer ${response.data.token}`;
  return response.data;
};

// Lister les serveurs
const getServers = async (page = 0, size = 20) => {
  const response = await api.get('/servers', { params: { page, size } });
  return response.data;
};

// Créer un serveur
const createServer = async (serverData) => {
  const response = await api.post('/servers', serverData);
  return response.data;
};
```
