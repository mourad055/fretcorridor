# ADR 0005 — Service worker PWA différé après le Sprint 1

**Statut** : Accepté

## Contexte

Le CDC (§4.2, stack) prévoit une PWA avec service worker pour `web/`. La Definition of Done du Sprint 1 (PRD §9) ne porte que sur l'authentification et la garde de routes RBAC.

## Décision

Le Sprint 1 livre un `manifest.webmanifest` minimal (installabilité) mais n'active pas encore `@angular/service-worker` (mise en cache offline, `ngsw-config.json`). Cette activation est reportée à un sprint où une page a effectivement besoin d'un comportement hors-ligne vérifiable (aucune actuellement).

## Conséquences

- Ne pas cocher ENF-OFF-01/02 comme couverts côté web tant que le service worker n'est pas activé et testé.
- Activer `@angular/service-worker` avant la fin de la Phase 1, au plus tard lors d'un sprint livrant une page dont l'usage hors-ligne est requis.
