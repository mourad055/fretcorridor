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
