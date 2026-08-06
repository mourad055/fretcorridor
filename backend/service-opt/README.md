# Moteur d'Optimisation (OPT)

Cœur algorithmique du matching, de la planification et de l'optimisation des flux (couches L0 à L4).

## Fonctionnalités clés (MVP Phase 1)

- **L0 – Filtrage géospatial** (§8.5.4)
  - Réduction de l'espace de recherche par zonage H3
  - Appel synchrone interne à `service-geo`
  - Budget latence ~50 ms
- **L1 – Affectation** (§8.5)
  - Appariement par lots (jamais glouton)
  - Résolution par algorithme hongrois (Kuhn-Munkres)
  - Coût composite multi-critères via `service-mat`
  - Traçabilité : `CycleMatching` stocké en base
- **Itinéraire Valhalla**
  - Calcul de trajet avec profil camion (poids, hauteur, essieux)
  - Mode dégradé si Valhalla injoignable (itinéraire null)
- **L4 – Tarification** (§8.9)
  - Régime `POIDS_TAXABLE` ou `FORFAITAIRE_VEHICULE`
  - Barèmes versionnés, configurables par axe et type de véhicule
  - Commission distincte, plancher activable, facteur de tension borné
- **Événements Kafka publiés** (flux asynchrone vers Mobile)
  - `PropositionEmise` → service-mkt (au plus 3 propositions par demande)
  - `AffectationConfirmee` → service-exe (création de mission)

## Exigences CDC couvertes

| Réf. | Description |
|------|-------------|
| EF-MAT-01/02/03 | Appariement par cycles, rayon borné, axe actif |
| EF-MAT-04 | Coût composite multi-critères |
| EF-MAT-11/12 | Traçabilité, signalement mode dégradé |
| EF-MAT-13 | Plan de chargement exploitable (Phase 2) |
| §8.5 à §8.9 | Spécification du moteur L0→L4 |
| ENF-PRF-01/02 | Budgets de latence (< 2s interactif, 50ms L0) |

## Dépendances

- `service-geo` (filtrage L0 – synchrone interne)
- `service-mat` (coût composite – synchrone interne)
- `Valhalla` (service d'itinéraires – synchrone externe)
- `Kafka` (publication d'événements)
- `PostgreSQL` (persistance des affectations)

## API exposées

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| `/api/opt/filtrage-l0` | GET | Test du filtrage H3 (temporaire, disparaîtra) |
| `/api/opt/affectation-l1` | POST | Test de L1 (temporaire, disparaîtra) |
| `/api/opt/affectations/{missionId}` | GET | Point d'entrée pour TRK (synchrone interne) |

## Événements Kafka

### Publiés
- `proposition-emise` → service-mkt
- `affectation-confirmee` → service-exe

## Contrat OpenAPI

`shared-contracts/openapi/opt-api.yaml` (à finaliser)

## Statut

✅ MVP Phase 1 – L0 et L1 fonctionnels, L2/L3 en Phase 2
