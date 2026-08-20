# FretCorridor v4 — Transmission d'état (mise à jour 20 août 2026, après-midi)

> Document de suivi/handoff, versionné dans le dépôt à la racine. Remplace
> la version du 20 août (matin) : **la passe de correction systématique
> sur `AUDIT_CDC_v4_complet_2026-08-19.md` est close pour cette session**
> — sur les 18 bloquants initiaux, **15 sont désormais résolus et
> confirmés dans le code réel** de `dev`. Deux nouveaux fixes cet
> après-midi : consultation de dossier ADM non journalisée (ENF-SEC-02,
> PR #114) et endpoint véhicule public sans filtre tenant (service-flt,
> PR #115). Un troisième bloquant (EF-MAT-10, "détour jamais borné") a
> été trouvé déjà réglé par le Moteur indépendamment de cette passe
> (commit `33818d3`, non documenté avant ce jour). **Restent
> explicitement hors périmètre (2)** : RG-039 (une seule proposition,
> nécessite un vrai algorithme) et RG-070 (preuve de livraison
> photo/tiers, nécessite une infrastructure d'upload — MinIO — absente
> aujourd'hui). Les deux sont documentés dans le code source lui-même
> comme des fonctionnalités distinctes, pas des bugs — ne pas les
> bricoler avec un correctif superficiel si le sujet revient. **22 PR**
> au total mergées depuis le début de cette passe (#91 à #115, sauf
> #111 — voir §6 pour le détail complet).

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

### Explicitement pas traités — à ne pas croire résolus

- **RG-039** (3 propositions au lieu d'une seule, EF-MKT-07) — nécessite
  un vrai algorithme de sélection, mis de côté d'un commun accord dès le
  début de cette passe. Ne pas improviser un correctif superficiel si ce
  point revient.
- **RG-070** (preuve de livraison photo/tiers, EF-EXE-03) — la
  **précédence** des étapes est corrigée (RG-062, PR #96), mais
  `MissionService.verifierPrecedence` documente explicitement que la
  preuve minimale reste hors périmètre : une `LIVRAISON` avec un simple
  `libelle` texte libère toujours le séquestre. Nécessite une
  infrastructure de capture/upload (photo, MinIO déjà dans la stack
  mais pas câblé ici) + UI mobile — une fonctionnalité à part entière,
  pas un correctif de sécurité ponctuel. Ne pas improviser un champ
  "preuve" texte obligatoire en pensant régler le problème : ça ne
  changerait rien à la fraude réelle que RG-070 vise à empêcher.
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
