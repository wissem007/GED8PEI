# GED-PEI WebApp

Application web de gestion des serveurs GED-PEI (Gestion Electronique de Documents - Plan d'Excellence Industrielle) pour EDF.

## Description

GED-PEI WebApp est une application full-stack permettant de gérer et superviser les serveurs Linux et Windows dans le cadre du programme PEI d'EDF. Elle offre une interface moderne pour le suivi des serveurs, la gestion des versions logicielles et l'import/export de données.

## Fonctionnalités

- **Gestion des serveurs** : CRUD complet pour les serveurs Linux et Windows
- **Dashboard** : Vue d'ensemble avec métriques et statistiques en temps réel
- **Suivi des versions** : Gestion des versions logicielles installées
- **Import de données** : Import CSV/Excel avec validation
- **Export de données** : Export des données serveurs et versions
- **Authentification** : Système JWT sécurisé
- **Gestion des sites** : Organisation par sites et environnements

## Stack Technique

| Composant | Technologies | Version |
|-----------|-------------|---------|
| Frontend | React, Vite, Material-UI | 18.2, 5.0, 5.15 |
| Backend | Spring Boot, Java | 3.2, 17 |
| Base de données | PostgreSQL | 15 |
| Conteneurisation | Docker, Docker Compose | 3.8 |
| Serveur Web | Nginx | Alpine |

## Prérequis

- Docker et Docker Compose
- Node.js 20+ (pour le développement frontend)
- Java 17+ et Maven 3.9+ (pour le développement backend)
- Git

## Installation Rapide

```bash
# Cloner le projet
git clone <repository-url>
cd ged-pei-webapp

# Copier et configurer les variables d'environnement
cp .env.example .env

# Lancer avec Docker Compose
docker-compose up -d
```

L'application sera accessible sur :
- **Frontend** : http://localhost:3080
- **Backend API** : http://localhost:8082
- **Swagger UI** : http://localhost:8082/swagger-ui.html

## Documentation

- [Installation détaillée](docs/INSTALL.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Documentation API](docs/API.md)
- [Import CSV Versions](docs/IMPORT_CSV.md)
- [Inventaire Serveurs](docs/SERVEURS.md)
- [Schéma Base de données](docs/DATABASE.md)
- [Guide de contribution](docs/CONTRIBUTING.md)
- [Changelog](CHANGELOG.md)

## Structure du Projet

```
ged-pei-webapp/
├── frontend/                 # Application React
│   ├── src/
│   │   ├── components/      # Composants réutilisables
│   │   ├── pages/           # Pages de l'application
│   │   ├── services/        # Services API
│   │   └── context/         # Contextes React
│   ├── package.json
│   └── Dockerfile
├── backend/                  # Application Spring Boot
│   ├── src/main/java/
│   │   └── com/edf/gedpei/
│   │       ├── controller/  # Endpoints REST
│   │       ├── service/     # Logique métier
│   │       ├── entity/      # Entités JPA
│   │       ├── dto/         # Objets de transfert
│   │       ├── repository/  # Repositories JPA
│   │       └── security/    # Configuration JWT
│   ├── pom.xml
│   └── Dockerfile
├── docs/                     # Documentation
├── docker-compose.yml        # Configuration production
├── docker-compose.dev.yml    # Configuration développement
└── .env.example              # Variables d'environnement
```

## Développement

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Le serveur de développement sera accessible sur http://localhost:3002

### Backend

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Le backend sera accessible sur http://localhost:8080 avec la base H2 en mémoire.

## Variables d'Environnement

| Variable | Description | Valeur par défaut |
|----------|-------------|-------------------|
| `DB_PASSWORD` | Mot de passe PostgreSQL | - |
| `JWT_SECRET` | Clé secrète JWT | - |
| `FRONTEND_PORT` | Port du frontend | 80 |
| `BACKEND_PORT` | Port du backend | 8080 |

## Licence

Propriétaire - EDF

## Contact

Pour toute question, contactez l'équipe de développement GED-PEI.
