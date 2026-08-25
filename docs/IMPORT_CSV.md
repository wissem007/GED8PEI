# Documentation Import CSV - Versions de Logiciels

Ce document decrit le fonctionnement de l'import CSV pour les versions de logiciels dans l'application GED-PEI.

## Table des matieres

- [Vue d'ensemble](#vue-densemble)
- [Formats CSV supportes](#formats-csv-supportes)
- [Detection automatique](#detection-automatique)
- [Colonnes reconnues](#colonnes-reconnues)
- [Traitement des donnees](#traitement-des-donnees)
- [Gestion des liens utiles](#gestion-des-liens-utiles)
- [Statuts des versions](#statuts-des-versions)
- [Resultat de l'import](#resultat-de-limport)
- [Exemples](#exemples)

## Vue d'ensemble

Le service `SoftwareVersionService` permet d'importer des fichiers CSV contenant des informations sur les versions de logiciels. L'import est intelligent et supporte plusieurs formats de fichiers provenant de differentes sources.

### Fonctionnalites principales

- Detection automatique de l'encodage (UTF-8, Windows-1252)
- Detection automatique du delimiteur (`;`, `,`, `\t`)
- Mapping intelligent des colonnes
- Gestion des doublons (mise a jour si existant)
- Extraction des URLs depuis les champs "Version Package"
- Conversion des liens au format JSON

## Formats CSV supportes

### 1. Format Sipedia - CSR Detail des versions

Format exporte depuis Sipedia avec les colonnes detaillees des versions.

| Colonne | Description |
|---------|-------------|
| Libelle Solution | Nom du logiciel |
| Version Roadmap | Version recommandee |
| Statut Version Roadmap | Statut de la version |
| Version Package | Version avec liens optionnels |
| Derniere mise a jour | Date de mise a jour |
| Fin Support Initial | Date fin support initial |
| Fin Support Etendu | Date fin support etendu |
| Fin Support Cyber | Date fin support cyber |
| Liens utiles sur la version package | URLs associees |

### 2. Format Sipedia - Catalogue des solutions

Format simplifie du catalogue.

| Colonne | Description |
|---------|-------------|
| Libelle Solution | Nom du logiciel |
| Politique Industrielle | Statut (Privilegiee, Autorisee, etc.) |
| Fournisseur/Editeur | Vendeur |
| Descriptif | Notes |

> **Note** : Dans ce format, si "Version Roadmap" est absente, la version sera "N/A".

### 3. Format standard

Format generique simple.

| Colonne | Description |
|---------|-------------|
| Logiciel / Software / Nom | Nom du logiciel |
| Version | Numero de version |
| Statut / Status | Statut de la version |
| Date Statut | Date de mise a jour |
| Notes / Commentaire | Remarques |

## Detection automatique

### Encodage

Le service detecte automatiquement l'encodage du fichier :

```java
// Detection basee sur les octets du fichier
- UTF-8 BOM (EF BB BC)
- Caracteres Windows-1252 (octets 0x80-0x9F)
- UTF-8 par defaut
```

### Delimiteur

```java
// Compte les occurrences pour determiner le delimiteur
- Tabulation (\t) si majoritaire
- Virgule (,) si plus nombreuse que point-virgule
- Point-virgule (;) par defaut
```

## Colonnes reconnues

Le mapping des colonnes est effectue par analyse des en-tetes :

| Mots-cles | Colonne mappee |
|-----------|----------------|
| "libelle" + "solution" | Nom du logiciel |
| "version" + "roadmap" | **Version** (source unique) |
| "version" + "package" | Liens utiles (extraction URLs) |
| "statut" + "version" + "roadmap" | Statut |
| "derniere" + "mise" + "jour" | Date mise a jour |
| "fin" + "support" + "init" | Fin support initial |
| "fin" + "support" + "etendu" | Fin support etendu |
| "fin" + "support" + "cyb" | Fin support cyber |
| "liens" + "utiles" | Liens URL |
| "politique" + "industrielle" | Statut (Sipedia catalogue) |

> **Note importante** : La colonne "Identifiant de la solution" n'est PAS utilisee pour la version.
> Seule la colonne "Version Roadmap" est utilisee comme source pour le champ Version.

## Traitement des donnees

### Source de la version

Le champ **Version** est alimente **uniquement** par la colonne "Version Roadmap" :

```
Colonne CSV "Version Roadmap" → Champ Version
```

| Valeur CSV | Version affichee |
|------------|------------------|
| R2025 | R2025 |
| 7.3.2 | 7.3.2 |
| 10.8.X | 10.8.X |
| (vide) | N/A |

> **Important** : La colonne "Version Package" n'est **jamais** utilisee pour le champ Version
> car elle contient souvent des descriptions de liens (DeposIT, Installation, etc.) et non des versions.

### Gestion des doublons

L'import verifie si une combinaison (logiciel, version) existe deja :
- **Si existant** : Mise a jour des champs
- **Si nouveau** : Creation d'un nouvel enregistrement

## Gestion des liens utiles

### Format d'entree

Les liens peuvent etre fournis dans plusieurs formats :

```
# Format "Label (URL)"
DeposIT - Package Mig (https://deposit.edf.fr/...), Guide (https://docs.edf.fr/...)

# URL simple
https://example.com/download

# Plusieurs liens separes par virgule
Doc (https://...) , Download (https://...)
```

### Format de stockage (JSON)

Les liens sont convertis en JSON pour le stockage :

```json
[
  {"label": "DeposIT - Package Mig", "url": "https://deposit.edf.fr/..."},
  {"label": "Guide", "url": "https://docs.edf.fr/..."}
]
```

### Extraction depuis Version Package

Si le champ "Version Package" contient des URLs, elles sont automatiquement extraites et ajoutees aux liens utiles.

## Statuts des versions

### Mapping des statuts

| Valeur CSV | Statut interne |
|------------|----------------|
| Preconisee | `PRECONISEE` |
| Trajectoire Preconisee | `TRAJECTOIRE_PRECONISEE` |
| Prochaine | `TRAJECTOIRE_PRECONISEE` |
| Toleree | `TOLEREE` |
| Autorisee | `TOLEREE` |
| Alternative | `TOLEREE` |
| Privilegiee | `PRECONISEE` |
| Interdite | `INTERDITE` |
| Deconseillee | `INTERDITE` |
| Interdite Cyber | `INTERDITE_CYBER` |

### Valeur par defaut

Si le statut n'est pas reconnu ou vide : `TOLEREE`

## Resultat de l'import

L'API retourne un objet JSON avec les statistiques :

```json
{
  "imported": 150,       // Nouveaux enregistrements
  "updated": 45,         // Enregistrements mis a jour
  "errors": 3,           // Erreurs rencontrees
  "total": 195,          // Total traite
  "errorMessages": [     // Details des erreurs
    "Ligne 42: Nom du logiciel manquant"
  ]
}
```

## Exemples

### Exemple 1 : Import Sipedia CSR

```csv
Libelle Solution;Version Roadmap;Statut Version Roadmap;Version Package;Liens utiles
Java JRE;8u402;Preconisee;8u402 (https://java.com/dl);Doc (https://docs.oracle.com)
Apache Tomcat;9.0.85;Toleree;9.0.85;
```

### Exemple 2 : Import standard

```csv
Logiciel;Version;Statut;Notes
PostgreSQL;16.1;Preconisee;LTS version
MySQL;8.0.35;Toleree;Community edition
```

### Exemple 3 : Import avec liens multiples

```csv
Libelle Solution;Version Roadmap;Liens utiles sur la version package
AIX;7.3;DeposIT (https://deposit.edf.fr/aix) , Guide Install (https://docs.ibm.com/aix)
```

## Fichiers Excel (.xlsx)

Depuis la version courante, les deux imports acceptent directement les classeurs Excel,
sans conversion prealable. Les exports EDF peuvent donc etre deposes tels quels.

### Choix de l'onglet

Un classeur contient plusieurs onglets ; le service retient automatiquement le bon.

| Import | Onglet retenu | Critere |
|--------|---------------|---------|
| `/api/software-versions/import` | `CSR - Detail des versions` | premier onglet portant a la fois le libelle de la solution et une version. Le catalogue `CSR - Liste des solutions` ne sert que de repli (versions a `N/A`) |
| `/api/server-obsolescence/import` | `DONNEES` | l'onglet produisant le plus d'entrees |

La ligne d'en-tete est cherchee dans les 20 (versions) ou 30 (obsolescence) premieres
lignes : les lignes de filtres et de titres placees au-dessus sont ignorees.

### Import obsolescence : deux formes acceptees

**Extraction Prevobs a plat** (onglet `DONNEES`, ou export CSV direct depuis Power BI) :
une ligne par couple serveur/composant. Colonnes lues par leur nom, accents et casse
indifferents : `SERVEUR`, `ENVIRONNEMENT`, `INSTANCE`, `CSR STATUT VERSION PACKAGE`,
`INVENTIV NOM COMPOSANT`, `CSR VERSION PACKAGE`, `DATE FIN SUPPORT`,
`OS CIBLE CSR VERSION PACKAGE`.

L'extraction couvre tout le SI : les lignes sont filtrees sur la colonne `NOM SERVICE`.

```yaml
gedpei:
  import:
    service-filter: GED-PEI   # defaut ; vider pour tout importer
```

**TCD hierarchique** prepare sous Excel : reconnu a ses colonnes `ENVIRONNEMENT` /
`INSTANCE` / `SERVEUR` en positions 0, 1 et 2. Le statut et le serveur sont portes par
des lignes de regroupement et s'appliquent aux lignes de composants qui suivent.

> **Attention** : un TCD est souvent livre replie sur quelques serveurs seulement.
> L'onglet `DONNEES` du meme classeur contient la totalite des lignes, c'est lui qu'il
> faut privilegier.

---

## API Endpoint

### Import CSV

```http
POST /api/software-versions/import
Content-Type: multipart/form-data

file: [fichier.csv ou fichier.xlsx]
```

### Reponse

```json
{
  "imported": 100,
  "updated": 25,
  "errors": 2,
  "total": 125,
  "errorMessages": ["Ligne 15: Format de date invalide"]
}
```

## Code source

Le service d'import est implemente dans :
- Backend : `SoftwareVersionService.java` (methode `importFromCsv`)
- Frontend : `SoftwareVersions.jsx` (composant d'import)

## Notes techniques

### Performance

- Import par lots avec transactions
- Detection d'encodage sur les 1000 premiers octets
- Parsing CSV avec OpenCSV

### Securite

- Validation des donnees avant insertion
- Echappement JSON pour les liens
- Taille de fichier limitee par configuration Spring

### Maintenance

Pour ajouter un nouveau format CSV :
1. Identifier les en-tetes du nouveau format
2. Ajouter les mots-cles dans `detectColumnMapping()`
3. Adapter `parseRecord()` si necessaire
4. Tester avec un fichier d'exemple
