# ADR 0004 — Epinglage de l'image Valhalla par digest, pas par tag semver

## Statut
Accepte

## Contexte
Le CDC v4.0 (S8.11.2, S8.14) cite Valhalla en version 3.7.0 (licence MIT),
et exige par ailleurs (S8.10) que "toute execution [du moteur] soit
reproductible a partir de ses entrees, de sa configuration et de sa graine
aleatoire". L'image Docker officielle `ghcr.io/valhalla/valhalla-scripted`
ne publie cependant aucun tag versionne (verifie le 2026-08-04) : seuls
`latest`, `latest-amd64`, `latest-arm64` et des tags de branche de
developpement existent. Le "3.7.0" du CDC designe la version du logiciel
Valhalla (release Git du 2026-04-28), pas un tag d'image disponible.

## Decision
On epingle l'image par son digest SHA256 capture le 2026-08-04 :
`ghcr.io/valhalla/valhalla-scripted@sha256:e454d110227a83804785ff271628d36548388777939f5e18a887ee1bc3f0ffef`

Ceci fige l'image de facon immuable (contrairement a `:latest`, qui peut
changer de contenu a tout moment sans avertissement), sans pretendre a une
garantie de version logicielle exacte 3.7.0.

## Consequences
- Reproductibilite des builds assuree tant que ce digest reste utilise.
- Necessite une mise a jour manuelle du digest pour toute montee de version
  future (pas de mise a jour automatique via `docker compose pull`).
- Si la version logicielle exacte devient critique (ex. avant mise en
  production), envisager de construire l'image soi-meme depuis le tag Git
  `3.7.0` du depot valhalla/valhalla avec leur `Dockerfile-scripted`.
