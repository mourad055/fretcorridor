# FretCorridor v4 — Transmission d'état (mise à jour 17 août 2026)

> Document de suivi/handoff, versionné dans le dépôt à la racine. Remplace
> la version du 7 août 2026, largement obsolète : Phase 2 entière a été
> codée, mergée, et une partie déjà rebranchée sur le vrai backend depuis.

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
  8092) — voir commit `9c52f02`. ⚠️ `lib/providers/dio_provider.dart`
  actuel sur `dev` a un seul `dioProvider` (baseUrl 8088, faux pour la
  plupart des écrans) — le refactor en 5 clients nommés par service
  existe dans l'historique (`9c52f02`) mais **n'est pas fusionné dans
  `dev`**, à vérifier/réappliquer si des écrans Client ont des soucis
  réseau.
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
| S11 — Consolidation LTL | ✅ mocké (tournée multi-étapes) — PR #51 | ✅ mocké (indicateur consolidation) — PR #52 |
| S12 — Retour à vide | ✅ mocké (notification retour) — PR #53 | — (rien pour Client) |
| S13 | — (backend/Web uniquement) | — |
| S14 — Paiement Mobile Money | ✅ mocké (affichage règlement) — PR #54 | ✅ mocké (choix moyen paiement) — PR #55 |
| S15 — Second axe | ✅ **branché sur le vrai backend** (PR #56 puis re-câblage réel PR #60) | ✅ **branché sur le vrai backend** (PR #57 puis re-câblage réel PR #61) |

**S15 est le seul sprint Phase 2 sorti du mode mock à ce jour** (17 août) :
les deux apps appellent réellement `service-geo`
(`GET /api/geo/axes?tenantId=...`, direct côté Client — pas de route
gateway pour le rôle Chargeur ; via `GET /axes` gateway côté
Chauffeur/Transporteur) et affichent les 2 axes réels en base.

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
  marqué version 1.0.0 (pas brouillon), mais **incohérent avec ce que le
  Moteur a annoncé oralement** : il avait dit que l'événement porterait
  soit `tourneeId` soit `affectationId` (jamais les deux) pour couvrir le
  cas FTL simple (majoritaire en Phase 1, pas seulement les tournées LTL
  consolidées). Le fichier réellement mergé n'a **que `tourneeId`
  (requis)**, aucun champ `affectationId`. À clarifier avec le Moteur
  avant d'implémenter quoi que ce soit côté S12 réel — son fix FTL n'est
  peut-être pas encore dans ce fichier.

---

## 5. Ce qui reste ouvert

1. **Test réel dans l'app Chauffeur/Transporteur** — lancer l'app pour de
   vrai (pas juste vérifier que le code compile) et confirmer que "Mes
   missions" affiche une mission pour un compte de test, preuve que la
   chaîne complète (service-cap → service-opt → service-exe,
   `transporteurId` rempli) fonctionne jusqu'à l'UI mobile. Le backend est
   vérifié (PR #58, tests Kafka confirmés par le Moteur) mais aucune trace
   dans le dépôt d'un lancement réel de l'app elle-même. **C'est le seul
   point du dernier récap resté en suspens.**
2. **S11/S12 réels** — attendre confirmation explicite du Moteur (pas
   juste "c'est mergé dans dev") avant de sortir ces sprints du mode mock
   côté mobile — voir §4.
3. **`dio_provider.dart` côté Client** — vérifier si le refactor multi-
   clients (`9c52f02`, jamais mergé dans `dev`) doit être réappliqué ; le
   `dioProvider` unique actuel pointe sur un mauvais port par défaut pour
   plusieurs écrans (pas un bug introduit récemment, présent depuis
   longtemps sur `dev` — juste jamais corrigé sur cette branche).
