# Guide d'Installation

Ce document décrit les différentes méthodes d'installation de GED-PEI WebApp.

## Table des matières

- [Prérequis](#prérequis)
- [Installation avec Docker (Recommandé)](#installation-avec-docker-recommandé)
- [Installation manuelle](#installation-manuelle)
- [Configuration](#configuration)
- [Vérification de l'installation](#vérification-de-linstallation)
- [Dépannage](#dépannage)

## Prérequis

### Pour Docker (Production)

- Docker 20.10+
- Docker Compose 2.0+
- 4 Go de RAM minimum
- 10 Go d'espace disque

### Pour le développement

- Node.js 20+ et npm 10+
- Java 17 (JDK)
- Maven 3.9+
- PostgreSQL 15+ (ou utiliser H2 en mode dev)
- Git

## Installation avec Docker (Recommandé)

### 1. Cloner le projet

```bash
git clone <repository-url>
cd ged-pei-webapp
```

### 2. Configurer les variables d'environnement

```bash
# Copier le fichier exemple
cp .env.example .env

# Éditer le fichier .env
nano .env
```

Contenu du fichier `.env` :

```env
# Base de données
DB_PASSWORD=votre_mot_de_passe_securise

# JWT
JWT_SECRET=votre_cle_secrete_jwt_minimum_32_caracteres

# Ports
FRONTEND_PORT=80
BACKEND_PORT=8080

# Nom du projet Docker
COMPOSE_PROJECT_NAME=gedpei
```

> **Important** : Utilisez des mots de passe forts en production !

### 3. Lancer les conteneurs

```bash
# Mode production
docker-compose up -d

# Voir les logs
docker-compose logs -f

# Vérifier le statut
docker-compose ps
```

### 4. Accéder à l'application

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3080 |
| Backend API | http://localhost:8082 |
| Swagger UI | http://localhost:8082/swagger-ui.html |

## Installation manuelle

### Backend (Spring Boot)

#### 1. Configurer la base de données

**Option A : PostgreSQL**

```bash
# Créer la base de données
psql -U postgres
CREATE DATABASE gedpei;
CREATE USER gedpei_user WITH PASSWORD 'votre_mot_de_passe';
GRANT ALL PRIVILEGES ON DATABASE gedpei TO gedpei_user;
\q
```

**Option B : H2 (développement uniquement)**

Aucune configuration nécessaire, H2 est embarqué.

#### 2. Configurer le backend

Éditer `backend/src/main/resources/application.yml` :

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/gedpei
    username: gedpei_user
    password: votre_mot_de_passe
```

Ou utiliser le profil dev avec H2 :

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

#### 3. Compiler et lancer

```bash
cd backend

# Compiler
mvn clean package -DskipTests

# Lancer
java -jar target/ged-pei-1.0.0.jar
```

### Frontend (React)

#### 1. Installer les dépendances

```bash
cd frontend
npm install
```

#### 2. Configurer le proxy API

Éditer `vite.config.js` si le backend n'est pas sur le port par défaut :

```javascript
export default defineConfig({
  server: {
    port: 3002,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

#### 3. Lancer en développement

```bash
npm run dev
```

#### 4. Build de production

```bash
npm run build
```

Les fichiers de production seront dans `dist/`.

## Configuration

### Configuration du Backend

Le fichier `application.yml` supporte plusieurs profils :

| Profil | Description | Base de données |
|--------|-------------|-----------------|
| `default` | Production | PostgreSQL |
| `dev` | Développement | H2 (mémoire) |
| `prod` | Production optimisée | PostgreSQL + pool |

Activer un profil :

```bash
# Via variable d'environnement
export SPRING_PROFILES_ACTIVE=dev

# Via argument
java -jar app.jar --spring.profiles.active=dev
```

### Configuration JWT

```yaml
application:
  security:
    jwt:
      secret-key: ${JWT_SECRET:defaultSecretKey}
      expiration: 86400000  # 24 heures en millisecondes
```

### Configuration CORS

Par défaut, le backend accepte les requêtes de :
- `http://localhost:3000`
- `http://localhost:3002`
- `http://localhost:3080`

Pour ajouter des origines, modifier `SecurityConfig.java`.

## Vérification de l'installation

### 1. Vérifier le backend

```bash
# Health check
curl http://localhost:8080/actuator/health

# Réponse attendue
{"status":"UP"}
```

### 2. Vérifier le frontend

Ouvrir http://localhost:3080 dans un navigateur.

### 3. Vérifier la base de données

```bash
# Docker
docker-compose exec db psql -U gedpei -d gedpei -c "\dt"

# Local PostgreSQL
psql -U gedpei_user -d gedpei -c "\dt"
```

### 4. Tester l'authentification

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
```

## Dépannage

### Le backend ne démarre pas

**Erreur de connexion à la base de données**

```
Connection refused to host: localhost:5432
```

Solution : Vérifier que PostgreSQL est démarré ou utiliser le profil `dev` avec H2.

**Port déjà utilisé**

```
Port 8080 is already in use
```

Solution : Changer le port dans `application.yml` ou arrêter le processus existant.

### Le frontend ne se connecte pas au backend

**Erreur CORS**

Vérifier que l'origine du frontend est autorisée dans `SecurityConfig.java`.

**Erreur de proxy**

Vérifier la configuration du proxy dans `vite.config.js`.

### Problèmes Docker

**Conteneurs qui ne démarrent pas**

```bash
# Voir les logs détaillés
docker-compose logs backend
docker-compose logs frontend
docker-compose logs db

# Reconstruire les images
docker-compose build --no-cache
docker-compose up -d
```

**Problèmes de volume**

```bash
# Supprimer les volumes et recommencer
docker-compose down -v
docker-compose up -d
```

### Réinitialiser complètement

```bash
# Arrêter et supprimer tout
docker-compose down -v --rmi all

# Nettoyer Docker
docker system prune -a

# Recommencer
docker-compose up -d --build
```

## Mise à jour

### Avec Docker

```bash
git pull
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

### Installation manuelle

```bash
git pull

# Backend
cd backend
mvn clean package -DskipTests

# Frontend
cd ../frontend
npm install
npm run build
```
