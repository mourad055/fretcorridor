# Contrats partagés – FretCorridor

Spécifications OpenAPI (REST) et AsyncAPI (Kafka) pour les services du périmètre Moteur.

## Structure

shared-contracts/
├── openapi/
│ ├── geo-api.yaml # Service Géospatial (GEO)
│ ├── mat-api.yaml # Service de Matching (MAT)
│ ├── opt-api.yaml # Moteur d'Optimisation (OPT) – à finaliser
│ └── trk-api.yaml # Service de Suivi (TRK) – pas d'API REST directe
└── asyncapi/
└── events/
    ├── capacite-declaree.yaml # CAP → MAT/OPT (capacité déclarée, brouillon)
    ├── demande-publiee.yaml # MKT → MAT/OPT (demande publiée, brouillon)
    ├── position-brute.yaml # FLT → TRK (position GPS brute)
    ├── proposition-emise.yaml # OPT → MKT (propositions de matching)
    ├── affectation-confirmee.yaml # OPT → EXE (affectation confirmée)
    ├── position-eta.yaml # TRK → EXE (mise à jour ETA)
    └── alerte-ecart.yaml # TRK → NOT (anomalie détectée)

## Contrats REST (OpenAPI)

| Service | Fichier | Statut |
|---------|---------|--------|
| GEO | `openapi/geo-api.yaml` | ✅ Validé pour Phase 1 |
| MAT | `openapi/mat-api.yaml` | ✅ Validé pour Phase 1 |
| OPT | `openapi/opt-api.yaml` | ✅ Validé pour Phase 1 (endpoints de test marqués `deprecated`) |
| TRK | _(aucun fichier)_ | Pas d'API REST directe – tout est événementiel |

## Contrats événementiels (AsyncAPI)

| Topic | Source → Cible | Statut |
|-------|----------------|--------|
| `capacite-declaree` | CAP (Mobile) → MAT/OPT | ⚠️ Brouillon, à valider avec Mobile |
| `demande-publiee` | MKT (Mobile) → MAT/OPT | ⚠️ Brouillon, à valider avec Mobile |
| `position-brute` | FLT (Mobile) → TRK | ✅ Défini |
| `proposition-emise` | OPT → MKT (Mobile) | ✅ Défini |
| `affectation-confirmee` | OPT → EXE (Mobile) | ✅ Défini |
| `position-eta` | TRK → EXE (Mobile) | ✅ Défini |
| `alerte-ecart` | TRK → NOT (Mobile) | ✅ Défini |

## Convention de versionnement

- Les contrats sont versionnés en sémantique (`v1.0.0`, `v1.1.0`, etc.)
- Toute modification cassante est discutée lors du point de synchronisation hebdomadaire
- Les changements sont documentés dans les `# Changelog` du fichier concerné

## Utilisation

Les contrats sont utilisés par :
- **Mobile** (Personne 1) : consomme `proposition-emise`, `affectation-confirmee` ; produit `position-brute`
- **Web** (Personne 2) : consomme l'API REST GEO pour la cartographie
- **Moteur** (Personne 3) : produit/consomme tous les événements listés

## Statut

✅ Phase 1 : contrats REST GEO/MAT validés, événements Kafka définis
