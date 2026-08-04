# ADR 0008 — Testcontainers sur cet environnement de développement (Ryuk désactivé, version relevée)

**Statut** : Accepté

## Contexte

Le premier test d'intégration Testcontainers de `service-bur` (Sprint 5, PRD §9) a révélé deux incompatibilités propres à cette machine de développement :

1. **Registre Docker Hub inaccessible** (résolution DNS bloquée pour `registry-1.docker.io`). L'image `testcontainers/ryuk` (nettoyage automatique des conteneurs de test) n'est pas mise en cache localement et ne peut pas être tirée — tout test Testcontainers échoue au démarrage tant que Ryuk est actif.
2. **Négociation de version d'API Docker en échec** avec `testcontainers-bom:1.20.3` : `client version 1.32 is too old. Minimum supported API version is 1.40`. Le démon Docker installé (29.6.1, API 1.55) est trop récent pour la négociation de cette version de `docker-java`.

Un troisième conteneur `panora-staging-postgres` et un ancien build `fretcorridor-*` (sans rapport avec ce dépôt, cf. échange du 2026-08-04) occupaient déjà les ports standards — écartés, sans lien avec ce problème.

## Décision

- `backend/service-bur/pom.xml` utilise `testcontainers-bom:1.21.4` (au lieu de 1.20.3), qui résout la négociation de version d'API Docker.
- Les tests Testcontainers de ce dépôt s'exécutent avec la variable d'environnement `TESTCONTAINERS_RYUK_DISABLED=true` sur cette machine.
- L'image `postgres:16` utilisée par `PostgreSQLContainer<>` est déjà présente dans le cache Docker local (`docker images`) — aucun pull réseau n'est nécessaire, cohérent avec `infra/docker-compose.yml`.

## Conséquences

- **Ryuk désactivé = pas de nettoyage automatique en cas de crash du process de test.** Sur un poste avec accès réseau normal (CI GitHub Actions, autre poste de développeur), retirer cette variable d'environnement — Ryuk fonctionne alors normalement et il ne faut pas la désactiver par défaut.
- Documenté ici plutôt que codé en dur dans un script, pour que chaque porteur touchant à un test Testcontainers sache pourquoi la commande diffère de la documentation officielle.
- Si un nouveau module Spring Boot ajoute des tests Testcontainers, vérifier `docker images` pour l'image cible avant de lancer les tests — un pull réseau échouera silencieusement de la même façon que pour Ryuk.
