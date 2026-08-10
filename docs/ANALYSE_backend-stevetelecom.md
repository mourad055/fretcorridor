# Analyse — branche `backend-stevetelecom`

**Date** : 2026-08-09
**Auteur** : Mourad (volet Web), avec assistance Claude Code
**Objectif** : évaluer si `backend-stevetelecom` (1 commit d'écart avec `dev`, auteur `stevetelecom`) peut être fusionnée sans risque.

Méthode : lecture complète du diff (55 fichiers, +1423/-144), puis **vérification empirique** — suite de tests du gateway rejouée dans un worktree Git jetable sur cette branche (rien poussé), plutôt que de se fier au message de commit ou au diff seul.

**Statut au 2026-08-09 : intégré dans `dev`, avec 2 correctifs appliqués (§5).** Le commit d'origine n'a pas été fusionné tel quel — il a été cherry-pické puis corrigé sur les 2 points bloquants avant d'être poussé. Le détail ci-dessous reste l'analyse d'origine (utile pour comprendre le *pourquoi* des correctifs) ; §5 documente ce qui a été concrètement fait.

---

## Tableau comparatif — `dev` vs `backend-stevetelecom`

| # | Domaine | `dev` (actuel) | `backend-stevetelecom` | Vérifié | Verdict |
|---|---|---|---|---|---|
| 1 | Modèle `Axe` (gateway) | Enum `AxeEtatActivation` à valeur unique (`VISIBILITE`\|`MATCHING`\|`PAIEMENT`) — un axe ne peut avoir qu'un seul état actif à la fois | 3 booléens indépendants `visibiliteActive`/`matchingActif`/`paiementActif` | Diff relu (domaine, DTO REST, Angular) | ✅ **Bon** — conforme EF-GEO-03, aligné sur le vrai contrat `service-geo` |
| 2 | Adaptateur GEO | `MockGeoAdapter` — 4 axes statiques, filtrés par tenant en mémoire | `RealGeoAdapter` — appelle `service-geo` réel, **stamp le tenantId du JWT sur tous les axes retournés** (pas de filtre serveur) | Code relu + testé | 🔴 **Bloquant** — cf. §2, désaccord de périmètre Phase 1 mono-tenant (Moteur) vs isolation déjà testée (Web) |
| 3 | Isolation multi-tenant (tests) | `AxeControllerIsolationTest` : 3/3 verts | Même suite : **2/3 échouent** (connexion refusée à `service-geo`, ou données non filtrées si le service tourne) | Suite gateway rejouée dans un worktree jetable | 🔴 **Régression mesurée** — 94 tests gateway au total, 2 échecs, tous sur ce point |
| 4 | `infra/docker-compose.yml` | Contient encore `service-ida`/`gateway`/`service-mkt`/`service-flt`/`service-exe`/`service-not` en plus de l'infra partagée | Ces 6 services retirés (-71 lignes) — chacun a son propre `docker-compose.<service>.yml` | Diff relu, aucun doublon/perte confirmés | ✅ **Bon** — termine une migration que j'avais documentée comme incomplète |
| 5 | `service-cap` | `README.md` seul, aucun code | Entité `Capacite` complète, décrément atomique (verrou optimiste), contrôleur REST, migration SQL, événement Kafka | Code relu | 🟡 **Progrès réel, mais aucun test** — notamment sur le verrou optimiste, qui en mériterait un |
| 6 | `service-mkt` | `getPropositions` stub, retourne toujours une liste vide | Consumer Kafka réel (`Proposition`, `DemandePubliee`) branché | Diff relu | ✅ **Bon** — comble un stub explicite |
| 7 | `GatewayApplication` dupliquée | Classe fantôme `com.flysoft.fretcorridor.gateway.GatewayApplication` (résidu de ma fusion), inutilisée mais présente | Supprimée | Diff relu | ✅ **Bon** — corrige un oubli de ma part |
| 8 | `docker-compose.*.yml` — `context:` | `context: backend/<service>` (relatif à la racine du repo, testé avec `--project-directory .`, cf. ADR 0006/commit `5dfe8f5`) | `context: ../backend/<service>` sur gateway/pay/adm/bur (aligné sur ce que Moteur utilisait déjà pour geo/mat/opt/trk) | **Non vérifié empiriquement** (pas de Docker lancé) | ⚠️ **À tester** — les deux conventions ne peuvent pas être justes en même temps avec la même invocation |
| 9 | Port hôte `service-pay` | `8084:8084` | `8088:8084` (port conteneur inchangé) | Diff relu, cohérent avec l'arrivée de `service-geo` sur 8084 | 🟡 **Probablement volontaire** — à confirmer avec l'équipe |
| 10 | Message de commit | — | Titre : *"rayon matching EF-MAT-01, timeout service-geo, contrats opt/trk"* — aucun fichier `service-mat`/`service-opt`/`service-trk`/`shared-contracts/` dans le diff | `git show --stat` | ⚠️ **Trompeur** — ce travail existe (`17e0caa`) mais était déjà fusionné dans `dev` avant ce commit |
| 11 | Périmètre/ownership | Gateway = Web, `service-cap`/`service-mkt` = Mobile, `service-geo` = Moteur (Plan d'Exécution §4.1) | Un seul commit, a priori Moteur, touche gateway + construit `service-cap` (Mobile) + modifie 2 fichiers Web | Diff relu | 🟡 **Hors périmètre habituel** — fonctionnellement inoffensif ici, mais à signaler |

---

## 1. Ce qui est solide

- **Modèle `Axe` corrigé** : remplace un enum à valeur unique (`VISIBILITE`/`MATCHING`/`PAIEMENT`, mutuellement exclusif) par 3 booléens indépendants (`visibiliteActive`, `matchingActif`, `paiementActif`). C'est la bonne correction — conforme à EF-GEO-03 du CDC ("les 3 états sont indépendants") et au vrai contrat de `service-geo` (`AxeResponse` expose déjà ces 3 champs). Domaine gateway, DTO REST et composants Angular (`axe.models.ts`, `axes-map.component.ts`, `status-badge.component.ts`) mis à jour ensemble, sans toucher aux fonctions `status-badge` que j'utilise pour missions/dossiers/KYC — changement bien circonscrit.
- **Nettoie un bug que j'avais laissé dans ma fusion** : une classe `GatewayApplication` fantôme (`com.flysoft.fretcorridor.gateway`, l'ancien squelette de `dev` jamais supprimé lors de mon merge) — code mort inoffensif mais bon rattrapage.
- **Termine la migration docker-compose par service** que j'avais commencée et documentée comme "en cours" dans `docs/ANALYSE_FUSION_dev_web-socle.md` : `infra/docker-compose.yml` perd enfin `service-ida`/`gateway`/`service-mkt`/`service-flt`/`service-exe`/`service-not` (-71 lignes), chacun récupère son propre `docker-compose.<service>.yml`. Vérifié : aucun doublon, aucune perte de service dans le résultat.
- **`service-cap` prend forme** (jusque-là un `README.md` vide) : entité `Capacite` complète et cohérente avec EF-CAP-01/02/07 (poids taxable figé à la déclaration, décrément atomique par verrou optimiste, limitations documentées en commentaire plutôt que cachées — ex. "outbox transactionnel à envisager en Phase 2").
- **`service-mkt`** reçoit du vrai câblage Kafka (consumer, `Proposition`/`DemandePubliee` events) — comblait un stub explicite ("S5 — stub en attendant service-mat/service-opt").

## 2. Le point bloquant : `RealGeoAdapter`, et pourquoi ce n'est pas juste "du code buggé"

`MockGeoAdapter` est remplacé par un vrai appel HTTP à `service-geo`. Le code :

```java
.map(dto -> new Axe(
        dto.id(),
        tenantId, // tenant impose par le JWT, pas encore porte par service-geo (Phase 1)
        ...
```

`GET /api/geo/axes` renvoie *tous* les axes (pas de filtre serveur), et l'adaptateur **colle le tenantId du JWT appelant sur chaque axe retourné**, qu'il lui appartienne réellement ou non.

**Vérification empirique** (worktree jetable, suite gateway complète) :
```
Tests run: 94, Failures: 2, Errors: 0
→ AxeControllerIsolationTest.bureau_douala_sees_only_its_own_tenant_axes
→ AxeControllerIsolationTest.bureau_tchad_sees_only_its_own_tenant_axes_and_none_of_douala
```
(Sans `service-geo` lancé localement, l'appel échoue en connexion refusée — les 2 tests échouent nécessairement dans n'importe quel environnement CI/dev standard.)

**Mais en creusant la migration jointe** (`backend/service-geo/.../V4__add_tenant_id.sql`), le commentaire de `stevetelecom` est explicite :
> *"la logique d'isolation stricte (ENF-MUL-01/03, filtrage actif, tests d'étanchéité automatisés) est explicitement Phase 3 (Plan d'exécution S18, "second tenant institutionnel"). Un seul tenant existe en Phase 1 (BGFT, client-ancre) : rien à isoler de lui-même."*

**C'est donc un vrai désaccord de périmètre entre nos deux volets, pas juste un bug isolé** : côté Moteur, GEO est délibérément mono-tenant jusqu'en Phase 3 (décision documentée). Côté Web/gateway, mes tests d'isolation (Sprint 3) modélisent déjà 2 tenants de démonstration (Douala + Tchad) pour prouver ENF-MUL-01 — une exigence Must-have du CDC, mais que j'ai anticipée avant que GEO ne soit prêt à la supporter réellement.

Deux angles, à trancher en équipe plutôt qu'à fusionner en silence :
- Si on accepte que les axes restent mono-tenant en Phase 1 (aligné sur Moteur) : mon test Tchad doit être ajusté/marqué en attente de Phase 3, pas laissé rouge.
- Si on veut prouver ENF-MUL-01 sur les axes dès maintenant (aligné sur mon architecture actuelle) : `RealGeoAdapter` ne doit pas *fabriquer* un tenant sur des données non filtrées — il vaudrait mieux exposer clairement "GEO ne fournit qu'un seul tenant pour l'instant" plutôt que relabelliser silencieusement, ce qui deviendra une vraie fuite le jour où un deuxième tenant existera réellement dans `service-geo` sans que le filtrage serveur ait suivi.

Dans les deux cas, l'un des deux camps doit bouger — ni l'un ni l'autre en a le droit unilatéralement puisque ça touche un test et une garantie (ENF-MUL-01) qui ne sont pas la propriété exclusive d'une seule personne.

## 3. Points secondaires à lever avant fusion

- **`service-cap` : aucun test.** Ni sur `CapaciteService.decrementer` (verrou optimiste — précisément le genre de logique qui mérite un test de concurrence), ni sur le contrôleur. Détonne avec la rigueur du reste du monorepo (ArchUnit pour ENF-FIN, isolation systématique côté gateway).
- **`service-cap` sans sécurité** (pas de `spring-boot-starter-security`, aucun `@RequestHeader` d'auth) — cohérent avec `service-geo` qui a le même choix, mais à confirmer que c'est un choix Phase 1 assumé et pas un oubli, avant que le gateway ne s'y connecte un jour.
- **Message de commit trompeur** : le titre annonce *"rayon matching EF-MAT-01, timeout service-geo, contrats opt/trk"` — aucun fichier `service-mat`, `service-opt`, `service-trk` ou `shared-contracts/` n'apparaît dans le diff. Ce travail existe bien (`17e0caa`, "filtre L0 par rayon d'appariement"), mais il était déjà fusionné dans `dev` **avant** ce commit — le titre semble recyclé d'un commit précédent plutôt que décrire ce qui est réellement dedans. Gênant pour la revue, pas un bug en soi.
- **`context: ../backend/<service>`** dans `docker-compose.gateway.yml`/`service-pay`/`service-adm`/`service-bur` (et les nouveaux fichiers) — aligne sur ce que Moteur utilisait déjà pour GEO/MAT/OPT/TRK, mais contredit ma propre convention testée et documentée (ADR 0006 addendum, commit `5dfe8f5` : *"vérifié... fusion des 5 fichiers sans erreur"*). Non vérifié empiriquement ici (pas de Docker lancé) — à tester avant de trancher lequel des deux fonctionne réellement avec `--project-directory .`.
- **Port `service-pay` 8084→8088** (host uniquement, le port interne au conteneur ne change pas) — cohérent avec l'arrivée de `service-geo` sur 8084, a priori volontaire, à confirmer avec l'équipe plutôt qu'à supposer.
- **Périmètre** : ce commit, a priori du Moteur, construit aussi `service-cap` en entier (Mobile) et touche des fichiers Web (`axes-map.component.*`, `status-badge.component.ts`). Fonctionnellement ça ne casse rien côté Web (vérifié ligne à ligne), mais ça sort du principe "chaque personne développe les services dont son propre client a besoin" (Plan d'Exécution §4.1) — à mentionner à l'équipe, pas nécessairement à bloquer pour ça seul.

## 4. Recommandation

Ne pas fusionner tel quel. Avant de le faire :
1. Trancher en équipe le point §2 (axes mono-tenant assumé en Phase 1, ou filtrage réel exigé maintenant) — c'est le seul point qui casse des tests existants.
2. Ajouter au moins un test sur `CapaciteService.decrementer` (concurrence/idempotence) avant de considérer `service-cap` prêt à consommer.
3. Vérifier empiriquement (`docker compose config` ou `build` depuis la racine) lequel des deux `context:` fonctionne réellement, avant de généraliser l'un ou l'autre.

Le reste (modèle Axe, nettoyage, migration docker-compose, avancée MKT) est du bon travail à garder tel quel une fois le point §2 réglé.

## 5. Statut final — ce qui a été fait

Le commit `a671325` a été cherry-pické sur `feature/web-socle` puis `dev` (fast-forward), avec deux correctifs appliqués par-dessus avant de pousser :

1. **`RealGeoAdapter` neutralisé, puis activé le 2026-08-10.** Le point §2 a depuis été tranché en équipe : Phase 1 mono-tenant assumé (Feuille de route §1.1, un seul axe/tenant réel — cf. [ADR 0011](adr/0011-geo-mono-tenant-phase-1.md)). `RealGeoAdapter` est désormais l'implémentation active par défaut (plus de `@Profile`) ; `MockGeoAdapter` est devenu une fixture de test (`src/test/java`, `@Primary`, même mécanisme que l'auth). La limite (`RealGeoAdapter` ne filtre pas réellement par tenant) est caractérisée par un test dédié (`RealGeoAdapterTest`), pas seulement documentée — il est conçu pour casser dès que `service-geo` filtrera réellement, signal explicite pour la suite. **Suite de tests gateway : 95/95 verts.**
2. **Convention docker-compose unifiée.** `context: ../backend/<service>` (le changement de `stevetelecom`) est correct **à condition de ne jamais passer `--project-directory`** — vérifié empiriquement (`docker compose config`, les 15 services du monorepo, contextes tous résolus vers le bon chemin absolu). Les en-têtes de `backend/gateway`, `service-pay`, `service-adm`, `service-bur` — qui prescrivaient encore `--project-directory .` (l'ancienne convention Web, désormais abandonnée) — ont été corrigés pour refléter la commande réellement valide. Sans ce correctif, les 4 fichiers étaient internement contradictoires (commentaire et code se contredisant).

**Non corrigé, laissé en l'état (suivi séparé, pas bloquant pour la fusion)** :
- `service-cap` reste sans test (§3) — à traiter quand le gateway commencera à le consommer (cf. `docs/ROADMAP_INTEGRATION_gateway.md`, item #8).
- Le port `service-pay` 8084→8088 et le titre de commit trompeur sont restés tels quels — cosmétique/mineur, ne justifie pas de retoucher le commit de `stevetelecom`.
- Le désaccord de fond du §2 (axes mono-tenant Phase 1 vs isolation immédiate) reste à trancher en équipe — la neutralisation du profil ne fait que retarder la décision proprement, elle ne la remplace pas.

Vérification finale avant push : `mvn test` sur `backend/gateway` (94/94) et `backend/service-pay` (31/31) verts ; `mvn compile` sur `backend/service-cap` et `backend/service-mkt` verts.
