# Architecture

Ce document décrit l'architecture technique de l'application GED-PEI WebApp.

## Table des matières

- [Vue d'ensemble](#vue-densemble)
- [Architecture Frontend](#architecture-frontend)
- [Architecture Backend](#architecture-backend)
- [Base de données](#base-de-données)
- [Sécurité](#sécurité)
- [Infrastructure Docker](#infrastructure-docker)
- [Flux de données](#flux-de-données)

## Vue d'ensemble

GED-PEI WebApp suit une architecture **3-tiers** classique avec une séparation claire entre :

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENTS                                  │
│                   (Navigateurs Web)                              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     FRONTEND (React)                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │   Pages     │  │ Components  │  │  Services   │              │
│  │  (Routes)   │  │    (UI)     │  │   (API)     │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
│                         Nginx (Port 80/3080)                     │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼ /api/*
┌─────────────────────────────────────────────────────────────────┐
│                   BACKEND (Spring Boot)                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │ Controllers │─▶│  Services   │─▶│Repositories │              │
│  │   (REST)    │  │  (Logic)    │  │   (JPA)     │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
│                    Port 8080/8082                                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    DATABASE (PostgreSQL)                         │
│                        Port 5432                                 │
└─────────────────────────────────────────────────────────────────┘
```

## Architecture Frontend

### Stack technique

| Technologie | Version | Rôle |
|-------------|---------|------|
| React | 18.2 | Framework UI |
| Vite | 5.0 | Build tool |
| Material-UI | 5.15 | Composants UI |
| React Router | 6.21 | Routage |
| Axios | 1.6 | Client HTTP |
| Chart.js | - | Graphiques |

### Structure des dossiers

```
frontend/src/
├── main.jsx              # Point d'entrée React
├── App.jsx               # Composant racine + routage
├── components/
│   └── Layout.jsx        # Layout principal avec navigation
├── pages/
│   ├── Dashboard.jsx     # Page tableau de bord
│   ├── Login.jsx         # Page de connexion
│   ├── ServerList.jsx    # Liste des serveurs
│   ├── ServerDetail.jsx  # Détail d'un serveur
│   ├── ImportPage.jsx    # Import de données
│   └── SoftwareVersions.jsx  # Gestion des versions
├── context/
│   └── AuthContext.jsx   # Gestion de l'authentification
└── services/
    └── api.js            # Configuration Axios
```

### Gestion d'état

L'application utilise **React Context** pour la gestion d'état globale :

```
┌─────────────────────────────────────────┐
│            AuthProvider                  │
│  ┌─────────────────────────────────┐    │
│  │  • user (utilisateur connecté)  │    │
│  │  • token (JWT)                  │    │
│  │  • login()                      │    │
│  │  • logout()                     │    │
│  │  • isAuthenticated              │    │
│  └─────────────────────────────────┘    │
└─────────────────────────────────────────┘
              │
              ▼
        ┌─────────┐
        │   App   │
        └─────────┘
              │
    ┌─────────┼─────────┐
    ▼         ▼         ▼
┌───────┐ ┌───────┐ ┌───────┐
│ Login │ │ Dash  │ │Server │
└───────┘ └───────┘ └───────┘
```

### Routage

| Route | Composant | Description |
|-------|-----------|-------------|
| `/login` | Login | Page de connexion |
| `/` | Dashboard | Tableau de bord |
| `/servers` | ServerList | Liste des serveurs |
| `/servers/:id` | ServerDetail | Détail serveur |
| `/import` | ImportPage | Import de données |
| `/software-versions` | SoftwareVersions | Versions logicielles |

### Communication API

```javascript
// services/api.js
const api = axios.create({
  baseURL: '/api'
});

// Intercepteur pour ajouter le token JWT
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

## Architecture Backend

### Stack technique

| Technologie | Version | Rôle |
|-------------|---------|------|
| Spring Boot | 3.2 | Framework |
| Java | 17 | Langage |
| Spring Security | 6.x | Sécurité |
| Spring Data JPA | 3.x | Persistence |
| Hibernate | 6.x | ORM |
| JJWT | 0.12.3 | Tokens JWT |
| MapStruct | - | Mapping DTO |

### Structure des packages

```
com.edf.gedpei/
├── GedPeiApplication.java    # Point d'entrée Spring Boot
├── config/
│   ├── SecurityConfig.java   # Configuration sécurité
│   └── GlobalExceptionHandler.java
├── controller/
│   ├── AuthController.java
│   ├── ServerController.java
│   ├── DashboardController.java
│   ├── ImportController.java
│   ├── ExportController.java
│   └── SoftwareVersionController.java
├── service/
│   ├── UserService.java
│   ├── ServerService.java
│   ├── DashboardService.java
│   ├── ImportService.java
│   └── SoftwareVersionService.java
├── entity/
│   ├── User.java
│   ├── Server.java
│   ├── Site.java
│   ├── Environment.java
│   ├── ServerType.java
│   ├── SoftwareVersion.java
│   └── ImportHistory.java
├── dto/
│   ├── AuthDTO.java
│   ├── ServerDTO.java
│   ├── ServerCreateDTO.java
│   └── ...
├── repository/
│   ├── UserRepository.java
│   ├── ServerRepository.java
│   └── ...
└── security/
    ├── JwtService.java
    └── JwtAuthenticationFilter.java
```

### Couches applicatives

```
┌─────────────────────────────────────────────────────────────┐
│                     Controllers                              │
│  • Réception des requêtes HTTP                              │
│  • Validation des entrées                                    │
│  • Conversion DTO ↔ Entity                                  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      Services                                │
│  • Logique métier                                           │
│  • Transactions                                              │
│  • Orchestration                                             │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Repositories                              │
│  • Accès aux données (Spring Data JPA)                      │
│  • Requêtes personnalisées                                   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      Entities                                │
│  • Mapping ORM (Hibernate)                                  │
│  • Relations JPA                                             │
└─────────────────────────────────────────────────────────────┘
```

### Pattern DTO

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Client     │────▶│     DTO      │────▶│   Controller │
│  (Request)   │     │ (Validation) │     │              │
└──────────────┘     └──────────────┘     └──────────────┘
                                                 │
                                                 ▼
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Client     │◀────│     DTO      │◀────│   Service    │
│  (Response)  │     │  (Response)  │     │              │
└──────────────┘     └──────────────┘     └──────────────┘
```

## Base de données

### Modèle de données

```
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│    Site     │      │ Environment │      │ ServerType  │
├─────────────┤      ├─────────────┤      ├─────────────┤
│ id          │      │ id          │      │ id          │
│ name        │      │ name        │      │ name        │
│ code        │      │ code        │      │ code        │
└──────┬──────┘      └──────┬──────┘      └──────┬──────┘
       │                    │                    │
       └────────────────────┼────────────────────┘
                            │
                            ▼
                    ┌───────────────┐
                    │    Server     │
                    ├───────────────┤
                    │ id            │
                    │ name          │
                    │ hostname      │
                    │ ipAddress     │
                    │ operatingSystem│
                    │ description   │
                    │ status        │
                    │ site_id (FK)  │
                    │ environment_id│
                    │ server_type_id│
                    │ created_at    │
                    │ updated_at    │
                    └───────┬───────┘
                            │
                            ▼
                ┌───────────────────────┐
                │   SoftwareVersion     │
                ├───────────────────────┤
                │ id                    │
                │ software_name         │
                │ version               │
                │ server_id (FK)        │
                │ installed_at          │
                └───────────────────────┘

┌─────────────────┐              ┌─────────────────┐
│      User       │              │  ImportHistory  │
├─────────────────┤              ├─────────────────┤
│ id              │              │ id              │
│ username        │              │ filename        │
│ password (hash) │              │ imported_at     │
│ email           │              │ total_records   │
│ roles           │              │ success_count   │
│ enabled         │              │ error_count     │
└─────────────────┘              │ status          │
                                 └─────────────────┘
```

### Relations

| Relation | Type | Description |
|----------|------|-------------|
| Server → Site | Many-to-One | Un serveur appartient à un site |
| Server → Environment | Many-to-One | Un serveur est dans un environnement |
| Server → ServerType | Many-to-One | Un serveur a un type |
| Server → SoftwareVersion | One-to-Many | Un serveur a plusieurs versions logicielles |

## Sécurité

### Flux d'authentification JWT

```
┌────────┐                    ┌────────┐                    ┌────────┐
│ Client │                    │Backend │                    │  DB    │
└───┬────┘                    └───┬────┘                    └───┬────┘
    │                             │                             │
    │  POST /api/auth/login       │                             │
    │  {username, password}       │                             │
    │────────────────────────────▶│                             │
    │                             │  Verify credentials         │
    │                             │────────────────────────────▶│
    │                             │◀────────────────────────────│
    │                             │                             │
    │                             │  Generate JWT               │
    │                             │                             │
    │  {token: "eyJhbG..."}       │                             │
    │◀────────────────────────────│                             │
    │                             │                             │
    │  GET /api/servers           │                             │
    │  Authorization: Bearer xxx  │                             │
    │────────────────────────────▶│                             │
    │                             │  Validate JWT               │
    │                             │  Extract user               │
    │                             │────────────────────────────▶│
    │                             │◀────────────────────────────│
    │  {servers: [...]}           │                             │
    │◀────────────────────────────│                             │
```

### Configuration sécurité

```java
// SecurityConfig.java
@Configuration
public class SecurityConfig {

    // Endpoints publics
    String[] publicEndpoints = {
        "/api/auth/**",
        "/actuator/health",
        "/swagger-ui/**",
        "/v3/api-docs/**"
    };

    // CORS autorisés
    String[] allowedOrigins = {
        "http://localhost:3000",
        "http://localhost:3002",
        "http://localhost:3080"
    };
}
```

### Stockage des mots de passe

- Algorithme : **BCrypt**
- Salt : Généré automatiquement
- Rounds : 10 (par défaut)

## Infrastructure Docker

### Services

```yaml
services:
  frontend:
    build: ./frontend
    ports: 3080:80
    depends_on: backend

  backend:
    build: ./backend
    ports: 8082:8080
    depends_on: db
    environment:
      - SPRING_DATASOURCE_URL
      - JWT_SECRET

  db:
    image: postgres:15-alpine
    ports: 5432:5432
    volumes:
      - postgres-data:/var/lib/postgresql/data
```

### Réseau

```
┌─────────────────────────────────────────────────────────────┐
│                    gedpei-network                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
│  │  frontend   │  │   backend   │  │     db      │          │
│  │  (nginx)    │──│  (spring)   │──│ (postgres)  │          │
│  │  :80        │  │  :8080      │  │  :5432      │          │
│  └─────────────┘  └─────────────┘  └─────────────┘          │
└─────────────────────────────────────────────────────────────┘
        │                   │
        ▼                   ▼
    Port 3080           Port 8082
    (externe)           (externe)
```

### Volumes

| Volume | Usage |
|--------|-------|
| `postgres-data` | Données PostgreSQL |
| `backend-uploads` | Fichiers importés |
| `backend-exports` | Fichiers exportés |
| `backend-logs` | Logs applicatifs |

## Flux de données

### Import CSV/Excel

```
┌────────┐     ┌────────┐     ┌────────┐     ┌────────┐
│  User  │────▶│Frontend│────▶│Backend │────▶│   DB   │
│        │     │        │     │        │     │        │
│ Upload │     │Dropzone│     │ Parse  │     │ Insert │
│  file  │     │  API   │     │Validate│     │ Batch  │
└────────┘     └────────┘     └────────┘     └────────┘
                                  │
                                  ▼
                          ┌──────────────┐
                          │ImportHistory │
                          │   (log)      │
                          └──────────────┘
```

### Export CSV/Excel

```
┌────────┐     ┌────────┐     ┌────────┐     ┌────────┐
│  User  │◀────│Frontend│◀────│Backend │◀────│   DB   │
│        │     │        │     │        │     │        │
│Download│     │  Blob  │     │Generate│     │ Query  │
│  file  │     │Download│     │  File  │     │  All   │
└────────┘     └────────┘     └────────┘     └────────┘
```

### Dashboard Metrics

```
┌────────┐     ┌────────┐     ┌────────────────┐     ┌────────┐
│  User  │◀────│Frontend│◀────│    Backend     │◀────│   DB   │
│        │     │        │     │                │     │        │
│ View   │     │Chart.js│     │ Aggregate      │     │ COUNT  │
│ Charts │     │  MUI   │     │ Statistics     │     │ GROUP  │
└────────┘     └────────┘     └────────────────┘     └────────┘
```
