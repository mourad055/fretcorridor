# FretCorridor v4.0

Place de marché numérique du fret et du colis — Cameroun et zone CEMAC (Flysoft Engineering SAS).

Monorepo unique partagé par une équipe de 3 personnes : Mobile, Web, Moteur. Référence d'autorité : `docs/CDC_FretCorridor_v4_FSE2026004_lecture.pdf` (CDC v4.0, FSE-CDC-FRETCORRIDOR-2026-004).

## Qui porte quoi

| Dossier | Porteur | Périmètre |
|---|---|---|
| `mobile/app_chauffeur_transporteur/`, `mobile/app_client/`, `backend/service-ida/cap/mkt/flt/exe/not` | Personne 1 — Mobile | 2 apps Flutter + microservices associés |
| `web/`, `backend/gateway`, `backend/service-pay/bur/adm` | Personne 2 — Web | App Angular unique (3 rôles) + microservices associés |
| `backend/service-mat/opt/trk/geo` | Personne 3 — Moteur | Matching, planification, optimisation, géospatial |

Ce dépôt reflète le développement actif du périmètre **Web** (Personne 2). Les autres dossiers sont scaffoldés en placeholders (README + arborescence), sans logique métier, pour que les coéquipiers puissent y déposer leur travail. Voir `docs/PRD_FretCorridor_Web.md` pour le détail du périmètre Web.

## Documents de référence (`docs/`)

1. `CDC_FretCorridor_v4_FSE2026004_lecture.pdf` — cahier des charges, document d'autorité.
2. `FretCorridor_Plan_Execution_V4_2.docx` — architecture technique, découpage des microservices, plan de sprints.
3. `FretCorridor_Feuille_de_Route_V4_2.docx` — écrans à concevoir par volet et par rôle.
4. `PRD_FretCorridor_Web.md` — périmètre restreint et précisé pour le volet Web.
5. `adr/` — décisions d'architecture, écarts documentés par rapport au CDC/PRD.

## Lancer l'environnement local

```bash
cd infra
docker compose up -d
```

Démarre PostgreSQL 16 + PostGIS, Redis 7, Kafka + Zookeeper, MinIO.

## Contrats d'API

`shared-contracts/openapi/` (REST) et `shared-contracts/asyncapi/` (événements Kafka) sont la référence unique que mobile, web et chaque microservice implémentent. Tout endpoint ou événement doit y être documenté au moment de son implémentation, jamais après.

## Conventions

- Un module = un dossier top-level. Chaque microservice est un module Maven indépendant sous `backend/`.
- Branches : `feature/<module>/<courte-description>`.
- Commits conventionnels avec scope du module : `feat(service-pay): implemente le sequestre logique`.
- CI par path filters (`.github/workflows/`) : une PR touchant uniquement `web/` ne déclenche pas les pipelines `backend/service-mat` etc.
- Invariant financier non négociable : `service-pay` n'écrit que dans un grand livre miroir, jamais de détention de fonds (ENF-FIN-01/02/03).
