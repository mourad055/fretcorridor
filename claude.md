# FretCorridor v4 — Transmission d'état (mise à jour 20 août 2026, nuit)

> **Session de test mobile de bout en bout (20 août, soir/nuit)** — les
> deux apps mobiles (Client, Chauffeur/Transporteur) ont été installées
> et testées sur un téléphone physique (Pixel 6a). Résultats :
>
> **Fonctionnalité ajoutée** : l'app Client n'avait **aucun écran
> d'upload de pièce justificative KYC** — RG-011 exige identité déclarée
> ET pièce déposée pour atteindre NIVEAU_1 (débloquant "publier une
> demande"), mais `completer_profil_screen.dart` ne gérait que la
> première condition et fermait l'écran en laissant croire le profil
> complet. Ajout d'un écran en 2 étapes (identité, pièce photo via
> `image_picker`, même pattern que l'app Chauffeur) — `kyc_provider.dart`
> et `completer_profil_screen.dart` réécrits en conséquence.
>
> **4 bugs réels trouvés en testant le parcours complet publication
> demande → déclaration capacité → matching** (aucun lié aux correctifs
> d'audit précédents, tous pré-existants) :
> 1. Gateway → service-geo : le gateway (conteneur Docker) tentait de
>    résoudre `service-geo` comme un nom de conteneur Docker — cassé dès
>    que service-geo tourne hors conteneur (process hôte). Symptôme :
>    `DnsErrorCauseException: NXDOMAIN`.
> 2. Secret JWT désynchronisé entre le gateway et service-ida : chacun
>    utilisait sa propre valeur par défaut différente en l'absence de
>    `FRETCORRIDOR_JWT_SECRET` — tout jeton service-ida rejeté par le
>    gateway. (Sans effet sur le flux réel de l'app Chauffeur, qui passe
>    par le login **propre** du gateway `/api/v1/auth/login`, pas
>    `/api/auth/login` de service-ida — mais un vrai risque de
>    régression si les deux chemins se recoupent un jour.)
> 3. `com.fretcorridor.gateway.domain.Role.valueOf(...)` plante
>    (`NullPointerException: Name is null`) si un JWT ne porte pas une
>    claim `role` **singulier** — seul le gateway émet ce format ; un
>    jeton service-ida (claim `roles`, pluriel, liste) fait planter
>    `JwtReactiveAuthenticationManager`. Les deux systèmes d'auth (JWT
>    gateway vs JWT service-ida) ne sont **pas interopérables** — à
>    garder en tête si un flux futur mélange les deux.
> 4. service-opt → service-geo : même bug DNS que le gateway (service-opt
>    aussi en conteneur Docker, résout `service-geo:8084` en interne).
> 5. **Perte silencieuse de capacité déclarée** : `capaciteResiduelleKg`
>    arrivait `null` dans l'événement Kafka `CapaciteDeclaree` reçu par
>    service-opt (violation NOT NULL, capacité jamais matchée) — le code
>    source actuel a pourtant déjà un correctif documenté pour exactement
>    ce bug (18 août). Cause : le **conteneur Docker service-cap tournait
>    une image obsolète**, construite avant ce correctif. Résolu en le
>    relançant en process hôte avec le code `dev` actuel (comme fait pour
>    la plupart des services ce soir, cf. note environnement ci-dessous).
>
> **Note environnement (contexte, pas un bug applicatif)** : la stack
> Docker locale (`docker ps`, containers "Up 2 days") était bâtie sur du
> code d'avant la quasi-totalité des correctifs de la nuit. Plutôt que de
> tout reconstruire (risque disque — les deux partitions de cette machine
> sont proches de la saturation), la majorité des 14 microservices ont
> tourné en process `mvn spring-boot:run` sur l'hôte pendant cette
> session, avec quelques variables d'environnement ajustées à la main
> (`SPRING_DATASOURCE_URL` vers le port hôte 5434 de Postgres, `SERVER_PORT`
> pour éviter un conflit avec le gateway sur 8082 côté service-mkt). Le
> gateway et service-opt (restés en conteneurs) ont été recréés avec des
> variables d'environnement pointant vers `172.18.0.1` (passerelle Docker
> bridge) plutôt que les noms de conteneurs Docker absents. **Ceci est un
> contournement de session de test, pas un changement de configuration
> durable** — à refaire proprement (rebuild Docker complet ou tout en
> process hôte) pour la prochaine session.

---

> Document de suivi/handoff, versionné dans le dépôt à la racine. Remplace
> la version du 20 août (fin d'après-midi) : **les 18 bloquants initiaux
> de `AUDIT_CDC_v4_complet_2026-08-19.md` sont désormais tous traités**
> — 17 résolus sans réserve, le 18e (RG-039) traité avec une limitation
> explicitement documentée (voir ci-dessous et §6). **29 PR** au total
> mergées depuis le début de cette passe (#91 à #124, sauf #111).
>
> **Audit de suivi périmètre Mobile reçu des coéquipiers (dev@727410b)
> — vérifié et traité (PR #124, #125)** : 3 constats confirmés réels et
> corrigés — secret JWT non paramétrable (en réalité 5 services
> concernés, pas seulement service-ida comme rapporté) ; refresh token
> contournant le verrouillage de compte (`AuthService.rafraichir()`) ;
> `GET /api/cap/capacites/{id}` public sans aucune vérification (clé
> interne partagée service-not↔service-cap, PR #125 — décision explicite
> de l'utilisateur de le corriger plutôt que de le laisser comme
> service-geo/mat/opt/trk, vérifié conforme CDC/Plan d'Exécution §4.3
> avant de coder). **3 points de ce rapport sont faux/obsolètes** (RG-101,
> RG-070, endpoint véhicule service-flt donnés "toujours ouverts") —
> leur audit a été fait sur un commit ~12 merges avant mes correctifs
> du soir, pas une erreur de leur part. Détail complet §6.
>
> **RG-070 (preuve de livraison) est maintenant fermé de bout en bout** :
> backend (photo + signature tactile, PR #118) **et UI mobile app
> Chauffeur** (écran de capture, PR #122) — le parcours Phase 1→Phase 2
> est de nouveau testable normalement sur téléphone, prise en charge et
> livraison demandent désormais une photo + signature avant de valider.
> Le code SMS (autre mode de validation tiers prévu par le CDC) reste
> hors périmètre — le backend ne le supporte pas (numéro du destinataire
> non propagé jusqu'à service-exe).
>
> **Seule limitation restante, à connaître** : **RG-039** (jusqu'à 3
> propositions ordonnées, PR #120) — rang 2/3 ajoutés (informationnels,
> prix estimé) sans toucher au rang 1 existant. L'endpoint "accepter"
> (service-mkt) marque la proposition choisie, mais **ne déclenche pas
> la réservation réelle de capacité** (`decrementer()` exige le même
> tenant que le transporteur propriétaire — un chargeur d'un autre
> tenant ne peut pas l'appeler sans un pont de confiance cross-tenant
> qui n'existe pas encore).
>
> Cette limitation a été **décidée explicitement avec l'utilisateur**
> après découverte de son ampleur réelle en cours de route (pas un
> oubli) — voir §6 pour le détail complet et les échanges avec le
> collègue Moteur.
>
> **⚠️ Incident corrigé ce soir** : un merge de branche Moteur
> (`backend-stevetelecom`, commit `c564100` "fusion termine") a laissé
> des **marqueurs de conflit Git non résolus commités directement sur
> `dev`** dans ce fichier — corrigé immédiatement en gardant la version
> la plus à jour (celle-ci). Si un autre fichier affiche un
> comportement bizarre après un merge Moteur récent, vérifier d'abord
> l'absence de `<<<<<<<`/`=======`/`>>>>>>>` avant de chercher plus
> loin.

---

## Prérequis avant toute intégration réelle (S11 à S19)

**Tout sprint mobile de S11 à S19 (Phase 2 + Phase 3) reste en mode
mocké** tant que le backend réel visé n'est pas confirmé prêt côté
Moteur/Web pour le sprint concerné.

Le test bout-en-bout Docker du fix S7 (condition qui figurait ici au
17 août) **a été fait, en dehors d'une session Claude Code** — voir §5.1
pour le résultat détaillé. Ne plus le traiter comme "en attente".

**Exceptions actées — toute la Phase 2 en fait partie désormais** :
- **S15** (sélecteur d'axe, Chauffeur + Client) — `service-geo` confirmé
  prêt par le Moteur (2 axes réels en base, filtrage réel), câblé avec
  accord explicite de l'utilisateur.
- **S11** (tournée multi-étapes, Chauffeur) — le Moteur a construit et
  testé `TourneeConstituee` (20 août), câblé de bout en bout côté Mobile
  le jour même. Voir §5.4 pour le détail (solution différente de ce qui
  était envisagé le 18 août).
- **S12** (retour à vide, Chauffeur) — déjà réel depuis le 18 août, mais
  un gap silencieux (`etape-executee` sans producteur) empêchait le
  déclenchement effectif ; corrigé le 20 août (PR #85). Voir §5.2.
- **S14** (moyen de paiement, Chauffeur + Client) — backend Item B livré
  par Web le 18 août, gateway + mobile câblés le 20 (PR #87). Espèces
  reste une exception assumée (confirmation locale, jamais envoyée au
  backend) — voir §5.5, pas un oubli.

Ce n'est pas un modèle à reproduire automatiquement pour les **prochains**
sprints (S16 à S19, Phase 3) — ne sortir un sprint du mock que sur
demande explicite, après confirmation du backend concerné.

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

**Protocole confirmé en usage (20 août)** : branche `backend-stevetelecom`
poussée en PR (#81) plutôt que commitée directement cette fois — bon
signe. Le mot **PULL REQUEST** a été demandé et reçu avant chaque merge
(#81 à #104, plus de 20 PR consécutives au total sur la journée). Sur
#87, `gh pr checks` a d'abord montré des checks `pending` (gateway) :
attendre qu'ils passent avant de merger plutôt que de merger sur un
statut incomplet — quelques cycles de sondage (~10s) ont suffi.

**`gh pr merge` bloqué par le classificateur auto-mode — résolu (20
août, nuit)** : la commande était refusée par le classificateur malgré
le mot PULL REQUEST reçu. Cause : aucune règle de permission Bash
explicite pour `gh pr merge`/`gh pr create`/`gh pr checks` dans
`.claude/settings.local.json`. **Corrigé en ajoutant ces trois patterns
à `permissions.allow`** (avec l'accord explicite de l'utilisateur, qui a
choisi cette option plutôt que la fusion manuelle) — `gh pr merge`
fonctionne normalement depuis. Si le blocage réapparaît malgré la règle
présente, c'est un signal différent (mode d'exécution de la session, pas
un problème de settings) — rendre la main à l'utilisateur dans ce cas
précis, jamais chercher à contourner autrement.

**Fusions de PR dans la même branche : conflits attendus, pas une
anomalie.** Sur cette session, plusieurs correctifs indépendants
touchaient les mêmes fichiers (ex. `AdmPort`/`PayReadPort` côté gateway,
modifiés par PR #98, #100, #101, #102 quasi simultanément) — chaque
`gh pr merge` suivant a produit `GraphQL: Pull Request has merge
conflicts`. Résolution systématique : `git fetch origin <branche
source>`, checkout de cette branche, `git merge origin/dev`, résoudre
(généralement additif — combiner les deux signatures de méthode élargies
plutôt que choisir un côté), recompiler + retester le module concerné,
push, puis `gh pr merge` repasse.

**Contention de ressources entre suites Testcontainers lancées en
parallèle** : deux `mvn test` simultanés (ex. gateway + service-bur, deux
suites Testcontainers/PostgreSQL) ont produit des échecs qui
ressemblaient à de vraies régressions (timeouts 5s, crash de fork JVM
"VM terminated without properly saying goodbye"). Confirmé non-lié en
réexécutant chaque suite **séquentiellement** — toujours revalider
isolément avant de conclure à une régression quand des tests tournent en
parallèle dans le même sandbox.

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
| `service-exe` | Exécution de mission | Mobile | ✅ Consomme `AffectationConfirmee` + `TourneeConstituee` (20 août), expose `GET /missions/mes`, `POST /missions/{id}/etapes`, `GET /missions/tournees/{id}`, publie `MissionLivree` + `EtapeExecutee` (20 août) |
| `service-not` | Notifications in-app | Mobile | ✅ |
| `service-cap` | Capacité (déclaration véhicule) | Mobile | ✅ Dockerfile + docker-compose ajoutés (n'existaient pas au 7 août) |
| `service-geo` | Axes, zonage H3 | Moteur | ✅ `GET /api/geo/axes?tenantId=` filtre réellement en base (ENF-MUL-01, correctif du 2026-08-09) — **2 axes réels actifs en base** (Douala-Yaoundé, Douala-Bafoussam) |
| `service-mat` | Coût composite matching | Moteur | ✅ Avancé |
| `service-opt` | Moteur de matching + séquencement tournées | Moteur | ✅ Avancé — Kuhn-Munkres, + séquencement ALNS (Sprint 11/12), consomme `EtapeExecutee` (producteur réel côté `service-exe` depuis le 20 août), publie `PropositionRetourAVideEvent`, `TourneeConstituee` (S11, 20 août), oracle chargement S16 |
| `service-trk` | Suivi/ETA temps réel | Moteur | ✅ |
| `gateway` | Point d'entrée Web (Bureau/Transporteur/Admin) | Web | ✅ Architecture hexagonale, 2 fixes de ports mergés (service-adm, service-pay — PR #59) |
| `service-pay` | Paiement, grand livre miroir | Web | ✅ `ModePaiementChoisi` (S14 Item B, 18 août) : `POST`/`GET .../moyen-paiement`, proxifié par la gateway (Chauffeur) et appelé directement par l'app Client (20 août, voir §5.5) |
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

Toute la Phase 2 est mergée dans `dev` :

| Sprint | Chauffeur/Transporteur | Client |
|---|---|---|
| S11 — Consolidation LTL | ✅ **branché sur le vrai backend** (PR #81/#82/#84 — 20 août) | ✅ **branché sur le vrai backend** (PR #89 — 20 août) |
| S12 — Retour à vide | ✅ **branché sur le vrai backend, chaîne complète** (PR #76/#77 — 18 août, gap `etape-executee` comblé PR #85 — 20 août) | — (rien pour Client) |
| S13 | — (backend/Web uniquement) | — |
| S14 — Paiement Mobile Money | ✅ **branché sur le vrai backend** (Item B 18 août, gateway+mobile PR #87 — 20 août) | ✅ **branché sur le vrai backend pour MoMo/Orange Money** (PR #87) — Espèces reste local, exception assumée (§5.5) |
| S15 — Second axe | ✅ **branché sur le vrai backend** (PR #56 puis re-câblage réel PR #60) | ✅ **branché sur le vrai backend** (PR #57 puis re-câblage réel PR #61) |

**Toute la Phase 2 (S11, S12, S14, S15) est réelle de bout en bout,
Chauffeur et Client, depuis le 20 août au soir.** Le dernier volet mocké
(S11 Volet B, indicateur "envoi consolidé" côté Client) a fermé sans
aucun changement backend : `tourneeId` était déjà exposé par
`service-exe` sur l'endpoint chronologie du Client depuis la PR #82,
juste jamais lu côté app (PR #89). Seule exception assumée restante :
Espèces (S14 Client) confirmé localement, jamais envoyé au backend — un
choix de conception documenté en §5.5, pas un gap.

---

## 4. Contrats Kafka S11/S12 — mergés dans `shared-contracts/`, deux encore en BROUILLON

- **`shared-contracts/asyncapi/events/etape-executee.yaml`** — toujours
  marqué **BROUILLON** dans le fichier lui-même, mais **le producteur
  existe et fonctionne désormais** (`service-exe`, PR #85, 20 août) :
  publié à chaque `PRISE_EN_CHARGE`/`LIVRAISON` confirmée par le
  chauffeur. `missionId` = `Affectation.id`, confirmé. Le statut BROUILLON
  du fichier n'a donc plus valeur de blocage — implémentation faite en
  écrivant volontairement une copie locale du contrat côté `service-exe`
  (tolérante à une évolution ultérieure), pas en important le fichier.
- **`shared-contracts/asyncapi/events/tournee-constituee.yaml`** (nouveau,
  20 août, PR #81) — même statut BROUILLON assumé, même approche : copie
  locale côté `service-exe`/gateway, pas d'import direct. Publié par
  `service-opt` uniquement pour une Tournée LTL consolidée.
- **`shared-contracts/asyncapi/events/proposition-retour-a-vide.yaml`** —
  résolu et mergé le 18 août : `tourneeId`/`affectationId` nullable et
  mutuellement exclusifs, couvre FTL simple et LTL consolidé. Version
  1.0.0, plus un brouillon.
- **Bug de sérialisation transversal (dates OPT en epoch flottant)** —
  trouvé et corrigé le 17 août (`JavaTimeModule` + `WRITE_DATES_AS_TIMESTAMPS`
  désactivé, `service-opt`). Sans impact réel côté Mobile.

**Aucun contrat ne bloque plus S11/S12 aujourd'hui** — les deux
BROUILLON (`etape-executee`, `tournee-constituee`) ont un producteur et
un consommateur réels et fonctionnels malgré leur statut de fichier ; à
faire valider formellement au prochain point de synchro hebdo Moteur/Mobile,
sans urgence bloquante.

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

### 5.2 S12 réel (Chauffeur) — FAIT (18 août), chaîne complétée (20 août)

`proposition-retour-a-vide.yaml` corrigé et mergé, backend construit de
zéro (`service-not` : première consommation Kafka de ce service,
écouteur + résolution transporteur via nouveau `GET
/api/cap/capacites/{id}` côté `service-cap` — PR #76), câblage Flutter +
route gateway (PR #77). **La réponse accepter/refuser reste locale à
`service-not`** : aucun contrat n'existe pour la relayer au Moteur à ce
jour — à revoir si besoin plus tard.

**Gap trouvé par l'audit CDC du 19 août, corrigé le 20 (PR #85)** : toute
cette chaîne (`service-opt` → `service-not` → Mobile) était réelle et
mergée, mais **jamais déclenchée en pratique**. `EtapeExecuteeListener`
(`service-opt`) attend l'événement `etape-executee` pour figer l'exécuté
(EF-MAT-09) et appeler `proposerRetourAVide` — **aucun producteur
n'existait nulle part dans le dépôt**. `service-exe.MissionService.ajouterEtape()`
publie désormais cet événement à chaque `PRISE_EN_CHARGE`
(→`ENLEVEMENT`)/`LIVRAISON` confirmée. La leçon : un "branché sur le vrai
backend" mergé et testé unitairement peut quand même rester mort en
pratique si un maillon Kafka intermédiaire n'a pas de producteur —
vérifier la chaîne complète, pas seulement chaque bout séparément.

### 5.3 Test Docker bout-en-bout — bloqué (réseau, pas le code)

Tentatives répétées de reconstruire `service-cap` en Docker pour
revalider le fix §5.1 : builds bloqués à répétition (dépendances Maven
téléchargées from scratch dans le conteneur, pas de cache `~/.m2` de
l'hôte — contrairement à `mvn compile`/`mvn test` en local qui
fonctionnent instantanément). Abandonné après plusieurs tentatives sur
plusieurs dizaines de minutes. **Pas un problème de code** — juste pas
revalidé en conditions Docker réelles depuis.

### 5.4 S11 réel (Chauffeur) — FAIT (20 août)

Le blocage du 18 août (aucune donnée de tournée exposée par
`service-opt`) est levé, mais **pas par la voie envisagée à l'époque**
(`tourneeId`/liste d'étapes portée par `AffectationConfirmeeEvent`) — le
Moteur a tranché pour un **événement séparé**, `TourneeConstituee`,
publié uniquement quand une Tournée LTL consolidée est confirmée (jamais
pour une affectation FTL simple, qui reste entièrement décrite par son
propre `AffectationConfirmeeEvent`). `missionId` par étape = même UUID
qu'`AffectationConfirmeeEvent.missionId` — clé de corrélation avec les
Missions déjà créées côté `service-exe`.

Écart de modélisation assumé par le Moteur (à noter, pas à corriger) :
le CDC §13 prévoit une Mission unique portant plusieurs Étapes liées à
plusieurs Demandes ; côté implémentation réelle, chaque Affectation
génère sa propre Mission — `TourneeConstituee` les regroupe *a
posteriori* sous un `tourneeId` commun plutôt que de fusionner les
entités Mission. Le Moteur suggère qu'un ADR documente ce choix côté
`service-exe` si jugé structurant — pas fait à ce jour.

**Chaîne complète, mergée le 20 août** :
- `service-opt` publie `TourneeConstituee` (PR #81).
- `service-exe` la consomme (`TourneeConstitueeListener`), persiste
  l'ordre planifié (`EtapeTournee`), rattache les Missions à leur
  `tourneeId`, expose `GET /missions/tournees/{tourneeId}` (PR #82).
- La gateway proxifie vers `GET /api/v1/missions/tournees/{tourneeId}`
  (PR #82).
- L'app Chauffeur consomme cet endpoint réel, navigation depuis "Mes
  missions" (bandeau sur toute mission dont `tourneeId != null`) — plus
  d'écran "démo" (PR #84).

Contrat `tournee-constituee.yaml` toujours marqué BROUILLON dans le
fichier — voir §4, pas bloquant en pratique.

### 5.5 S14 réel (Chauffeur + Client) — FAIT (20 août)

Le blocage du 18 août ("le concept moyen de paiement n'existe nulle
part dans le domaine service-pay") avait été levé côté Web dès le 18 :
Item B (`docs/DEPENDANCES_MOBILE_PHASE4.md`) livré par Personne 2 —
`ModePaiementChoisi` (un choix par mission, "moyen choisi/prévu" distinct
du "moyen effectivement encaissé", volontairement non recoupés),
`POST`/`GET /api/v1/pay/missions/{id}/moyen-paiement`. Gateway et mobile
sont restés mockés jusqu'au 20 août (PR #87) — ce qui suit décrit ce lot.

**Gateway (Chauffeur, lecture seule)** : `PayReadPort.modePaiementChoisi()`
+ `ServicePayWebClientAdapter`, `GET /api/v1/paiement/missions/{id}/moyen-paiement`
(`PaiementReadController`) — ouvert à tout acteur authentifié (même
raisonnement que `/api/v1/paiement` déjà existant, sinon `CHAUFFEUR`
serait exclu). 404 (rien choisi) mappé sur `MissionIntrouvableException`,
déjà géré globalement.

**App Client (écrit)** : **aucune route gateway** — l'app Client appelle
`service-pay` directement (port 8088), même principe architectural que
tous ses autres providers (`dio_provider.dart` documente l'absence de
gateway unifiée pour le rôle Chargeur depuis le S6/S7). Nouveau
`payDioProvider`.

**Décision de conception notable, à connaître avant de toucher à cet
écran** : MoMo et Orange Money envoient tous deux `MONNAIE_ELECTRONIQUE`
(seule granularité connue de `ModePaiement`, 4 valeurs). **Espèces
n'appelle jamais le backend** — confirmation purement locale. Ce n'est
pas un oubli : le commentaire du commit service-pay Item B est explicite,
"espèces (EF-PAY-07) explicitement hors périmètre — mode dégradé décidé
à l'enlèvement, jamais choisi en amont dans l'app". Le mélange
réel/local sur un même écran est donc voulu, pas un bug à corriger.

**Écrans mobiles rattachés à une vraie mission** (fini le mode "démo"
sans contexte, même principe que S11/§5.4) : `PaiementScreen` (Client)
prend désormais un `missionId`, accessible depuis "Suivi"
(`suivi_screen.dart`, à côté de "Signaler un litige") — l'entrée
générique de l'accueil a été retirée. Côté Chauffeur, l'écran solde/gains
affiche les 4 valeurs réelles de `ModePaiement` (pas de distinction
MoMo/Orange Money, contrairement à ce que supposait le mock qu'il
remplace) ; `lib/mock/moyen_reglement_mock.dart` supprimé.

### 5.6 S11 Volet B (Client) — FAIT (20 août), zéro changement backend

Dernier volet mocké de toute la Phase 2. `tourneeId` (nullable) était en
fait déjà exposé sur `GET /missions/demande/{demandeId}/chronologie` —
le même champ ajouté à `ChronologieResponse` côté `service-exe` pour le
Volet A Chauffeur (PR #82), qui se trouve être le même endpoint que
consomme déjà l'écran de suivi Client pour sa propre chronologie. Personne
ne l'avait remarqué avant de vérifier : `ChronologieModel` (Client) ne
lisait simplement pas ce champ. Ajouté (`chronologie_model.dart`), le
bandeau "envoi groupé" (`suivi_screen.dart`) se base maintenant dessus au
lieu du mock déterministe sur `missionId.hashCode`. `lib/mock/consolidation_mock.dart`
supprimé (PR #89).

---

## 6. Suivi de l'audit CDC du 19 août — ce qui a été corrigé, ce qui reste ouvert

`AUDIT_CDC_v4_complet_2026-08-19.md` (racine du dépôt) reste la référence
complète : 18 bloquants, 35 majeurs, 29 mineurs, ~50 % de conformité CDC
globale au moment de sa rédaction. **Une passe de correction systématique
a eu lieu dans la nuit puis le matin du 20 août**, à la demande explicite
de l'utilisateur ("corrige les bugs bloquants restants de l'audit"),
service par service, dans l'ordre le plus sûr (mécaniques d'abord, puis
authentification, puis confiance mal placée dans le corps de requête).
**19 PR** (#91 à #110, plus #112 — #111 n'est pas de cette session) ont
chacune suivi la discipline habituelle : branche dédiée, compilation +
tests avant commit, mot **PULL REQUEST** explicite avant merge.

### Corrigés et mergés dans `dev` (bloquants/majeurs mécaniques + authentification, PR #91→#105)

| # | Service(s) | Correctif |
|---|---|---|
| #91 | `service-not` | IDOR sur `/notifications/{id}/lue` et `/repondre` — vérification tenant ajoutée |
| #92 | `service-ida` | Activation d'enrôlement agent rendue accessible (`permitAll` explicite sur l'endpoint, sans compte encore créé) |
| #93 | `service-cap` | Poids taxable — 3e terme LDM ajouté (RG-100) |
| #94 | `service-cap` | Perte d'écriture sous concurrence (EF-CAP-07) — **bug trouvé en cours de route, hors liste initiale de l'audit** ; savepoint JDBC natif remplace `REQUIRES_NEW` (qui commitait l'idempotency-log même quand le decrement échouait) |
| #95 | `service-cap`, `gateway` | Authentification JWT + IDOR sur `POST /decrement` ; `tenantId` ajouté à `Capacite` (migration V3) ; gateway transmet désormais le `delegationToken` |
| #96 | `service-exe` | Précédence des étapes imposée (RG-062/070) — `LIVRAISON` sans `PRISE_EN_CHARGE` préalable désormais rejetée (`ETAPE_HORS_SEQUENCE`) ; un test existant qui codifiait le bug a été corrigé, pas juste complété |
| #97 | `service-flt` | Contrainte d'unicité sur l'immatriculation (RG-088) |
| #98 | `service-mkt`, `service-geo` | Pipeline marketplace → matching mort (§1.2) — `DemandeService.publier()` ne renseignait jamais `axeId`/`valeursCriteres` ; nouveau `ServiceGeoClient` résout l'axe par nom de ville. **Côté `service-geo` (Java 21, Moteur) non compilé localement** (sandbox limité à Java 17, voir note ci-dessous) — revu manuellement champ par champ |
| #99 | `service-exe`, `service-flt`, `service-not` | Canaux Kafka morts `position-brute`/`alerte-ecart` (§7.1) fermés — `Mission.vehiculeId` ajouté (peuplé depuis `AffectationConfirmeeEvent`, jamais persisté avant), `service-flt` publie désormais `position-brute`, `service-not` consomme `alerte-ecart` |
| #100 | `service-pay`, `gateway` | Authentification JWT — analyse des appelants réels faite **avant** implémentation (discutée avec l'utilisateur, service financier sensible) : seul `/moyen-paiement` avait un vrai appelant (app Client, envoie déjà un token) ; `/webhooks/**` reste `permitAll` (signature HMAC, pas de JWT possible côté prestataire externe) |
| #101 | `service-adm`, `gateway` | Authentification JWT — dernier des « 8 services sans authentification » côté Mobile/Moteur/Web restants après #95/#100 |
| #102 | `service-bur`, `gateway` | Authentification JWT — dernier des 4 services **Web** (gateway/pay/adm/bur) sans authentification. `OptPort`/`TrkPort` (gateway) transmettent désormais le token vers `service-bur`, qu'ils appellent en réalité malgré leur nom (`ServiceBurMissionAppparieeAdapter`/`ServiceBurPositionAdapter`) |
| #103 | `service-adm` | IDOR sur `GET /api/v1/dossiers/{id}` (§7.2) — `FileTravailService.consulter()` vérifie désormais le tenant, même exception "introuvable" pour les deux cas |
| #104 | `app_chauffeur_transporteur` | File locale pour le suivi GPS hors ligne — même patron que l'enrôlement agent (`flutter_secure_storage` + retry à la reconnexion) |
| #105 | `web` (Angular) | `environment.development.ts` pointait vers `service-pay` (8088) au lieu de la gateway (8082) (majeur, §4 de l'audit) — poussée après une coupure réseau locale (`Destination Net Unreachable`, pas un problème GitHub) |

**Bonus incident, PR #94** : découverte en cours de route, pas dans la
liste initiale de l'audit — l'utilisateur, consulté explicitement, a
choisi de la traiter immédiatement plutôt que de la différer.

### `tenantId`/`acteurId` lus du corps de requête plutôt que du JWT — traité (matin du 20 août, PR #107→#110)

Point laissé explicitement ouvert la nuit précédente (voir version
antérieure de ce document) : l'authentification (#100-#102) vérifiait
qu'un JWT valide était présent, mais pas que le `tenantId`/`acteurId`
porté par le corps de la requête correspondait bien à celui du JWT — un
acteur authentifié pouvait agir pour n'importe quel tenant en le
choisissant simplement dans le payload. Traité à la demande explicite de
l'utilisateur ("attaque le tenantId lu du corps de requête").

| # | Service(s) | Correctif |
|---|---|---|
| #107 | `service-pay` | `PaiementController` (cloture, confirmerLivraison, souscrireGarantie, choisirModePaiement, declarerPaiementEspeces, reversement) : tenantId/acteurId extraits du JWT. `rapportTenant`/`paiementsEspecesTenant`/`ecrituresTransporteur` : IDOR corrigé (403 `AccesRefuseException` cross-tenant, override `ADMINISTRATION` conservé, même principe que `rapportFinancierAdmin` déjà existant) |
| #108 | `service-adm` | `DossierController` (ouvrir/prise-en-charge/decision) + `TenantController.creer` : même traitement. `fileDeTravail` (GET) : IDOR corrigé (403 cross-tenant, override `ADMINISTRATION`) — comble aussi le point "IDOR sur les endpoints de mutation service-adm" resté ouvert la nuit précédente |
| #109 | `service-adm` | `JournalAuditController` : comble le point "export du journal d'audit cross-tenant si tenantId omis" resté ouvert la nuit précédente — sans `ADMINISTRATION`, tenantId omis retombe désormais sur celui du JWT (plus "tous les tenants") ; tenantId différent du JWT → 403 |
| #110 | `service-bur` | **Constat étendu, hors des 18 bloquants de l'audit initial** : en vérifiant systématiquement les autres services pour la même classe de bug, les 4 contrôleurs REST internes (`AlerteSeuilController`, `MissionAppparieeController`, `PositionController`, `BureauAgregatController`) se sont révélés avoir le même problème, sous une docstring trompeuse ("pas de RBAC ici, le gateway filtre déjà par tenant") — le `SecurityConfig` exigeait bien un JWT valide, mais ne vérifiait jamais que son tenantId correspondait à celui du corps/query. Même traitement (tenantId/acteurId du JWT). Le gateway n'a pas eu besoin d'être modifié (il calculait déjà les bonnes valeurs, juste envoyées à un endroit qui ne les vérifiait pas) |

Pattern de fix commun aux 4 PR : extraction via
`@RequestHeader("Authorization") String authHeader` +
`jwtService.extraireTenantId(...)`/`extraireActeurId(...)`, champ
correspondant retiré du DTO de requête (Jackson/Spring ignorent
silencieusement un champ ou paramètre inconnu — vérifié empiriquement,
aucun appelant réel cassé). Tests d'intégration : pattern
`token(tenantId)` / `token(tenantId, acteurId)` (JWT dont le claim
correspond à ce que le test doit vérifier), répliqué identiquement sur
les 4 PR.

### Derniers correctifs de l'après-midi (PR #114, #115)

| # | Service(s) | Correctif |
|---|---|---|
| #114 | `gateway` | ENF-SEC-02 : `DossierController.consolide()` ne journalisait aucune consultation de dossier. `admPort.enregistrerAudit(tenantId, acteurId, "CONSULTATION_DOSSIER_DETAIL", "dossier:"+dossierId, ...)` appelé avant la lecture, même pattern exact que `MissionAppparieeController.detail()`/`PaiementReadController` |
| #115 | `service-flt`, `service-cap` | Bloquant audit §3 "endpoint véhicule public, sans filtre tenant" : `GET /api/flt/vehicules/{id}` était `permitAll()` sans aucune vérification (n'importe qui pouvait lire n'importe quel véhicule de n'importe quel tenant). `service-cap` (`ServiceFltClient`, seul appelant légitime, jamais via le gateway) transmet désormais son propre JWT ; `VehiculeController.consulter` filtre sur le tenantId du JWT (404 si mismatch, même principe que `DossierController` service-adm) |

Ces deux PR n'ont aucune CI (service-flt/service-cap et gateway sur ce
chemin ne sont pas dans le scope `backend-web-scope.yml` pour flt/cap —
vérifié localement par `mvn test` sur les modules concernés avant merge).

### RG-101 — coefficient volumétrique par tenant/axe (PR #117)

**3e bloquant manqué dans le décompte initial** (18 au total, seulement
16-17 nommément détaillés dans les versions précédentes de ce document) :
"Coefficient volumétrique global, non scopé tenant/axe" (§5.2 de
l'audit) — `CalculateurPoidsTaxable` (RHO/LAMBDA) lisait une seule
valeur dans `application.yml` pour tout le système. Distinct du bug
RG-100 déjà corrigé en PR #93 (3e terme LDM manquant, même fichier).

Fix : nouveau `ServiceGeoClient` côté service-cap résout
`Axe.parametres` (clés `coefficientVolumetriqueKgParM3`/
`coefficientPlancherKgParLdm`, même mécanisme que
`detourMaxDistanceKm`/EF-MAT-10) — un axe appartenant à exactement un
tenant, scoper par axe scope aussi par tenant. Repli sur la référence
globale (`application.yml`) si absent/injoignable (ENF-DIS-04).
Nouveau `CalculateurPoidsTaxableTest` (aucun test dédié n'existait
avant). Vérifié : `mvn -o test` service-cap, 6 tests, 0 échec.

### RG-070 — preuve d'enlèvement/livraison (PR #118 backend + #122 mobile — fermé de bout en bout)

Décision explicite de l'utilisateur ("signature tactile seule pour
l'instant") après découverte que le code SMS (autre mode de validation
tiers prévu par le CDC, UC-EXE-03) nécessiterait de faire traverser
`destinataireTelephone` à travers 3 services (mkt→opt→exe, changement
de contrat Kafka partagé avec le Moteur — signalé à sa session, pas
fait). **Ce point (code SMS) reste seul hors périmètre.**

**Backend (PR #118)** : `service-exe` refuse désormais toute
`PRISE_EN_CHARGE`/`LIVRAISON` sans au moins une photo ET une signature
tactile (`PREUVE_MANQUANTE`, nouveau endpoint multipart sur
`POST /api/missions/{id}/etapes`, différencié de l'ancien JSON par
`consumes`). Stockage MinIO + empreinte SHA-256 par photo
(`PreuveEtape`, RG-072/EF-EXE-05, immuable par construction). Le
gateway (WebFlux) reconstruit un multipart réactif vers service-exe
(`MissionExecutionPort.ajouterEtapeAvecPreuve`). EN_TRANSIT/INCIDENT
ne sont pas concernés (JSON existant inchangé pour ces deux types).

**Mobile, app Chauffeur/Transporteur (PR #122)** : nouveau
`SignaturePad` (pad tactile dessiné à la main, `CustomPainter` +
`RenderRepaintBoundary`, sans nouvelle dépendance pub.dev) +
`_FormulairePreuve` (`mission_detail_screen.dart`, même patron que
`_FormulaireIncident` déjà existant) — jusqu'à 3 photos
(`image_picker`, déjà une dépendance) + signature obligatoires avant
de valider PRISE_EN_CHARGE/LIVRAISON. `MissionNotifier
.ajouterEtapeAvecPreuve` (FormData multipart Dio) en plus de
l'`ajouterEtape` JSON existant (conservé pour EN_TRANSIT/INCIDENT).

**Le parcours de test Phase 1→Phase 2 sur téléphone fonctionne de
nouveau normalement** — prise en charge et livraison demandent
désormais une photo + signature à l'écran, tout le reste est inchangé.

Vérifié : `mvn -o test` service-exe (12 tests) + gateway (184 tests) —
backend. `flutter analyze` (0 issue) + `flutter build apk --debug`
(compilation réelle réussie) — mobile, aucun test Flutter n'existait
avant pour cet écran.

### RG-039 — jusqu'à 3 propositions ordonnées (PR #120)

**18e et dernier bloquant, traité en soirée.** Avant de coder, investigation
qui a révélé une portée bien plus large que "générer un top-3" :
`AffectationL1Service` (Kuhn-Munkres) **committe directement** une
`Affectation`/Mission réelle pour le seul match optimal, et **aucun appel**
à `POST /api/cap/capacites/{id}/decrement` n'existe nulle part dans le
pipeline de matching -- EF-MKT-08 ("réservation atomique de la capacité"
à l'acceptation du chargeur) n'était donc pas câblé du tout, pas
seulement le classement top-3. Signalé au collègue Moteur (propriétaire
d'`AffectationL1Service`) avant toute modification.

Décision utilisateur ("Backend complet : top-3 + endpoint accepter") :

- **service-opt** (`AffectationL1Service`) : rang 1 **inchangé**
  (toujours le pick Kuhn-Munkres, `Affectation`+`AffectationConfirmee`
  publiés exactement comme avant -- zéro impact sur service-exe/tournées
  déjà en prod). Rang 2/3 **ajoutés** (`publierAlternatives`) : jusqu'à 2
  alternatives par coût croissant sur la même ligne de la matrice de
  cette demande, purement informationnelles (aucune `Affectation`
  créée, prix estimé non ferme au sens RG-041). "Au plus trois", jamais
  forcé si moins de candidats disponibles.
- **service-mkt** : `Proposition.statut` (EN_ATTENTE/ACCEPTEE/EXPIREE),
  nouveau `POST /api/demandes/{id}/propositions/{propositionId}/accepter`
  qui marque la proposition choisie et expire les autres de la même
  demande.

**Limitation assumée et documentée** : l'endpoint "accepter" ne
déclenche **pas** la réservation réelle de capacité.
`CapaciteService.decrementer()` exige que l'appelant soit du **même
tenant** que le propriétaire de la capacité (IDOR corrigé en PR #95) --
un chargeur (tenant différent du transporteur) qui accepte ne peut donc
pas l'appeler directement sans un pont de confiance cross-tenant qui
n'existe pas aujourd'hui. Construire ce pont était hors périmètre de
cette session -- **à traiter séparément si EF-MKT-08 doit être fermé
complètement**.

Tests : `AffectationL1ServiceTest` (nouveau, aucun test n'existait avant
pour cette classe) -- classement rang 1/2/3 par coût, un seul candidat
ne produit qu'une seule proposition. `DemandeServiceTest` -- accepter
marque bien les autres EXPIREE, refuse une proposition déjà traitée.
Vérifié : `mvn -o test` service-opt (9 tests, Java 21) + service-mkt (4
tests), 0 échec.

### Audit de suivi périmètre Mobile (coéquipiers, dev@727410b) — PR #124, #125

Rapport reçu en soirée, vérifié point par point contre le code réel de
`dev` (jamais de confiance aveugle, ni dans mes propres PR ni dans un
rapport externe) avant d'agir.

**3 constats confirmés réels, tous absents des 18 bloquants originaux :**

- **Secret JWT non paramétrable via variable d'environnement** (PR
  #124) — le rapport ne citait que `service-ida`, vérification élargie
  à tout le dépôt : en réalité **5 services** concernés
  (`service-ida`, `service-exe`, `service-flt`, `service-mkt`,
  `service-not`), tous alignés sur `${FRETCORRIDOR_JWT_SECRET:...}`.
- **`AuthService.rafraichir()` (service-ida) ne vérifiait jamais
  `acteur.getActif()`** (PR #124), contrairement à `login()` — un
  compte verrouillé après 3 échecs de PIN pouvait continuer à
  rafraîchir son token indéfiniment. Même garde ajoutée. Nouveau
  `AuthServiceTest` (aucun test n'existait avant pour cette classe).
- **`GET /api/cap/capacites/{id}` totalement public** (PR #125) —
  d'abord laissé de côté (même architecture que service-geo/mat/opt/trk,
  appelant Kafka sans JWT disponible), puis **corrigé sur demande
  explicite de l'utilisateur** ("corrige une fois c'est pas mieux ?").
  Vérifié conforme CDC/Plan d'Exécution avant de coder : §4.3 du Plan
  d'Exécution autorise explicitement l'appel synchrone entre
  service-not et service-cap (même porteur Mobile) ; ENF-SEC-05
  ("secrets centralisés... rotation périodique") couvre la nouvelle clé.
  Fix : clé interne partagée (`X-Internal-Service-Key`,
  `fretcorridor.internal.service-key`, même mécanisme de rotation par
  variable d'environnement que le secret JWT) -- `ServiceCapClient`
  (service-not) l'envoie, `CapaciteController.obtenir` (service-cap) la
  vérifie, 401 si absente/incorrecte. `SecurityConfig` reste
  `permitAll()` au niveau Spring Security (rien ne change à cette
  couche, la clé est vérifiée au niveau du contrôleur) -- documenté
  explicitement pour ne pas reproduire l'ancien raisonnement "permitAll
  = pas de contrôle". Nouveau `CapaciteControllerTest` (aucun test HTTP
  n'existait avant pour ce contrôleur, comme relevé par le rapport).

**1 point du rapport confirmé réel, resté hors périmètre** : absence de
tests `@SpringBootTest`/`MockMvc` sur `service-ida`/`service-cap` au-delà
de ce qui a été ajouté ci-dessus -- construire une vraie suite
HTTP/Spring Security complète pour ces deux modules serait un chantier
à part, pas traité cette nuit.

**3 points du rapport vérifiés et FAUX/obsolètes** : RG-101, RG-070, et
l'endpoint véhicule `service-flt` donnés "toujours ouverts" sont en
réalité déjà corrigés sur `dev` (PR #117/#118/#115). Le rapport a été
produit sur le commit `727410b` (PR #111), ~12 merges avant ces
correctifs -- pas une erreur de méthode des coéquipiers, juste un
instantané pris trop tôt dans la soirée.

Vérification : `mvn -o test` sur les 5 services touchés par PR #124
(ida 15 tests, exe 12, flt 5, mkt 4, not 3) + service-cap/service-not
pour PR #125 (cap 9 tests, not 3 tests) -- 0 échec partout.

### Autres points hors périmètre — à ne pas croire résolus

- **Multi-pays / conventions bilatérales** (`service-geo`, EF-GEO-05) —
  fonctionnalité absente du domaine, hors périmètre d'un correctif
  ponctuel.
- **Secret webhook par défaut prévisible** (`service-pay`) — déjà piloté
  par `FRETCORRIDOR_PAY_WEBHOOK_SECRET` (fallback dev uniquement dans le
  code) ; l'action réelle relève du déploiement (positionner la variable
  en prod), pas d'un changement de code.

### Vérifié et jugé non concerné — service-geo/mat/opt/trk (Moteur)

En profitant de Java 21 (voir ci-dessous) pour vérifier si les 4
services Moteur avaient le même problème que `service-bur` (#110) :
**non, architecture différente et délibérée**, déjà revue par l'audit
du 19 août lui-même (les `SecurityConfig` de ces 4 services citent
explicitement cet audit dans leur javadoc, contrairement à `service-bur`
dont la docstring était juste trompeuse) :
- `service-geo` : lectures `permitAll` (appels synchrones internes L0
  ~50ms sans JWT + cartes Web en lecture), écritures sensibles
  (`POST`/`PATCH` sur `/api/geo/axes`) restreintes à
  `ROLE_ADMINISTRATION`.
- `service-mat`/`service-opt`/`service-trk` : `permitAll` total —
  endpoints purement internes qui ne transportent jamais de JWT
  (`CoutController` appelé par `ServiceMatClient` sans header
  Authorization), ou endpoints de test manuel (flux nominal réel via
  Kafka), ou (pour `service-trk`) aucun endpoint HTTP exposé du tout.

Ne pas retravailler ces 4 `SecurityConfig` en pensant reproduire le fix
`service-bur` — ce serait casser le budget de latence L0/L1 documenté
sans bénéfice de sécurité réel (l'appelant interne ne transporte de
toute façon aucune identité à vérifier).

### Déjà corrigé par le Moteur, indépendamment de cette passe

- **Fausses alertes `AnomalieDetector`** (`service-trk`, EF-TRK-03) —
  **déjà corrigé** par `stevetelecom` (commit `33818d3`, hors de cette
  session) : fenêtre glissante de 15 min au lieu de comparer à la toute
  première position de l'historique complet. Vérifié en lisant le code
  et son commentaire "BUG CORRIGE (audit du 2026-08-19)" — ne pas
  retravailler ce fichier en pensant le bug encore présent.
- **EF-MAT-10, détour jamais borné** (`service-opt`,
  `SequencementDeclencheur`/`ReplanificationService`/`DetourValidator`)
  — **même commit `33818d3`**, découvert cet après-midi en vérifiant ce
  bloquant précis (pas documenté avant ce jour, alors que
  l'`AnomalieDetector` l'était déjà). `alnsSolver.resoudre(...)` reçoit
  désormais `resoudreParametresAxe(axeId)` (résout `detourMaxDistanceKm`
  réel auprès de `service-geo`) au lieu d'un `Map.of()` codé en dur —
  vérifié réel, pas un stub (`ServiceGeoClient.axeParId`, dégradation
  gracieuse `Map.of()` seulement si axe absent/injoignable). Ne pas
  retravailler ces fichiers en pensant le détour encore illimité.

### Contrainte d'environnement Java 21 — résolue (matin du 20 août)

Ce sandbox n'avait que **Java 17** installé ; `service-geo`/`service-mat`/
`service-opt`/`service-trk` et `common-libs` exigent Java 21. Une
première tentative d'installer Java 21 via SDKMAN avait échoué
(téléchargement corrompu, 136 Mo au lieu des ~190 Mo attendus) —
abandonnée à l'époque plutôt que de s'acharner (**conséquence pratique
documentée alors** : tout changement Moteur, comme `AxeResponse.java`
dans PR #98, ne pouvait être vérifié que par relecture manuelle ligne à
ligne, jamais par compilation réelle).

**Résolu** : `sudo apt-get install -y openjdk-21-jdk` (paquet Ubuntu
standard, `21.0.11+10`) — installé par l'utilisateur lui-même (`sudo`
demande un mot de passe interactif, hors de portée de l'agent). `java`
par défaut bascule automatiquement sur 21 ; Maven nécessite en revanche
`export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` explicite à chaque
commande (l'état du shell ne persiste pas entre deux appels Bash dans
cet environnement — JAVA_HOME doit être réexporté systématiquement pour
tout `mvn` touchant Moteur/`common-libs`).

**Première exécution réelle de `mvn test`** sur les 4 modules Moteur
dans cette session : 2 échecs, tous deux des **tests eux-mêmes**, jamais
revérifiés depuis leur écriture faute de Java 21 — corrigés en PR #112
(`CoutCompositeServiceTest` côté `service-mat` : stub Mockito manquant
sur `saveAll` ; `KuhnMunkresSolverTest.matriceVide` côté `service-opt` :
attendait un comportement contraire à la précondition documentée du
solveur, jamais atteint en pratique). Aucun changement de code de
production. Les 4 modules sont maintenant entièrement verts.

Aucune CI ne couvre ces services Moteur (`backend-web-scope.yml` se
limite à gateway/pay/bur/adm) — toujours vrai, Java 21 disponible en
local ne change rien à ça côté GitHub Actions.

**Tout le reste de l'audit (majeurs/mineurs non cités ci-dessus) n'a pas
été vérifié ni corrigé** dans cette session — ne pas supposer qu'un point
de l'audit est réglé sans le revérifier dans le code, ce document ne
liste que ce qui a été touché explicitement.
