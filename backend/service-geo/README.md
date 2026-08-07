# Service Géospatial (GEO)

Référentiel géospatial pour FretCorridor : axes, hubs, zonage H3.

## Fonctionnalités clés (MVP Phase 1)

- Gestion des **axes** (liaison entre hubs) avec paramètres :
  - États d'activation indépendants : visibilité / matching / paiement
  - Rayon d'appariement, fenêtre de matching
  - Surcouche sécuritaire (zones sensibles)
- Gestion des **hubs** (villes, plateformes, points de consolidation)
- **Zonage H3** :
  - Conversion lat/lon → index H3 (`indexPourPoint`)
  - Voisinage hexagonal (`kRing`) pour le filtrage L0 d'OPT
  - Résolution configurable en base (anti-patron "pas de code en dur")

## Exigences CDC couvertes

| Réf. | Description |
|------|-------------|
| EF-GEO-01 | Axe/Hub comme entités de premier rang |
| EF-GEO-02 | Paramétrage par axe (routage, ETA, seuils, pondérations) |
| EF-GEO-03 | États d'activation indépendants (visibilité/matching/paiement) |
| EF-GEO-04 | Surcouche de risque sécuritaire par segment |
| EF-GEO-05 | Multi-pays et conventions bilatérales (Phase 4) |

## Dépendances

- `PostgreSQL` avec extension `PostGIS` (requêtes spatiales)
- `Uber H3` (zonage hexagonal)
- `Spring Boot 3.3.4` + `Spring Data JPA`

## API exposées

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| `/api/geo/axes` | POST | Création d'un axe |
| `/api/geo/axes` | GET | Liste des axes |
| `/api/geo/axes/{id}` | GET | Détail d'un axe |
| `/api/geo/axes/actifs-matching` | GET | Axes où `matchingActif=true` (EF-GEO-03) — consommé par OPT |
| `/api/geo/axes/{id}/etats/{etat}` | PATCH | Bascule un état d'activation (`visibilite`/`matching`/`paiement`), indépendamment des deux autres |
| `/api/geo/hubs` | POST | Création d'un hub (calcule l'index H3 à la création) |
| `/api/geo/hubs` | GET | Liste des hubs |
| `/api/geo/hubs/{id}` | GET | Détail d'un hub |
| `/api/geo/zonage/index` | GET | Conversion lat/lon → index H3 |
| `/api/geo/zonage/k-ring` | GET | Voisinage hexagonal d'une cellule H3 |
| `/api/geo/zonage/hubs-proches` | GET | Filtrage L0 : hubs autour d'un point (H3 k-ring) |

## Consommé par

- **OPT** (filtrage L0 – synchrone interne)
- **TRK** (écart de trajectoire – synchrone interne)
- **Web** (cartes, supervision – via API)

## Contrat OpenAPI

`shared-contracts/openapi/geo-api.yaml`

## Statut

✅ MVP Phase 1 – Fonctionnel
