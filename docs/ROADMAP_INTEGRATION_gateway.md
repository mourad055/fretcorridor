# Roadmap d'intégration — débrancher les mocks du gateway

**Date** : 2026-08-08
**Auteur** : Mourad (volet Web), avec assistance Claude Code
**Contexte** : `feature/web-socle` a reconstruit le gateway en architecture hexagonale complète (JWT/RBAC, PAY/ADM réels) pendant que `dev` (Mobile + Moteur) livrait les vrais microservices que ce gateway doit appeler. Décision d'équipe : le gateway garde son architecture actuelle ; les adaptateurs `Mock*` sont remplacés par de vrais appels HTTP, service par service, au fur et à mesure que les services cibles exposent ce dont le gateway a besoin.

Pour l'historique de l'analyse de fusion (conflits Git, divergence des deux branches), voir [`docs/ANALYSE_FUSION_dev_web-socle.md`](ANALYSE_FUSION_dev_web-socle.md). Ce document-ci ne couvre que le suivi de l'intégration gateway ↔ services réels.

---

## Constat

8 adaptateurs `Mock*` existent dans `backend/gateway`. Après exploration détaillée du code réel de `origin/dev` (pas seulement de ses contrats OpenAPI), **4 sont désormais réellement branchés** : l'authentification (`service-ida`), les axes Bureau (`service-geo`, décision mono-tenant Phase 1 assumée — cf. Phase 1bis), les missions appariées (`service-bur`, consommation Kafka de `AffectationConfirmee` — cf. Phase 1quater) et les positions véhicules (`service-bur`, consommation Kafka de `position-eta` — cf. Phase 1quinquies). Le blocage transverse n°6 (double autorité JWT) est réglé (Phase 1ter), ce qui débloque partiellement #3 et #5. Les autres restent bloqués par une absence réelle côté service cible — pas par un problème côté gateway.

## Phase 1 — Fait : authentification réelle (`service-ida`)

`MockIdaAuthenticationAdapter` remplacé par `ServiceIdaAuthenticationAdapter`, qui appelle `POST /api/auth/login` sur `service-ida` (port 8081).

Détails :
- **PIN, pas OTP.** `service-ida` expose téléphone + code PIN. Le CDC (EF-IDA-01) exige un « code à usage unique » (OTP) — écart réel, pas de notre ressort technique : signalé ici pour que l'équipe/le sponsor tranche, mais le gateway s'adapte au contrat qui existe réellement plutôt que d'implémenter un flux qui n'existe nulle part.
- `AuthController` (gateway) est inchangé : il continue d'émettre son propre JWT après authentification — `service-ida` n'est appelé que pour valider les identifiants, son propre JWT (secret différent) n'est jamais réutilisé.
- Mapping des rôles : `service-ida` peut renvoyer plusieurs rôles métier (ex. `CHAUFFEUR`) ; le gateway retient le premier rôle qu'il reconnaît (`BUREAU`, `TRANSPORTEUR`, `ADMINISTRATION`→`ADMIN`). Aucun rôle reconnu → traité comme identifiants invalides (pas de distinction, pour ne rien révéler à un attaquant).
- Erreurs : `401` de `service-ida` → `InvalidCredentialsException` (401) ; tout le reste (timeout, panne, réponse malformée) → nouvelle `AuthenticationServiceUnavailableException` (503) — un service en panne n'est pas la même situation qu'un mauvais code, l'utilisateur ne doit pas les confondre.
- Timeout explicite de 3 s (absent des adaptateurs `ServiceAdmWebClientAdapter`/`ServicePayWebClientAdapter` existants — écart de rigueur préexistant, non corrigé ici, à traiter uniformément si l'équipe le juge utile).
- **Tests non affectés** : `MockIdaAuthenticationAdapter` déplacé vers `src/test/java`, marqué `@Primary` — les 10 suites `@SpringBootTest` existantes (isolation, RBAC, KYC) continuent de tourner sans réseau ni instance `service-ida`, sans qu'un seul fichier de test existant n'ait eu besoin d'être modifié.
- **Comptes de démonstration** : `service-ida` n'a aucune donnée de démo et aucun endpoint de création de compte BUREAU/TRANSPORTEUR/ADMIN. Script de seed proposé dans [`docs/proposition-seed-service-ida.sql`](proposition-seed-service-ida.sql) (5 comptes identiques à l'ancien mock) — **à relire et fusionner par Mobile**, pas poussé directement dans `backend/service-ida/`.

Vérification : `cd backend/gateway && TESTCONTAINERS_RYUK_DISABLED=true mvn test` — suite complète verte.

## Phase 1bis — Fait : axes Bureau (`service-geo`), mono-tenant Phase 1 assumé

`MockGeoAdapter` remplacé par `RealGeoAdapter`, qui appelle `GET /api/geo/axes` sur `service-geo` (port 8084). Décision d'équipe le 2026-08-10 (cf. [ADR 0011](adr/0011-geo-mono-tenant-phase-1.md)) : `service-geo` ne filtre pas par tenant, mais la Feuille de route §1.1 scope la Phase 1 à un seul axe/tenant réel (BGFT) — l'absence de filtrage serveur est donc sans conséquence dans ce périmètre.

Détails :
- `RealGeoAdapter` colle le `tenantId` du JWT appelant sur chaque axe retourné par `service-geo` (qui n'en filtre aucun lui-même) — **ce n'est pas une garantie d'isolation réelle**, seulement une absence de risque tant qu'un seul tenant existe. Documenté explicitement dans le Javadoc de la classe et dans l'ADR.
- **La limite est testée, pas juste commentée** : `RealGeoAdapterTest` (nouveau) caractérise ce comportement contre un `service-geo` factice — il est censé casser le jour où `service-geo` commence à filtrer réellement, signal explicite pour migrer la suite d'isolation.
- `AxeControllerIsolationTest` continue de vérifier le contrat du port `GeoPort`, mais via la fixture `MockGeoAdapter` (déplacée en `src/test/java`, `@Primary` — même mécanisme que l'auth), pas via `RealGeoAdapter` : Javadoc mis à jour pour le préciser sans ambiguïté.
- `fretcorridor.service-geo.base-url` déjà présent dans `application.yml`/`docker-compose.gateway.yml` depuis l'intégration de `backend-stevetelecom`.

Vérification : suite gateway complète — 95/95 verts (94 précédents + `RealGeoAdapterTest`).

## Phase 1ter — Fait : mécanisme de retransmission du token service-ida (débloque le point n°6)

Décision d'équipe le 2026-08-10 (cf. [ADR 0012](adr/0012-delegation-token-service-ida.md)) : le gateway retransmet le JWT brut de `service-ida` plutôt que d'aligner les secrets entre le gateway et Mobile (option qui aurait exigé une coordination cross-équipe sur le secret ET la forme des claims).

Détails :
- `Actor` porte désormais un champ `delegationToken` (le `accessToken` de `service-ida`) ; `ServiceIdaAuthenticationAdapter` le renseigne, `MockIdaAuthenticationAdapter` (fixture de test) porte une valeur factice.
- `JwtService.issue` l'embarque dans le JWT du gateway (claim privée `idaToken`, jamais régénérée) ; `JwtReactiveAuthenticationManager` l'extrait à chaque requête et le porte sur `AuthenticatedActor.delegationToken()`, disponible à tout contrôleur/adaptateur via `@AuthenticationPrincipal`.
- Usage prévu pour les futurs `RealExeAdapter`/`RealNotAdapter` (non construits ici — items n°3/n°5 encore bloqués par ailleurs, cf. tableau) : `Authorization: Bearer <actor.delegationToken()>` vers ces services, jamais le JWT du gateway.
- Tests dédiés : round-trip complet couvert à chaque étage (`ServiceIdaAuthenticationAdapterTest`, `JwtServiceTest`, nouveau `JwtReactiveAuthenticationManagerTest`).

Vérification : suite gateway complète — 100/100 verts (95 précédents + 5 nouveaux tests).

## Phase 1quater — Fait (partiellement) : missions appariées (`service-bur`), consommation Kafka

Décision d'équipe le 2026-08-10 (cf. [ADR 0013](adr/0013-bur-consomme-affectation-confirmee.md)) : `opt-api.yaml` interdit explicitement tout consommateur cross-porteur en REST direct sur `service-opt` — le chemin correct est de consommer l'événement `AffectationConfirmee` (déjà publié par le Moteur, déjà consommé par `service-exe`), pas de demander un nouvel endpoint à Moteur.

Détails :
- **Nouveau côté `service-bur`** : `AffectationConfirmeeListener` (Kafka, idempotent via contrainte d'unicité sur `eventId` — même principe que `DemandePublieeListener` côté `service-opt`) matérialise un modèle de lecture (`MissionAppariee`), exposé via `GET /api/v1/bur/missions-appariees?tenantId=`.
- **Côté gateway** : `MockOptAdapter` remplacé par `ServiceBurMissionAppparieeAdapter`, qui appelle `service-bur` (jamais `service-opt`) — `MockOptAdapter` déplacé en fixture de test (`src/test/java`, `@Primary`, même mécanisme que GEO/auth).
- **Deux limites acceptées et documentées** (Javadoc + ADR 0013) : `transporteurNom` affiche l'id brut (`transporteurId`, l'événement ne porte pas de nom — résoudre exigerait un appel à `service-ida`, hors périmètre ici) ; `statut` vaut toujours `CONFIRMEE` (aucun événement de transition EN_COURS/CLOTUREE n'existe encore côté `service-exe`). Le filtre par statut déjà construit côté gateway (EF-BUR-02) ne trouvera donc jamais `EN_COURS`/`CLOTUREE` dans les vraies données pour l'instant — comportement correct, pas un bug.
- Tenant stampé par configuration (`fretcorridor.bur.tenant-id-phase1`), même raisonnement mono-tenant Phase 1 que GEO (ADR 0011).

Vérification : suite `service-bur` — 18/18 verts (nouveau, premier test `@SpringBootTest` de ce service à cohabiter avec `@EnableKafka` — confirmé sans bloquer le démarrage du contexte même sans broker Kafka joignable). Suite gateway complète — 102/102 verts.

## Phase 1quinquies — Fait : positions véhicules (`service-bur`), consommation Kafka (`position-eta`)

Décision d'équipe le 2026-08-10 (cf. [ADR 0014](adr/0014-bur-consomme-position-eta.md)) : `service-trk` n'expose **aucune API REST** — le patron Kafka de la Phase 1quater (OPT) s'applique tel quel, sans dépendre de Moteur.

Détails :
- **Nouveau côté `service-bur`** : `PositionEtaListener` (Kafka, 2ᵉ `ConsumerFactory`/`ListenerContainerFactory` ajoutée à `KafkaConsumerConfig`) matérialise un modèle de lecture (`PositionVehicule`), exposé via `GET /api/v1/bur/positions?tenantId=`.
- **Persistance différente de la Phase 1quater, volontairement** : `PositionRepositoryAdapter.enregistrerSiPlusRecente` fait un **upsert conditionnel** (une position par `missionId`, remplacée seulement si le nouvel horodatage est strictement postérieur), pas un append-only comme `MissionAppparieeEntity`. Une mission n'a qu'une seule position "courante" ; sans cette garde, un message Kafka en retard ou rejoué pourrait afficher une position obsolète comme si elle était actuelle (RG-043). Testé explicitement (`PositionRepositoryAdapterIntegrationTest.ignores_a_position_older_than_the_one_already_stored_late_kafka_message`).
- **Côté gateway** : `MockTrkAdapter` remplacé par `ServiceBurPositionAdapter`, qui appelle `service-bur` (jamais `service-trk`) — `MockTrkAdapter` déplacé en fixture de test (`src/test/java`, `@Primary`, même mécanisme que GEO/OPT/auth).
- **Limite acceptée et documentée** (Javadoc + ADR 0014) : `vehiculeLabel` affiche l'id brut (`vehiculeId`, l'événement ne porte pas de libellé — résoudre exigerait un appel à un référentiel véhicules, hors périmètre ici).
- Tenant stampé par configuration (`fretcorridor.bur.tenant-id-phase1`, réutilisée telle quelle), même raisonnement mono-tenant Phase 1 que GEO/OPT.

Vérification : suite `service-bur` — 26/26 verts (18 précédents + 8 nouveaux : `PositionServiceTest`, `PositionEtaListenerTest`, `PositionControllerTest` ×2, `PositionRepositoryAdapterIntegrationTest` ×4). Suite gateway complète — 104/104 verts (102 précédents + `ServiceBurPositionAdapterTest` ×2).

## Phase 2 — Suivi des mocks restants

Statut tenu à jour au fil des sprints. Chaque ligne : constat exact sur `dev`, ce qu'il faut construire, porteur, priorité.

| # | Mock gateway | Statut | Constat sur `dev` | Ce qu'il faut construire | Porteur | Priorité |
|---|---|---|---|---|---|---|
| 2 | `MockOptAdapter` (missions appariées, incl. filtre/détail/export EF-BUR-02) | 🟢 Branché (limité) | `ServiceBurMissionAppparieeAdapter` (Phase 1quater) consomme `AffectationConfirmee` via `service-bur`. Fonctionne pour les cas nominaux, mais `transporteurNom` = id brut, et `statut` toujours `CONFIRMEE` (aucun événement de transition n'existe encore). | Un événement de transition de statut côté `service-exe` (EN_COURS/CLOTUREE), et une résolution du nom transporteur (appel `service-ida` ou événement enrichi). | Mobile (événements), Web (consommation) | Moyenne — le nominal fonctionne, l'enrichissement reste à faire |
| 3 | `MockExeAdapter` (chronologie mission, missions transporteur) | 🟡 Partiellement débloqué | `service-exe` n'a qu'un lookup par `demandeId` (pas `missionId`, pas de liste par tenant/transporteur). Le mécanisme JWT (point n°6, ex-bloquant) est réglé — le seul obstacle restant est l'absence d'endpoint de liste. | Endpoint de liste tenant-scopée + liste par transporteur. | Mobile | Moyenne |
| 4 | `MockTrkAdapter` (positions) | 🟢 Branché (limité) | `ServiceBurPositionAdapter` (Phase 1quinquies) consomme `position-eta` via `service-bur`. Fonctionne pour le cas nominal (latitude/longitude/horodatage réels), mais `vehiculeLabel` = id brut. | Une résolution du libellé véhicule (appel à un référentiel véhicules ou événement enrichi). `alerte-ecart` reste non consommé (hors périmètre de cette passe). | Web (consommation), Moteur (si événement enrichi) | Basse — le nominal fonctionne, l'enrichissement reste à faire |
| 5 | `MockNotAdapter` (notifications Bureau) | 🟡 Partiellement débloqué | `GET /api/notifications` scope "mes notifications" (acteur du JWT), pas tenant-wide. Le mécanisme JWT (point n°6, ex-bloquant) est réglé — le seul obstacle restant est la forme de l'endpoint. | Endpoint tenant-wide. | Mobile | Basse |
| 6 | *(transverse)* Double autorité JWT | ✅ Réglé (2026-08-10) | `service-exe`/`service-not`/`service-mkt` valident les JWT signés par `service-ida` ; le gateway signe les siens avec son propre secret. | Fait : le gateway retransmet le JWT brut de `service-ida` (`Actor.delegationToken` → claim `idaToken` → `AuthenticatedActor.delegationToken()`) — cf. [ADR 0012](adr/0012-delegation-token-service-ida.md). | Web | — |
| 7 | `MockIdaKycAdapter` (décisions KYC admin) | 🔴 Bloqué | `service-ida` n'a aucun endpoint de décision KYC admin — auto-complétion de profil seulement, `NIVEAU_2` explicitement hors périmètre actuel. | Endpoint `POST` de décision KYC réservé à un rôle admin, idempotent (le gateway l'exige déjà). | Mobile | Basse |
| 8 | `MockCapAdapter` (capacités transporteur) | 🔴 Bloqué — analysé en détail le 2026-08-10, pas juste « à confirmer » | `service-cap` a un vrai code, solide côté domaine : `POST /api/cap/capacites` (déclaration, poids taxable calculé, EF-CAP-01/02), `POST /api/cap/capacites/{id}/decrement` (verrou optimiste `@Version` + table `decrement_log` pour l'idempotence, EF-CAP-07), expiration automatique planifiée (EF-CAP-08), événement Kafka `capacite-declaree` publié. Mais **la forme ne correspond pas au besoin du gateway**, pas juste « pas encore branché » : aucun endpoint de liste (seulement les deux `POST` ci-dessus), aucun champ `transporteurId` nulle part (ni entité, ni événement Kafka — seul `vehiculeId` existe, l'appartenance vit dans `service-flt`), aucune notion de `destination` (seulement un point d'origine + un `axeId`), aucun libellé véhicule lisible, `modeDeclaration` (TOTALE/FRACTIONNAIRE_ASSISTEE/PRECISE) sans rapport avec le `modeCollecte` (PORTE_A_PORTE/POINT_DEPOT) attendu côté gateway, pas d'état `APPARIEE` (seulement `publiee`/`expiree`). Contrairement à OPT/TRK, l'événement Kafka ne comble pas ce manque : il ne porte pas non plus de `transporteurId`. Aucune sécurité (pas de dépendance Spring Security dans le `pom.xml`, tout endpoint est ouvert) ; **aucun test**. | Un endpoint `GET` de liste scopée par transporteur côté `service-cap` (ou un événement enrichi avec `transporteurId`, résolu via `service-flt`), une authentification JWT sur les deux endpoints existants, et au moins un test sur le décrément concurrent. Ce n'est pas une passe de câblage : c'est un travail de conception à faire côté Mobile (`service-cap` + `service-flt`). | Mobile | Basse (le service progresse sur son propre périmètre — déclaration/décrément — la lecture par transporteur reste à concevoir) |

**Constat transverse** : `service-trk` publie aussi `alerte-ecart` (contrat existant dans `shared-contracts/asyncapi/events/`), non consommé ici — les positions (`position-eta`) couvraient le besoin immédiat de supervision (carte Bureau). Un futur écran d'alertes suivrait le même patron (nouveau listener + modèle de lecture + endpoint dans `service-bur`), bon candidat pour une prochaine itération.

**Suivi Phase 3 (à ne pas perdre)** : quand `service-geo` accueillera un second tenant institutionnel, `RealGeoAdapterTest` (nouveau) est conçu pour casser — c'est le signal explicite qu'il faut exiger un vrai filtre serveur côté `service-geo` et migrer `AxeControllerIsolationTest` pour vérifier `RealGeoAdapter` directement (cf. [ADR 0011](adr/0011-geo-mono-tenant-phase-1.md)).

## Comment avancer sur une ligne du tableau

Quand un service cible expose enfin ce qu'il faut :
1. Suivre le pattern déjà en place — `ServiceIdaAuthenticationAdapter`, `ServiceAdmWebClientAdapter`, `ServicePayWebClientAdapter` — un `@Component` implémentant le port existant du domaine, `WebClient.Builder` + `@Value("${fretcorridor.<service>.base-url}")` en constructeur.
2. Ajouter `fretcorridor.<service>.base-url` à `application.yml` et la variable d'environnement correspondante à `docker-compose.gateway.yml`.
3. Déplacer le `Mock*Adapter` correspondant vers `src/test/java` avec `@Primary` (même mécanisme que pour l'auth) — aucune suite de test existante à modifier.
4. Mettre à jour ce document (statut de la ligne).
