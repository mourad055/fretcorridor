# ADR 0007 — Carte des axes schématique plutôt que géospatiale réelle (Sprint 3)

**Statut** : Accepté

## Contexte

Le PRD §9 Sprint S3 demande « une carte des axes de son tenant » pour le rôle Bureau. Une carte géospatiale réelle (tuiles OSM, Leaflet/MapLibre, coordonnées GPS) suppose des coordonnées de hubs que seul `service-geo` (Moteur, PostGIS) fournira, et un accès réseau à un serveur de tuiles — non garanti sur tous les postes de développement/CI.

## Décision

Le Sprint 3 livre une représentation schématique en SVG : les hubs uniques (origines/destinations) sont positionnés par un calcul déterministe (répartition sur une ligne), reliés par des segments représentant les axes, colorés selon l'état d'activation (EF-GEO-03). Un tableau détaillé accompagne la carte. Aucune coordonnée géographique n'est utilisée ni affirmée comme réelle.

## Conséquences

- Cette représentation satisfait le critère de sortie du Sprint 3 (« un Bureau voit une carte des axes de son tenant ») sans dépendance à un service de tuiles externe.
- Elle sera remplacée par une carte géospatiale réelle une fois `service-geo` livré des coordonnées de hubs (Moteur, `@stevetelecom`), au plus tard lors d'un sprint consommant `service-trk` pour le suivi temps réel (Sprint 6, PRD §9).
- Ne pas confondre cette représentation avec une carte géographique dans la documentation utilisateur.

## Addendum (Sprint 12) — carte Leaflet réelle, en attendant service-geo

À la demande explicite du produit, la représentation schématique SVG est
remplacée par une vraie carte géospatiale (`CorridorMapComponent`, Leaflet +
tuiles OpenStreetMap), centrée sur le corridor CEMAC Cameroun–Tchad (centre
`[6.5, 12.5]`, zoom 5). `service-geo` n'étant toujours pas livré, les
coordonnées des hubs proviennent d'un référentiel statique côté web
(`web/src/app/features/bureau/axes/villes-cemac.ts`), documenté comme un
palliatif temporaire — pas un remplacement définitif de `service-geo`.

- Le référentiel ne couvre que les villes déjà utilisées par les données
  mockées de `service-geo`/`service-trk` (Douala, Yaoundé, Bafoussam,
  N'Djamena, Moundou) plus quelques villes du corridor pour anticiper de
  nouveaux axes (Garoua, Maroua, Sarh). Un axe référençant une ville absente
  du référentiel est silencieusement omis de la carte (mais reste visible
  dans le tableau détaillé, qui n'a pas besoin de coordonnées).
- Dès que `service-geo` expose de vraies coordonnées de hub, ce référentiel
  statique doit être supprimé au profit des coordonnées reçues par API —
  ne pas le laisser vivre en parallèle d'une source de vérité serveur.
- Le tableau « Détail des axes » est conservé sous la carte : il reste la
  source exhaustive (aucune perte de données même pour une ville non
  cartographiée).
- Tests : Leaflet est doublé en Jest (`src/testing/leaflet.mock.ts`, mappé
  via `moduleNameMapper`) — jsdom ne supporte pas assez le rendu SVG/Canvas
  pour la vraie bibliothèque. Le rendu réel est vérifié manuellement et en
  E2E (navigateur réel via Playwright).
