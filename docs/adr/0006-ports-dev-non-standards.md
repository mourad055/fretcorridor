# ADR 0006 — Ports de développement non standards (gateway 8082, web 4201 en E2E)

**Statut** : Accepté

## Contexte

La machine utilisée pour ce Sprint 1 est un poste de développement partagé où d'autres services occupent déjà les ports conventionnels : `8080` (un service tiers non lié au projet) et `4200` (idem). `docker compose` (infra/docker-compose.yml) n'est lui-même pas concerné (PostgreSQL/Redis/Kafka/MinIO sur leurs ports standards, déjà distincts).

La première configuration Playwright utilisait `reuseExistingServer: true` avec les ports standards : les tests E2E ont silencieusement piloté un serveur **étranger** déjà présent sur `4200`/`8080` au lieu de l'application FretCorridor, produisant des échecs incompréhensibles (formulaire de connexion d'une autre application).

## Décision

- `backend/gateway` écoute par défaut sur `8082` (`server.port: ${SERVER_PORT:8082}`), surchargeable par variable d'environnement `SERVER_PORT`.
- `web/src/environments/environment.development.ts` pointe vers `http://localhost:8082/api/v1`.
- `web/playwright.config.ts` sert l'application sur `4201` et désactive `reuseExistingServer` pour les deux serveurs (gateway + web), afin de ne jamais réutiliser un processus dont on ne peut pas garantir l'identité.

## Conséquences

- En environnement CI ou sur une machine dédiée sans conflit de port, ces valeurs restent valables (aucune raison de revenir à 8080/4200 spécifiquement).
- Si un déploiement cible exige explicitement le port 8080, le surcharger via `SERVER_PORT=8080`.
- Retenir le principe : ne jamais laisser `reuseExistingServer: true` sur un port qui n'est pas garanti dédié à ce projet.

## Addendum (Sprint 8) — `service-pay` et `infra/docker-compose.yml`

Même principe appliqué à deux nouveaux cas découverts au Sprint 8 :

- `backend/service-pay` écoute par défaut sur `8084` (`server.port: ${SERVER_PORT:8084}`), pour rester distinct de `service-bur` (8083) et du gateway (8082).
- `infra/docker-compose.yml` : le service `postgres` a été renommé `fretcorridor-web-postgres` (au lieu de `fretcorridor-postgres`) et exposé sur le port hôte `5434` (au lieu de `5432`). Cette machine a en effet un autre conteneur nommé `panora-staging-postgres` déjà lié au port 5432 standard, et un conteneur `fretcorridor-postgres` préexistant sans rapport avec ce dépôt (cf. échange du 2026-08-04 sur les conteneurs `fretcorridor-*` antérieurs, stoppés mais non supprimés). Renommer évite tout conflit de nom ou de port sans toucher à des ressources qui ne nous appartiennent pas.

## Addendum (Sprint 13) — un `docker-compose.<service>.yml` par microservice, jamais dans `infra/docker-compose.yml`

Contexte : un collaborateur du périmètre Moteur a signalé que plusieurs
personnes ajoutaient leurs microservices directement dans
`infra/docker-compose.yml` au fil de leurs tests locaux, faisant diverger le
fichier entre branches et créant un risque de conflit (ou pire, d'écrasement
silencieux) à chaque merge vers `dev`. Vérification faite : le Plan
d'Exécution (§2.2) est explicite — `infra/docker-compose.yml` ne contient
que l'infra partagée (PostgreSQL/PostGIS, Redis, Kafka+Zookeeper, MinIO, et
plus tard un sandbox de paiement) ; chaque microservice « est un module
Maven/Gradle indépendant sous `backend/`, avec son propre build **et son
propre déploiement** » (§2.2). Côté Web (ce périmètre), `infra/docker-compose.yml`
n'a jamais contenu `gateway`/`service-pay`/`service-bur`/`service-adm` — seul
le bloc `postgres` partagé y a été modifié (addendum Sprint 8 ci-dessus).

Décision, adoptée pour l'ensemble de l'équipe (en écho à la proposition du
Moteur) : chaque service applicatif reçoit son propre
`backend/<service>/docker-compose.<service>.yml`, jamais fusionné dans le
fichier partagé. On les combine par empilement de `-f`, toujours exécuté
**depuis la racine du repo**, avec `--project-directory .` explicite (sinon
Docker Compose résout les chemins de build relatifs au répertoire du
*premier* `-f`, pas à celui du fichier qui définit le service — piège
constaté en testant cette convention) :

```bash
docker compose --project-directory . \
  -f infra/docker-compose.yml \
  -f backend/gateway/docker-compose.gateway.yml \
  -f backend/service-pay/docker-compose.service-pay.yml \
  -f backend/service-bur/docker-compose.service-bur.yml \
  -f backend/service-adm/docker-compose.service-adm.yml \
  up -d
```

Chacun n'empile que les fichiers des services qu'il veut tester ; personne
ne touche au fichier d'un autre service. `infra/docker-compose.yml` reste
un fichier à modification rare et concertée.

### Conséquences

- Un `Dockerfile` multi-étapes (build Maven puis image JRE) a été ajouté à
  `backend/gateway`, `backend/service-pay`, `backend/service-bur` et
  `backend/service-adm` pour rendre ces fichiers compose réellement
  buildables — ils n'existaient pas avant (le développement local se faisait
  uniquement via `mvn spring-boot:run`, qui reste la méthode par défaut au
  quotidien ; ces images Docker servent surtout à l'intégration multi-service
  et à l'onboarding).
- `docker compose config` (fusion des 5 fichiers) a été vérifié sans erreur.
  La construction de l'image `service-bur` a été vérifiée de bout en bout
  (`docker build` puis `docker compose build`) : le conteneur de build
  BuildKit de cet environnement de développement n'a par défaut pas d'accès
  réseau sortant vers Maven Central (`docker build --network=host` a été
  nécessaire pour la toute première résolution de dépendances) — à
  revérifier sur un poste/CI standard, où ce contournement ne devrait pas
  être nécessaire.
- `docker compose up` a aussi été testé : il détecte correctement un
  conflit si un conteneur `fretcorridor-web-postgres` tourne déjà en dehors
  de ce projet compose (ex. démarré à la main via `docker start`) — comportement
  attendu, pas un bug. Ne jamais lancer `up` sur `infra/docker-compose.yml`
  quand une instance de l'infra partagée tourne déjà par un autre moyen ;
  l'arrêter proprement d'abord (`docker stop`) ou la laisser sous compose.
- Chaque service backend garde son port par défaut documenté dans son propre
  `application.yml` (gateway 8082, service-bur 8083, service-pay 8084,
  service-adm 8085) ; les fichiers compose ne font que les exposer côté hôte
  à l'identique, sans les redéfinir.
