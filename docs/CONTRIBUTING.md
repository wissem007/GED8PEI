# Guide de Contribution

Ce document décrit les conventions et procédures pour contribuer au projet GED-PEI WebApp.

## Table des matières

- [Environnement de développement](#environnement-de-développement)
- [Conventions de code](#conventions-de-code)
- [Workflow Git](#workflow-git)
- [Tests](#tests)
- [Revue de code](#revue-de-code)

## Environnement de développement

### Prérequis

- **Node.js** 20+ et npm 10+
- **Java** 17 (JDK)
- **Maven** 3.9+
- **Git**
- **IDE recommandé** : VS Code (frontend) + IntelliJ IDEA (backend)

### Configuration initiale

```bash
# Cloner le projet
git clone <repository-url>
cd ged-pei-webapp

# Configuration du backend
cd backend
mvn clean install

# Configuration du frontend
cd ../frontend
npm install
```

### Lancer l'environnement de développement

**Terminal 1 - Backend :**

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Terminal 2 - Frontend :**

```bash
cd frontend
npm run dev
```

### URLs de développement

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3002 |
| Backend | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| H2 Console | http://localhost:8080/h2-console |

### Configuration IDE

#### VS Code (Frontend)

Extensions recommandées :

```json
{
  "recommendations": [
    "dbaeumer.vscode-eslint",
    "esbenp.prettier-vscode",
    "dsznajder.es7-react-js-snippets",
    "formulahendry.auto-rename-tag"
  ]
}
```

#### IntelliJ IDEA (Backend)

- Activer l'annotation processing pour Lombok
- Configurer le SDK Java 17
- Installer le plugin Spring Boot

## Conventions de code

### Frontend (React/JavaScript)

#### Structure des fichiers

```
src/
├── components/          # Composants réutilisables
│   └── ComponentName/
│       ├── index.jsx
│       └── ComponentName.styles.js
├── pages/              # Pages/routes
│   └── PageName.jsx
├── services/           # Services API
│   └── serviceName.js
├── context/            # Contextes React
│   └── ContextName.jsx
├── hooks/              # Hooks personnalisés
│   └── useHookName.js
└── utils/              # Utilitaires
    └── utilName.js
```

#### Conventions de nommage

| Type | Convention | Exemple |
|------|------------|---------|
| Composants | PascalCase | `ServerList.jsx` |
| Hooks | camelCase avec "use" | `useAuth.js` |
| Services | camelCase | `serverService.js` |
| Constantes | UPPER_SNAKE_CASE | `API_BASE_URL` |
| Variables/fonctions | camelCase | `getServerById` |

#### Style de code

```javascript
// Utiliser des fonctions fléchées pour les composants
const ServerCard = ({ server }) => {
  return (
    <Card>
      <CardContent>
        <Typography>{server.name}</Typography>
      </CardContent>
    </Card>
  );
};

// Destructurer les props
const ServerList = ({ servers, onSelect, isLoading }) => {
  // ...
};

// Utiliser les hooks au début du composant
const Dashboard = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const { user } = useAuth();

  useEffect(() => {
    // ...
  }, []);

  return (/* ... */);
};
```

### Backend (Java/Spring Boot)

#### Structure des packages

```
com.edf.gedpei/
├── config/         # Configuration Spring
├── controller/     # Contrôleurs REST
├── service/        # Services métier
├── repository/     # Repositories JPA
├── entity/         # Entités JPA
├── dto/            # Data Transfer Objects
├── security/       # Composants sécurité
├── exception/      # Exceptions personnalisées
└── util/           # Utilitaires
```

#### Conventions de nommage

| Type | Convention | Exemple |
|------|------------|---------|
| Classes | PascalCase | `ServerService` |
| Méthodes | camelCase | `findByName()` |
| Constantes | UPPER_SNAKE_CASE | `MAX_PAGE_SIZE` |
| Packages | lowercase | `com.edf.gedpei.service` |

#### Style de code

```java
// Controller
@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
public class ServerController {

    private final ServerService serverService;

    @GetMapping
    public ResponseEntity<Page<ServerDTO>> getAll(Pageable pageable) {
        return ResponseEntity.ok(serverService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServerDTO> getById(@PathVariable Long id) {
        return serverService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}

// Service
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServerService {

    private final ServerRepository serverRepository;

    public Page<ServerDTO> findAll(Pageable pageable) {
        return serverRepository.findAll(pageable)
            .map(this::toDTO);
    }

    @Transactional
    public ServerDTO create(ServerCreateDTO dto) {
        // ...
    }
}
```

### Commentaires et documentation

```java
/**
 * Service de gestion des serveurs.
 * Gère les opérations CRUD et la logique métier associée.
 */
@Service
public class ServerService {

    /**
     * Recherche un serveur par son ID.
     *
     * @param id Identifiant du serveur
     * @return Le serveur trouvé ou Optional.empty()
     */
    public Optional<ServerDTO> findById(Long id) {
        // ...
    }
}
```

## Workflow Git

### Branches

| Branche | Description |
|---------|-------------|
| `main` | Production stable |
| `develop` | Développement |
| `feature/*` | Nouvelles fonctionnalités |
| `bugfix/*` | Corrections de bugs |
| `hotfix/*` | Corrections urgentes production |

### Workflow de développement

```bash
# 1. Créer une branche feature depuis develop
git checkout develop
git pull origin develop
git checkout -b feature/nom-de-la-feature

# 2. Développer et commiter
git add .
git commit -m "feat: description de la fonctionnalité"

# 3. Pousser et créer une Pull Request
git push origin feature/nom-de-la-feature
```

### Conventions de commit

Format : `<type>(<scope>): <description>`

| Type | Description |
|------|-------------|
| `feat` | Nouvelle fonctionnalité |
| `fix` | Correction de bug |
| `docs` | Documentation |
| `style` | Formatage (pas de changement de code) |
| `refactor` | Refactoring |
| `test` | Ajout/modification de tests |
| `chore` | Maintenance (build, CI, etc.) |

Exemples :

```bash
git commit -m "feat(server): ajouter la pagination sur la liste"
git commit -m "fix(auth): corriger l'expiration du token JWT"
git commit -m "docs(api): mettre à jour la documentation Swagger"
git commit -m "refactor(service): extraire la logique de validation"
```

## Tests

### Frontend

```bash
# Lancer les tests
npm test

# Lancer avec couverture
npm run test:coverage

# Mode watch
npm run test:watch
```

Structure des tests :

```
src/
├── components/
│   └── ServerCard/
│       ├── ServerCard.jsx
│       └── ServerCard.test.jsx
└── pages/
    └── ServerList.test.jsx
```

### Backend

```bash
# Lancer tous les tests
mvn test

# Lancer avec couverture (JaCoCo)
mvn verify

# Lancer un test spécifique
mvn test -Dtest=ServerServiceTest
```

Structure des tests :

```
src/test/java/com/edf/gedpei/
├── controller/
│   └── ServerControllerTest.java
├── service/
│   └── ServerServiceTest.java
└── repository/
    └── ServerRepositoryTest.java
```

Exemple de test :

```java
@SpringBootTest
@AutoConfigureMockMvc
class ServerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnAllServers() throws Exception {
        mockMvc.perform(get("/api/servers")
                .header("Authorization", "Bearer " + getToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }
}
```

## Revue de code

### Checklist de revue

#### Fonctionnel

- [ ] Le code répond aux exigences
- [ ] Les cas limites sont gérés
- [ ] Les erreurs sont correctement gérées

#### Qualité

- [ ] Le code est lisible et maintenable
- [ ] Les conventions sont respectées
- [ ] Pas de duplication de code
- [ ] Les noms sont explicites

#### Sécurité

- [ ] Pas de données sensibles en dur
- [ ] Les entrées sont validées
- [ ] Les autorisations sont vérifiées

#### Performance

- [ ] Pas de requêtes N+1
- [ ] Pagination implémentée si nécessaire
- [ ] Pas de chargement inutile

#### Tests

- [ ] Tests unitaires ajoutés/mis à jour
- [ ] Couverture de code suffisante
- [ ] Tests passent en CI

### Process de Pull Request

1. Créer la PR avec une description claire
2. Assigner un reviewer
3. Répondre aux commentaires
4. Obtenir l'approbation
5. Merger dans develop

## Déploiement

### Build de production

```bash
# Frontend
cd frontend
npm run build

# Backend
cd backend
mvn clean package -DskipTests
```

### Docker

```bash
# Build des images
docker-compose build

# Déployer
docker-compose up -d
```

## Ressources

- [Documentation React](https://react.dev/)
- [Documentation Spring Boot](https://spring.io/projects/spring-boot)
- [Material-UI](https://mui.com/)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
