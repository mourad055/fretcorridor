# ADR 0014 — `service-bur` consomme `position-eta` (Kafka) plutôt qu'un endpoint REST `service-trk`

**Statut** : Accepté (décision d'équipe, 2026-08-10)

## Contexte

Le point n°4 de `docs/ROADMAP_INTEGRATION_gateway.md` (positions véhicules, supervision Bureau) était bloqué : `service-trk` (Moteur) n'expose **aucune API REST**, tout y est événementiel (Kafka `position-eta`/`alerte-ecart`). Il n'y a donc même pas d'endpoint à demander à Moteur — la seule voie possible est la consommation d'événements, exactement le même raisonnement que l'ADR 0013 pour OPT/missions-appariées.

L'événement `position-eta` (déjà publié par `service-trk`, déjà consommé par Mobile) porte l'essentiel de ce qu'une vue Bureau nécessite : `missionId`, `vehiculeId`, `derniereLatitude`/`derniereLongitude`, `horodatageDernierePosition`.

## Décision

`service-bur` (Web) consomme directement `position-eta` et matérialise un modèle de lecture local (`PositionVehicule`), exposé en REST au gateway sous `GET /api/v1/bur/positions?tenantId=`. Le gateway remplace `MockTrkAdapter` par `ServiceBurPositionAdapter`, qui appelle `service-bur` — jamais `service-trk`. Même patron d'implémentation que l'ADR 0013 (fixture de test déplacée en `@Primary`, WebClient minimal, DTO miroir).

Une nuance distingue toutefois ce cas du cas OPT : **la persistance est un upsert-si-plus-récent, pas un append-only.** Une mission a une seule position "courante" ; contrairement aux missions appariées (un événement = une ligne, jamais mise à jour), chaque nouvel événement `position-eta` pour un `missionId` donné doit remplacer la position précédente — mais seulement si son horodatage (`horodatageDernierePosition`) est strictement postérieur à celui déjà stocké. Kafka ne garantit pas l'ordre entre partitions ; sans cette garde, un message en retard ou rejoué pourrait écraser une position plus récente et afficher un véhicule à une position obsolète sans que rien ne l'indique (violation de RG-043 en pratique, même si l'horodatage réel serait techniquement présent).

Trois limites acceptées, documentées dans le code plutôt que masquées :
- **Pas de libellé véhicule.** L'événement ne porte qu'un `vehiculeId` (UUID) ; le résoudre en plaque/label exigerait un appel supplémentaire à un référentiel véhicules, hors périmètre de cette décision. `vehiculeLabel` affiche l'id brut en attendant — même limite, même raisonnement que `transporteurNom` dans l'ADR 0013.
- **Tenant stampé, pas dérivé.** Même raisonnement que les ADR 0011 (GEO) et 0013 (OPT) : l'événement ne porte pas de tenant, Phase 1 est mono-tenant (Feuille de route §1.1), le tenant est fixé par la même configuration `fretcorridor.bur.tenant-id-phase1` (réutilisée, pas dupliquée).
- **ETA non retenu.** `position-eta` porte aussi l'ETA calculé par Moteur pour EF-TRK-02 (`etaCentral`/`etaBorneBasse`/`etaBorneHaute`, `distanceRestanteKm`, `vitesseEstimeeKmh`) ; `PositionEtaListener` n'en extrait que la position brute. Choix délibéré, pas un oubli : EF-BUR-01 (supervision Bureau) porte sur les *missions*, pas sur l'ETA par véhicule, et le CDC n'exige nulle part que le Bureau affiche cet ETA — seul EF-TRK-04 (position + âge) est dans le périmètre de cette vue. Si un futur besoin Bureau l'exige, `PositionVehicule`/`PositionEntity` devront être étendus pour porter ces champs, sans changer le mécanisme d'ingestion.

Idempotence assurée par `enregistrerSiPlusRecente` (upsert conditionnel sur `capturedLe`, contrainte d'unicité sur `mission_id`) — un rejeu ou un désordre Kafka du même événement ne peut jamais reculer l'état affiché.

## Conséquences

- **Débloque le point n°4**, sans dépendre de Moteur : le Bureau voit désormais de vraies positions (latitude/longitude/horodatage), plus des données mockées.
- `vehiculeLabel` affichant un UUID est une régression d'affichage clairement documentée (code + ce fichier), pas un choix silencieux.
- Le modèle de persistance (`PositionEntity`, upsert-si-plus-récent) diffère délibérément de `MissionAppparieeEntity` (append-only, insert-once) : ne pas les traiter comme interchangeables lors d'une future évolution du schéma.
- Si un référentiel véhicules devient accessible côté service-bur, `vehiculeLabel` pourra être enrichi sans changer le contrat `TrkPort`.
