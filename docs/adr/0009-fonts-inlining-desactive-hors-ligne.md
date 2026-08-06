# ADR 0009 — Inlining des polices désactivé pour `ng build` en environnement sans accès Internet

**Statut** : Accepté

## Contexte

Le Sprint 11 (design repris de FretCorridor V3) introduit la police Google
Fonts Montserrat, chargée via `<link>` dans `web/src/index.html`. Par défaut,
`ng build` en configuration `production` tente d'**inliner** les polices
(récupération du CSS `@font-face` sur `fonts.googleapis.com` et embarquement
en base64 dans le bundle, optimisation de first-paint standard d'Angular
CLI). La machine de développement utilisée pour ce dépôt n'a pas d'accès
Internet sortant : `ng build` échouait systématiquement avec `Inlining of
fonts failed`, bloquant toute vérification de build de production.

## Décision

- `web/angular.json`, configuration `production` : `optimization.fonts.inline`
  mis à `false`. Les polices restent chargées via le `<link>` classique dans
  `index.html` (comportement identique à un navigateur sans JavaScript), sans
  optimisation d'inlining base64.
- Budget `anyComponentStyle` relevé de `2kB` à `3kB` (avertissement, pas
  erreur) : le volet vidéo de l'écran de connexion (repris de V3) a un CSS
  légitimement plus riche que les composants précédents.

## Conséquences

- En environnement avec accès Internet (CI, déploiement), le build reste
  valide à l'identique — seule l'optimisation d'inlining est simplement
  absente, ce qui n'affecte pas le fonctionnement de l'application (la
  police se charge normalement au runtime, avec une requête réseau
  supplémentaire au lieu d'un CSS pré-embarqué).
- Si l'environnement de production final dispose d'un accès Internet garanti
  et qu'on souhaite regagner cette optimisation, repasser `fonts.inline` à
  `true` (ou le supprimer, `true` étant la valeur par défaut d'Angular CLI).
