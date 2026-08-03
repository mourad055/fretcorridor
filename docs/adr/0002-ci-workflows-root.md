# ADR 0002 — Workflows CI à la racine (`.github/workflows/`) plutôt que dans `infra/`

**Statut** : Accepté

## Contexte

Le PRD §Étape 1 positionne les pipelines CI sous `infra/.github/workflows/`. GitHub Actions n'exécute que les workflows situés sous `.github/workflows/` à la racine du dépôt — un chemin `infra/.github/workflows/` n'est jamais déclenché par la plateforme.

## Décision

Les workflows sont placés à la racine (`.github/workflows/`), avec des path filters par module (`mobile/**`, `web/**`, `backend/<service>/**`) comme demandé, pour respecter l'intention du PRD (CI ciblée par module) sans compromettre son fonctionnement réel.

`infra/` conserve `docker-compose.yml` et toute configuration d'infrastructure locale.

## Conséquences

Aucune, hormis l'emplacement physique du dossier `workflows/`.
