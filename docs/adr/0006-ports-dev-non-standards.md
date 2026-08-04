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
