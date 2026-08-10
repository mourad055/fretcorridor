# ADR 0013 — `service-bur` consomme `AffectationConfirmee` (Kafka) plutôt qu'un endpoint REST `service-opt`

**Statut** : Accepté (décision d'équipe, 2026-08-10)

## Contexte

Le point n°2 de `docs/ROADMAP_INTEGRATION_gateway.md` (missions appariées, cœur métier du Bureau) proposait deux options : un nouvel endpoint de liste tenant-scopée côté `service-opt`, ou une matérialisation depuis les événements Kafka déjà publiés.

`shared-contracts/openapi/opt-api.yaml` tranche la question sans ambiguïté : *« Aucun consommateur cross-porteur (Mobile/Web) n'appelle cette API directement — le flux OPT -> Mobile passe exclusivement par événements Kafka asynchrones. »* Demander à Moteur un endpoint REST pour le Bureau irait contre leur propre contrat documenté, et contre la règle de communication du Plan d'Exécution §4.3 (asynchrone obligatoire entre porteurs différents).

L'événement `AffectationConfirmee` (déjà publié par `service-opt`, déjà consommé par `service-exe`) porte déjà l'essentiel de ce qu'une vue Bureau nécessite : `missionId`, `origineNom`/`destinationNom` (noms lisibles), `transporteurId`, `axeId`, `prixTransport`, `devise`.

## Décision

`service-bur` (Web, déjà porteur de la supervision Bureau) consomme directement `affectation-confirmee` et matérialise un modèle de lecture local (`MissionAppariee`), exposé en REST au gateway sous `GET /api/v1/bur/missions-appariees?tenantId=`. Le gateway remplace `MockOptAdapter` par `ServiceBurMissionAppparieeAdapter`, qui appelle `service-bur` — jamais `service-opt`.

Deux limites acceptées, documentées dans le code plutôt que masquées :
- **Pas de nom de transporteur.** L'événement ne porte qu'un `transporteurId` (UUID) ; le résoudre en nom exigerait un appel supplémentaire à `service-ida`, hors périmètre de cette décision. `transporteurNom` affiche l'id brut en attendant.
- **Statut toujours `CONFIRMEE`.** Aucun événement publié à ce jour (`shared-contracts/asyncapi/events/`) ne porte de transition de cycle de vie (EN_COURS/CLOTUREE) — cette information appartient à `service-exe` (Mobile), qui ne publie encore aucun événement de ce type.
- **Tenant stampé, pas dérivé.** Même raisonnement que l'ADR 0011 (GEO) : l'événement ne porte pas de tenant, Phase 1 est mono-tenant (Feuille de route §1.1), le tenant est fixé par configuration (`fretcorridor.bur.tenant-id-phase1`) plutôt que déduit d'une donnée qui n'existe pas encore.

Idempotence assurée par une contrainte d'unicité sur `eventId` (même mécanisme que `DemandePublieeListener` côté `service-opt`) — un rejeu Kafka du même événement ne crée jamais de doublon.

## Conséquences

- **Débloque une bonne partie du point n°2**, sans dépendre de Moteur : le Bureau voit désormais de vraies missions appariées (origine, destination, prix), plus des données mockées.
- Le filtre par `statut` (EF-BUR-02, déjà construit côté gateway pour `EN_COURS`/`CLOTUREE`) ne trouvera jamais que `CONFIRMEE` dans les données réelles tant que `service-exe` ne publie pas d'événement de transition — comportement correct, pas un bug, mais à garder en tête si le Bureau signale l'absence de missions "en cours"/"clôturées" dans l'écran réel.
- `transporteurNom` affichant un UUID est une régression d'affichage clairement documentée (code + ce fichier), pas un choix silencieux — un futur enrichissement (appel `service-ida` pour résoudre le nom) reste possible sans changer le contrat `OptPort`.
- Si `service-exe` publie un jour un événement de clôture/démarrage de mission, `AffectationConfirmeeListener`/`MissionAppariee` devront être étendus pour le consommer aussi et faire évoluer le statut stocké — actuellement, l'entité ne porte qu'un état figé au moment de la confirmation.
