# Service de Suivi (TRK)

Ingestion des positions GPS, calcul d'ETA, détection d'anomalies.

## Fonctionnalités clés (MVP Phase 1)

- **Ingestion des positions** (EF-TRK-01)
  - Consommation Kafka de `PositionBrute` (service-flt → Mobile)
  - Idempotence garantie par `UNIQUE(event_id)` en base
  - Trois horodatages : capture / transmission / ingestion
- **Calcul d'ETA** (EF-TRK-02, RG-067, RG-068)
  - Estimation par vitesse moyenne sur les N dernières positions
  - Intervalle de confiance asymétrique (pénalise la sous-estimation)
  - Phase 1 : destination = dernière position (Phase 2 : destination réelle via OPT)
- **Détection d'anomalies** (EF-TRK-03, EF-TRK-04)
  - Arrêt prolongé (> 30 min, < 0.1 km parcouru)
  - Absence prolongée de position (> 2h)
  - Saut aberrant (> 200 km/h)
  - Écart de corridor (> 50 km – version simplifiée MVP)
- **Événements Kafka publiés** (flux asynchrone vers Mobile)
  - `PositionETA` → service-exe (mise à jour du suivi client)
  - `AlerteEcart` → service-not (notification multicanal)

## Exigences CDC couvertes

| Réf. | Description |
|------|-------------|
| EF-TRK-01/02 | Ingestion tolérante à la connectivité, ETA dynamique avec intervalle |
| EF-TRK-03/04 | Détection d'anomalies, affichage de l'âge de la position |
| RG-066 | Fraîcheur affichée (horodatage + âge) |
| RG-067 | Honnêteté de l'ETA (intervalle de confiance) |
| RG-068 | Asymétrie du coût de l'erreur |

## Dépendances

- `service-opt` (missionId → origine/destination – Phase 2)
- `service-geo` (axes/hubs – Phase 2 pour écart de corridor)
- `Kafka` (consommation de `position-brute`, publication de `position-eta` et `alerte-ecart`)
- `PostgreSQL` (persistance des positions)

## API exposées

Aucune API REST publique (tout est événementiel).  
Point d'entrée interne : consommateur Kafka `position-brute`.

## Événements Kafka

### Consommés
- `position-brute` → depuis service-flt (Mobile)

### Publiés
- `position-eta` → service-exe (Mobile)
- `alerte-ecart` → service-not (Mobile)

## Contrats AsyncAPI

`shared-contracts/asyncapi/` (position-brute, position-eta, alerte-ecart)

## Statut

✅ MVP Phase 1 – Fonctionnel, avec améliorations prévues en Phase 2 (destination réelle, corridor exact)
