# FretCorridor v4 — Transmission d'état (mise à jour 18 août 2026)

> Document de suivi/handoff, versionné dans le dépôt à la racine. Remplace
> la version du 7 août 2026, largement obsolète : Phase 2 entière a été
> codée, mergée, et une partie déjà rebranchée sur le vrai backend depuis.

---

## Prérequis avant toute intégration réelle (S11 à S19)

**Tout sprint mobile de S11 à S19 (Phase 2 + Phase 3) reste en mode
mocké** tant que le backend réel visé n'est pas confirmé prêt côté
Moteur/Web pour le sprint concerné.

Le test bout-en-bout Docker du fix S7 (condition qui figurait ici au
17 août) **a été fait, en dehors d'une session Claude Code** — voir §5.1
pour le résultat détaillé. Ne plus le traiter comme "en attente".

**Exception déjà actée** : S15 (sélecteur d'axe, Chauffeur + Client) est
sorti du mode mock — `service-geo` était confirmé prêt par le Moteur
(2 axes réels en base, `GET /api/geo/axes?tenantId=` filtré réellement)
et le câblage a été fait avec accord explicite de l'utilisateur. Ce n'est
pas un modèle à reproduire automatiquement pour les autres sprints — ne
sortir un sprint du mock que sur demande explicite, après confirmation
du backend concerné.

---

## Règle absolue (Git) — à respecter dans toute session

**INTERDICTION FORMELLE de `git merge`, `git push origin dev`, ou toute
action qui modifie directement `dev`.** Toujours : créer une branche →
committer dessus → pousser cette branche → s'arrêter et attendre que
l'utilisateur crée et fasse fusionner la Pull Request lui-même sur GitHub.
Si une tâche semble nécessiter de fusionner dans `dev`, demander
confirmation explicite et attendre une réponse contenant le mot **PULL
REQUEST** avant toute action de merge (ouvrir la PR via `gh pr create` est
en revanche acceptable une fois ce mot reçu).

**⚠️ Pattern récurrent à surveiller (pas un incident isolé)** : le Moteur
(`stevetelecom`) a commité directement sur `dev` sans PR **trois fois** :
`384f168` et `ef71786` (14 août), `a640efe` (17 août — fix sérialisation
Kafka + `proposition-retour-a-vide.yaml`). Signalé fermement par
l'utilisateur après la 3e fois. Ne pas traiter comme un oubli ponctuel :
si un futur changement côté Moteur apparaît dans `dev` sans commit de
merge de PR associé, c'est probablement lui — le signaler explicitement
à l'utilisateur plutôt que de le documenter comme un merge normal.

---

## 1. Objectif du projet

FretCorridor v4 est une marketplace numérique de fret et de colis pour le
Cameroun et la zone CEMAC (Flysoft Engineering SAS). Trois volets :

- **App mobile Client** — publier une demande de transport, suivre sa
  livraison, payer
- **App mobile Chauffeur/Transporteur** — s'authentifier, déclarer sa
  capacité disponible, exécuter des missions, être payé
- **Portail Web** — Bureau de fret (supervision), Transporteur (gestion
  flotte), Administration (KYC, configuration, audit)

Architecture microservices Spring Boot (Java 17), une gateway API en
entrée pour les rôles Web (BUREAU/TRANSPORTEUR/ADMIN — **pas** l'app
Client, cf §2), communication inter-services par Kafka, PostgreSQL/PostGIS,
Redis, MinIO. Phase 1 = Sprints 1 à 10. Phase 2 = Sprints 11, 12, 14, 15
(S13 backend/Web uniquement, rien côté mobile).

---

## 2. État actuel du projet (branche `dev`)

### Back-end — tous les microservices existent et sont avancés

| Service | Rôle | Porteur | État |
|---|---|---|---|
| `service-ida` | Identité, KYC, RBAC | Mobile | ✅ |
| `service-mkt` | Marketplace (demandes client) | Mobile | ✅ Pipeline Kafka complet — `DemandePubliee` publié, `proposition-emise` consommé (voir §3, ancienne "tâche concrète" du 7 août, **résolue**) |
| `service-flt` | Positions GPS | Mobile | ✅ Fix "exempte lookup véhicule interne" mergé (PR #58, débloque S7) |
| `service-exe` | Exécution de mission | Mobile | ✅ Consomme `AffectationConfirmee`, expose `GET /missions/mes`, `POST /missions/{id}/etapes` |
| `service-not` | Notifications in-app | Mobile | ✅ |
| `service-cap` | Capacité (déclaration véhicule) | Mobile | ✅ Dockerfile + docker-compose ajoutés (n'existaient pas au 7 août) |
| `service-geo` | Axes, zonage H3 | Moteur | ✅ `GET /api/geo/axes?tenantId=` filtre réellement en base (ENF-MUL-01, correctif du 2026-08-09) — **2 axes réels actifs en base** (Douala-Yaoundé, Douala-Bafoussam) |
| `service-mat` | Coût composite matching | Moteur | ✅ Avancé |
| `service-opt` | Moteur de matching + séquencement tournées | Moteur | ✅ Avancé — Kuhn-Munkres, + séquencement ALNS (Sprint 11/12), consomme `EtapeExecutee`, publie `PropositionRetourAVideEvent` |
| `service-trk` | Suivi/ETA temps réel | Moteur | ✅ |
| `gateway` | Point d'entrée Web (Bureau/Transporteur/Admin) | Web | ✅ Architecture hexagonale, 2 fixes de ports mergés (service-adm, service-pay — PR #59) |
| `service-pay` | Paiement, grand livre miroir | Web | ✅ |
| `service-bur` | Agrégation Bureau (missions) | Web | ✅ |
| `service-adm` | Back-office (KYC, tenants, config, audit) | Web | ✅ |

### Mobile — les deux apps sont maintenant sur `dev`

- **App Client** (`mobile/app_client/`) — Phase 1 (S1-S9) + Phase 2 (S11
  Volet B, S12 — rien pour Client ce sprint, S14 Volet B, S15 Volet B)
  intégrés dans `dev`. **Pas de gateway unifiée pour ce rôle** : chaque
  écran appelle directement le microservice concerné (auth/KYC →
  service-ida 8081, demandes → service-mkt 8089, notifications →
  service-not 8094, chronologie/position → service-exe 8093 / service-flt
  8092). `lib/providers/dio_provider.dart` expose désormais 5 clients Dio
  nommés (un par service, réapplication du correctif `9c52f02` — PR #64,
  mergée le 17 août) — le `dioProvider` unique/mauvais port n'existe plus.
- **App Chauffeur/Transporteur** (`mobile/app_chauffeur_transporteur/`) —
  Phase 1 (S1-S10) + Phase 2 (S11 Volet A, S12, S14 Volet A, S15 Volet A)
  intégrés dans `dev`. Passe par la gateway (port 8082, chemins
  `/api/v1/...`).

### Web (Angular)

Portails Bureau, Transporteur, Admin — largement construits, tests
unitaires et e2e (Playwright). Pas revérifié depuis le 7 août, pas de
raison de penser que ça a régressé.

---

## 3. Phase 2 (Sprints 11, 12, 14, 15) — état détaillé

Toute la Phase 2 est mergée dans `dev`, **mode mocké par défaut** (le
backend Moteur n'était pas prêt au moment du développement) :

| Sprint | Chauffeur/Transporteur | Client |
|---|---|---|
| S11 — Consolidation LTL | ⚠️ mocké, **bloqué** (voir ci-dessous) | ⚠️ mocké, **bloqué** |
| S12 — Retour à vide | ✅ **branché sur le vrai backend** (PR #76, #77 — 18 août) | — (rien pour Client) |
| S13 | — (backend/Web uniquement) | — |
| S14 — Paiement Mobile Money | ⚠️ mocké, **bloqué** (voir ci-dessous) | ⚠️ mocké, **bloqué** |
| S15 — Second axe | ✅ **branché sur le vrai backend** (PR #56 puis re-câblage réel PR #60) | ✅ **branché sur le vrai backend** (PR #57 puis re-câblage réel PR #61) |

**S12 et S15 sont réels. S11 et S14 sont bloqués, pas par manque de
temps mais parce que la pièce nécessaire n'existe nulle part dans le
dépôt** — voir §5.4/§5.5, chacun nécessite un accord avec une équipe
externe (Moteur pour S11, Web pour S14) avant tout code Mobile.

---

## 4. Contrats Kafka S11/S12 — mergés dans `shared-contracts/` mais pas figés

Depuis le merge des PR #60/#61 (17 août), `dev` contient aussi le travail
du Moteur sur le séquencement ALNS et deux nouveaux contrats :

- **`shared-contracts/asyncapi/events/etape-executee.yaml`** — toujours
  marqué **BROUILLON** dans le fichier lui-même ("à valider avec Mobile
  avant toute implémentation côté EXE"). `missionId` = `Affectation.id`,
  confirmé par le Moteur oralement et dans le fichier. **Ne pas coder en
  dur dessus côté app tant que le Moteur n'a pas donné le feu vert
  explicite** (mot du Moteur attendu, pas juste "c'est sur dev").
- **`shared-contracts/asyncapi/events/proposition-retour-a-vide.yaml`** —
  incohérence du 17 août **résolue et mergée dans `dev`** le 18 août :
  `tourneeId` et `affectationId` sont désormais tous deux `nullable: true`
  et mutuellement exclusifs (documenté explicitement dans le fichier),
  couvrant bien le cas FTL simple (`affectationId` rempli, `tourneeId`
  null) en plus du cas Tournee consolidée (LTL). Version 1.0.0, plus un
  brouillon.
- **Bug de sérialisation transversal (dates OPT en epoch flottant au lieu
  d'ISO-8601)** — trouvé par le Moteur le 17 août en testant le contrat
  ci-dessus, touchait tous les événements publiés par `service-opt`
  (`PropositionEmise`, `AffectationConfirmee`, `PropositionRetourAVide`).
  **Corrigé et mergé** (`backend/service-opt/.../config/KafkaProducerConfig.java`
  — `JavaTimeModule` + `WRITE_DATES_AS_TIMESTAMPS` désactivé). Vérifié :
  aucun impact réel n'avait eu lieu côté Mobile, `AffectationConfirmeeEvent.horodatageConfirmation`
  n'étant utilisé nulle part dans `service-exe`.

**`etape-executee.yaml` reste le seul contrat encore en BROUILLON non
validé** — ne pas coder en dur dessus côté app tant que le Moteur n'a pas
donné le feu vert explicite. `proposition-retour-a-vide.yaml` est
maintenant utilisable pour une implémentation réelle côté S12 (Chauffeur)
si demandé explicitement — voir §5.2 pour ce qui manque encore côté
Mobile avant de pouvoir le faire (aucun consommateur n'existe à ce jour).

---

## 5. Ce qui reste ouvert

### 5.1 Test S7 en Docker — FAIT (18 août), résultat détaillé

**Ne plus traiter ce point comme "en attente"** — le test bout-en-bout
réel (déclaration de capacité via la gateway, deux essais successifs) a
été effectué en dehors d'une session Claude Code. Résultat :

- **Fix de port confirmé fonctionnel** (8092→8083,
  `service-cap/application-docker.yml`) : `transporteur_id` se peuple
  bien en base côté `service-cap` une fois le pool de connexions
  `ServiceFltClient` chaud. Le tout premier appel à froid après un
  `docker-compose up` échoue en timeout (dégradation gracieuse comme
  prévu, ENF-DIS-04) — **corrigé** en allongeant les timeouts
  (`connectTimeoutMs` 200→500, `readTimeoutMs` 300→1000,
  `ServiceFltClientProperties`, PR #71, mergée).
- **Nouveau bug trouvé en poussant le test jusqu'à `service-opt`** :
  `CapaciteDeclareeEvent` a divergé entre les deux copies du contrat.
  La copie côté `service-opt` a deux champs de plus
  (`capaciteResiduelleKg`, `volumeResiduelM3`, ajoutés pour le
  séquencement L2 Phase 2 / EF-CAP-07) que `service-cap` ne publie
  jamais. Conséquence : ces champs arrivent `null` côté `service-opt`,
  violent la contrainte `NOT NULL` en base
  (`capacite_en_attente.capacite_residuelle_kg`, migration V10) et font
  échouer l'insertion à **chaque** déclaration de capacité — y compris le
  flux Phase 1 basique, pas seulement Phase 2. Vérifié dans le code :
  `CapaciteDeclareeListener.ingerer()` (service-opt) attrape
  `DataIntegrityViolationException` de façon trop large et logue
  "CapaciteDeclaree deja ingeree, doublon ignore" — **la capacité est
  donc perdue silencieusement**, sans marquage d'erreur distinct pour la
  retrouver (le message de log est trompeur : ce n'est pas un doublon).
  **Décision du Moteur (18 août)** : fix côté `service-cap` (le Moteur a
  un empêchement réseau) plutôt que nullable côté `service-opt`. **Fait et
  mergé** — `capaciteResiduelleKg`/`volumeResiduelM3` ajoutés à
  `CapaciteDeclareeEvent` (service-cap), valeurs prises sur
  `Capacite.getCapaciteResiduelleKg()`/`getVolumeM3()` (PR #73, mergée).
  Revalidation Docker bout-en-bout abandonnée (réseau trop instable
  pendant la session) — repose sur compilation + tests unitaires
  uniquement pour l'instant.

### 5.2 S12 réel (Chauffeur) — FAIT (18 août)

`proposition-retour-a-vide.yaml` corrigé et mergé, backend construit de
zéro (`service-not` : première consommation Kafka de ce service,
écouteur + résolution transporteur via nouveau `GET
/api/cap/capacites/{id}` côté `service-cap` — PR #76), câblage Flutter +
route gateway (PR #77). **La réponse accepter/refuser reste locale à
`service-not`** : aucun contrat n'existe pour la relayer au Moteur à ce
jour — à revoir si besoin plus tard.

### 5.3 Test Docker bout-en-bout — bloqué (réseau, pas le code)

Tentatives répétées de reconstruire `service-cap` en Docker pour
revalider le fix §5.1 : builds bloqués à répétition (dépendances Maven
téléchargées from scratch dans le conteneur, pas de cache `~/.m2` de
l'hôte — contrairement à `mvn compile`/`mvn test` en local qui
fonctionnent instantanément). Abandonné après plusieurs tentatives sur
plusieurs dizaines de minutes. **Pas un problème de code** — juste pas
revalidé en conditions Docker réelles depuis.

### 5.4 S11 réel (Chauffeur) — bloqué, aucune donnée de tournée exposée

Pas seulement `etape-executee.yaml` (toujours BROUILLON). Vérifié dans
`OptEventPublisher.java` : `service-opt` ne publie que 3 événements
(`proposition-emise`, `affectation-confirmee`, `proposition-retour-a-vide`).
`Tournee`/`EtapeTournee` (séquencement multi-étapes) sont **entièrement
internes** à `service-opt`, jamais exposés sur Kafka —
`AffectationConfirmeeEvent` n'a aucun `tourneeId`. Concrètement,
`service-exe` n'a **aucun moyen** de savoir que plusieurs missions
reçues appartiennent à la même tournée, ni dans quel ordre — pas un
contrat pas encore validé, une donnée qui n'existe nulle part.

**Message envoyé au Moteur (18 août)** : demande d'exposer `tourneeId`
(nullable) + un moyen de connaître le rang/ordre des étapes sur
`AffectationConfirmeeEvent` (même pattern que `proposition-retour-a-vide`).
**En attente de sa réponse — ne rien coder côté S11 avant.**

**Proposition affinée par l'utilisateur (18 août)**, vérifiée solide
contre les documents sources (`docs/CDC_FretCorridor_v4_FSE2026004_lecture.pdf`
§13, `docs/FretCorridor_Plan_Execution_V4_2.docx`) : plutôt qu'un
`tourneeId` isolé, exposer directement une liste d'étapes sur
`AffectationConfirmeeEvent` (`etapes: List<EtapeDto>` — `rang`, `type`,
`demandeId`, point, fenêtre), conforme au modèle CDC où **Mission porte
`n-n Demande`** et où `Étape` est l'entité de rang/point/fenêtre. Le
Plan d'exécution confirme aussi qu'`AffectationConfirmee` *"déclenche
la création de la mission"* — cohérent avec l'idée de porter les
étapes sur ce même événement plutôt qu'un event séparé. **Nuance à
faire trancher par le Moteur** : `AffectationConfirmeeEvent` porte déjà
un `demandeId` unique au niveau racine — ambigu si une tournée groupe
plusieurs demandes, à clarifier (gardé pour le cas FTL simple
seulement, ou déprécié au profit de la liste).

### 5.5 S14 réel (Chauffeur + Client) — bloqué, domaine financier absent

`EcritureMiroir` (service-pay) n'a **aucun champ moyen de paiement**
nulle part dans le domaine — pas un endpoint manquant, le concept
n'existe pas. Plus bloquant côté Client : `PayReadPort`/
`PaiementReadController` (gateway) sont **lecture seule** — le code
documente explicitement *"ENF-FIN-01 : aucune écriture depuis le
mobile"*, règle architecturale délibérée. `POST /missions/{id}/cloture`
(service-pay) n'est appelé par rien dans le dépôt (ni gateway ni
mobile) ; même `ClotureMissionRequest` n'a pas de champ `moyenPaiement`.

Toucherait le domaine financier de `service-pay`, qui appartient à
Personne 2 (Web), pas à Mobile. **Décision utilisateur (18 août) : reste
mocké pour l'instant**, pas de message envoyé à Personne 2 — à
reconsidérer plus tard si besoin.
