# Analyse de fusion — `origin/dev` × `feature/web-socle`

**Date** : 2026-08-07 (mise à jour le même jour après 47 nouveaux commits sur `dev`)
**Auteur** : Mourad (volet Web), avec assistance Claude Code
**Objectif** : évaluer le risque de conflit et la conformité au CDC v4 / Feuille de route V4.2 / Plan d'Exécution V4.2 avant de fusionner `feature/web-socle` dans `dev`.

Méthode : comparaison des deux branches depuis leur ancêtre commun (`b58bda0`, bootstrap initial du monorepo), complétée par un essai de fusion réel dans un worktree Git jetable (annulé ensuite, rien n'a été poussé) pour observer les conflits effectifs plutôt que de les déduire.

---

## 0. Mise à jour — qu'est-ce qui a changé depuis la première analyse ?

`dev` a reçu 47 commits supplémentaires (Mobile : app_client Phase 1 complète ; Moteur : validation Phase 1 GEO/MAT/OPT/TRK, alignement des contrats sur le code réel). Nouvel essai de fusion à l'identique : **le nombre de conflits n'a pas changé.**

| | Avant | Après |
|---|---|---|
| Fichiers en conflit textuel | 4 (`Dockerfile`, `pom.xml`, `application.yml` du gateway ; `docker-compose.yml` fusionne seul) | Identique |
| Conflits réels à résoudre à la main | 3 (le gateway, toujours en double implémentation) | Identique |
| `shared-contracts/` | `geo-api.yaml` seul côté Moteur, non recroisé | `mat-api.yaml` et `opt-api.yaml` ajoutés, `geo-api.yaml` et les README réalignés sur le code réellement testé (**réduit le risque d'intégration**, pas encore exploité côté gateway) |
| Convention docker-compose par service | Ignorée côté `dev` (tout dans `infra/docker-compose.yml`) | **Adoptée par le Moteur** (service-geo/mat/opt/trk ont désormais leur propre `docker-compose.<service>.yml`) ; toujours pas adoptée côté Mobile (gateway, service-ida, service-mkt, service-flt, service-exe, service-not restent dans le fichier partagé) |
| Fragilité observée | — | Deux commits `dev` ("restaure gateway/service-ida supprimes par erreur") montrent que la migration vers les compose par service a fait disparaître par accident les entrées gateway/service-ida avant d'être restaurée — la coordination sur ce chantier est encore en cours côté équipe |

**Conclusion de la mise à jour : le risque de conflit Git n'a pas diminué (toujours 3 conflits, tous sur le gateway), mais le risque d'intégration derrière ces conflits a légèrement diminué** grâce à des contrats `shared-contracts/` plus complets et plus fiables, et à un début de convergence sur la convention docker-compose. Les sections ci-dessous sont mises à jour en conséquence.

## 1. Ampleur de la divergence

| | Fichiers modifiés | Insertions | Contenu |
|---|---|---|---|
| `feature/web-socle` (Web) | 440 | +36 170 | gateway complet, service-adm, service-pay, service-bur, app Angular 3 rôles |
| `origin/dev` (Mobile + Moteur) | 370 | +18 386 | app_client Flutter, service-ida, service-mkt, service-cap/flt/exe/not, service-geo/mat/opt/trk |

Les deux branches sont parties du même point il y a longtemps et n'ont jamais été resynchronisées : fusion de type "big bang", le scénario que le point hebdomadaire de synchronisation des contrats d'API (Feuille de route §5.1) est censé éviter.

## 2. Conflits Git textuels

Un seul recoupement de fichiers entre les deux branches :

- `backend/gateway/Dockerfile`
- `backend/gateway/pom.xml`
- `backend/gateway/src/main/resources/application.yml`
- `infra/docker-compose.yml` (fusionne automatiquement, sans conflit — additions des deux côtés)

**Le problème n'est pas textuel, il est architectural.** `backend/gateway` a été créé indépendamment des deux côtés :

- **Côté `dev`** : le squelette Sprint 1 d'origine (`b58bda0`), jamais retouché depuis. Spring Cloud Gateway classique, routes proxy HTTP vers `service-ida`/`service-mkt`, `groupId com.flysoft.fretcorridor`, port 8080.
- **Côté `web-socle`** : reconstruit en hexagonal complet — JWT/RBAC réels, contrôleurs REST propres pour KYC/CAP/GEO/OPT/TRK/EXE/NOT/ADM/PAY — avec des adaptateurs `Mock*` (`MockGeoAdapter`, `MockOptAdapter`, `MockTrkAdapter`, `MockCapAdapter`, etc.) en attendant l'intégration réelle. `groupId com.fretcorridor`, port 8082 (ADR 0006).

Résoudre les 4 fichiers en conflit prendra quelques minutes. **Le vrai travail est de brancher les adaptateurs mock du gateway sur les services réels livrés sur `dev`** (service-ida, service-mkt, service-geo en priorité) — potentiellement plusieurs jours, pas un merge Git.

## 3. Conformité aux 3 documents de référence

### Respecté

- Répartition des porteurs (Feuille de route §1, Plan d'Exécution §4.1) : gateway/PAY/BUR/ADM côté Web, IDA/CAP/MKT/FLT/EXE/NOT côté Mobile, MAT/OPT/TRK/GEO côté Moteur — aucune violation de périmètre constatée.
- Architecture hexagonale (§3.2) respectée côté gateway local (ports/adapters propres).
- Démarrage anticipé sans confirmation formelle des verrous V1/V2 : documenté et assumé (ADR 0003), cohérent des deux côtés puisque `dev` a aussi commencé à construire la plateforme.
- Aucun conflit sur `web/` ni sur `.github/workflows/` — le périmètre Web reste isolé du reste.

### Points de non-conformité ou de dérive à traiter

1. **`shared-contracts/` toujours pas exploités côté gateway, mais désormais fiables.**
   Côté `web-socle` : 13 contrats OpenAPI publiés (`gateway-*`, `service-adm`, `service-bur`, `service-pay`), toujours inchangés.
   Côté `dev` : `geo-api.yaml` (réduit et réaligné sur le code réellement testé, 421 → 336 lignes), `mat-api.yaml` et `opt-api.yaml` désormais publiés, + 7 événements AsyncAPI (`position-brute`, `position-eta`, `proposition-emise`, `affectation-confirmee`, `alerte-ecart`, `capacite-declaree`, `demande-publiee`), README `shared-contracts/` et `asyncapi/` mis à jour.
   Aucun chevauchement de nom, donc toujours pas de conflit Git — mais les adaptateurs `Mock*` du gateway local ont été écrits **avant** cet alignement et n'ont toujours pas été confrontés à ces contrats. Le point positif : contrairement à la première analyse, ces contrats reflètent maintenant du code réellement testé côté Moteur, donc le travail de branchement post-merge (§5.4) repose sur une base plus solide qu'il y a quelques heures.

2. **Convention docker-compose par service : adoption partielle, en cours.**
   Le commit `5dfe8f5` (branche `web-socle`) affirme une convention "adoptée pour toute l'équipe suite au signalement d'un collaborateur (périmètre Moteur)" : un `docker-compose.<service>.yml` par service, `infra/docker-compose.yml` réservé à l'infra partagée.
   Depuis, le Moteur a effectivement migré `service-geo`, `service-mat`, `service-opt` et `service-trk` vers cette convention (commits `7b9fc4a`/`664b3ab` "sort geo/mat/opt/trk (+valhalla) vers compose par service"). Le Mobile n'a pas encore migré : `gateway`, `service-ida`, `service-mkt`, `service-flt`, `service-exe`, `service-not` restent définis dans `infra/docker-compose.yml`.
   Signe de fragilité : deux commits (`01bb4bf`, `6f8ca1a`) montrent que la migration du Moteur a fait disparaître par erreur les entrées `gateway`/`service-ida` du fichier partagé, avant d'être restaurées manuellement — la convention est en cours d'adoption mais pas encore stabilisée en équipe. À vérifier lors du point de synchro avant fusion, pour éviter que la fusion ne réintroduise une régression similaire côté Web.

3. **Double implémentation de l'identité/authentification en germe.**
   Le gateway local contient `MockIdaAuthenticationAdapter` et `MockIdaKycAdapter` (JWT généré localement dans le gateway). `service-ida` (porteur Mobile, commit `ba64533`) implémente déjà une vraie authentification JWT. Le Plan d'Exécution §4.1 fait de `service-ida` le porteur unique de l'identité — après fusion, il faudra explicitement retirer/rebrancher les mocks d'auth du gateway vers `service-ida`, sinon il existe deux sources de vérité JWT.

4. **Incohérence de `groupId` Maven pré-existante.**
   `com.fretcorridor` (Web, Moteur) vs `com.flysoft.fretcorridor` (Mobile, stub gateway) — mineure, mais à trancher en équipe plutôt qu'au hasard du merge.

## 4. Ce qui n'a pas été audité

La conformité CDC du code Mobile/Moteur au niveau détail (ex. anti-patron glouton dans OPT, absence de valeurs codées en dur dans MAT) n'a pas été vérifiée — cela demanderait une lecture approfondie de ce code, hors du périmètre de cette comparaison de branches.

## 5. Recommandation pour la fusion vers `dev`

Ne pas fusionner tel quel. Ordre suggéré :

1. Committer tout travail en cours des deux côtés avant de commencer.
2. Point de synchro avec Mobile/Moteur sur `shared-contracts/` (surtout `geo-api.yaml` et les événements Kafka) et sur la convention docker-compose, **avant** la fusion.
3. Fusionner, résoudre les 4 conflits gateway en conservant l'implémentation `web-socle` (plus avancée) mais en réintégrant les routes IDA/MKT du stub `dev` comme point de départ.
4. Remplacer ensuite, service par service, les adaptateurs Mock par de vrais appels vers `service-ida`, `service-geo`, etc. — c'est le chantier principal post-merge, pas la fusion elle-même.
