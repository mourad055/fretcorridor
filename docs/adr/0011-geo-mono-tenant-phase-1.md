# ADR 0011 — GEO mono-tenant assumé en Phase 1 (RealGeoAdapter actif sans filtrage serveur)

**Statut** : Accepté (décision d'équipe, 2026-08-10)

## Contexte

`service-geo` (Moteur) expose `GET /api/geo/axes`, qui renvoie l'intégralité des axes qu'il porte, sans paramètre de filtre ni notion de tenant côté API — bien qu'une colonne `tenant_id` ait été ajoutée en base (migration `V4__add_tenant_id.sql`), elle n'est pas encore exposée par le contrôleur. Le commentaire de cette migration est explicite : *« la logique d'isolation stricte (ENF-MUL-01/03, filtrage actif, tests d'étanchéité automatisés) est explicitement Phase 3 (Plan d'Exécution S18, "second tenant institutionnel"). Un seul tenant existe en Phase 1 (BGFT, client-ancre) »*.

Côté gateway, `RealGeoAdapter` (qui remplace `MockGeoAdapter` pour consommer le service réel — cf. `docs/ANALYSE_backend-stevetelecom.md`) ne peut donc pas filtrer par tenant : il colle le `tenantId` du JWT de l'appelant sur chaque axe retourné par `service-geo`, sans le vérifier contre une donnée réelle. Ce n'est pas une garantie d'isolation ENF-MUL-01 — c'est une absence de risque tant qu'un seul tenant réel existe dans `service-geo`.

Le gateway modélise pourtant déjà 2 tenants de démonstration (`tenant-bgft-douala`, `tenant-bnft-ndjamena` — cf. ADR 0010) pour les tests d'isolation (`AxeControllerIsolationTest`), construits en Sprint 3 par anticipation, avant que le périmètre réel de `service-geo` ne soit connu.

## Décision

- **Le périmètre réel de la Phase 1 est mono-tenant.** La Feuille de route V4 §1.1 est explicite : *« Concentration géographique sur le Cameroun (un seul axe en Phase 1), le réseau CEMAC est différé, pas abandonné »*. Il n'existe donc, en production Phase 1, qu'un seul tenant institutionnel réel (BGFT) et un seul axe. L'absence de filtrage serveur côté `service-geo` est sans conséquence dans ce périmètre.
- `RealGeoAdapter` devient l'implémentation active par défaut du gateway (plus de profil Spring conditionnel) — `MockGeoAdapter` est déplacé en fixture de test (`src/test/java`, `@Primary`), même mécanisme que `ServiceIdaAuthenticationAdapter`/`MockIdaAuthenticationAdapter`.
- La limite est rendue **explicite et testée**, pas seulement documentée en commentaire : `RealGeoAdapterTest` caractérise le comportement réel (colle le tenant demandé, ne filtre rien) contre un `service-geo` factice ; `AxeControllerIsolationTest` continue de vérifier le contrat du port `GeoPort` via la fixture `MockGeoAdapter`, avec un Javadoc précisant qu'il ne teste pas `RealGeoAdapter` lui-même.

## Conséquences

- **Dette explicitement tracée, pas cachée.** Dès qu'un deuxième tenant institutionnel rejoint réellement `service-geo` (Phase 3, Plan d'Exécution S18), ce comportement redevient dangereux : il faudra que `service-geo` expose un vrai filtre serveur (ex. `GET /api/geo/axes?tenantId=`, ou tout mécanisme équivalent côté Moteur) avant d'accueillir ce second tenant — pas après.
- `RealGeoAdapterTest` est conçu pour casser le jour où `service-geo` commence à filtrer réellement : c'est le signal explicite qu'il faut migrer `AxeControllerIsolationTest` pour vérifier `RealGeoAdapter` directement plutôt que la fixture.
- Aucune action requise côté Mobile/Web : ce choix ne change ni le contrat `GeoPort` ni les DTO REST exposés par le gateway au Bureau.
