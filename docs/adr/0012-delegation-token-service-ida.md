# ADR 0012 — Retransmission du token service-ida (double autorité JWT)

**Statut** : Accepté (décision d'équipe, 2026-08-10)

## Contexte

Le gateway (Web) émet son propre JWT (secret propre, claims `role`/`tenantId`) pour protéger ses routes RBAC. `service-exe`, `service-not` et `service-mkt` (Mobile) valident, eux, les JWT signés par `service-ida` (secret propre, distinct de celui du gateway). Un JWT du gateway ne valide donc jamais sur ces services — aucun appel du gateway vers l'un d'eux, pour le compte d'un acteur connecté, n'est possible tel quel. C'était le point n°6 (transverse) de `docs/ROADMAP_INTEGRATION_gateway.md`, bloquant explicitement les items n°3 (`service-exe`) et n°5 (`service-not`).

Deux options envisagées :
1. Aligner secret et forme des claims entre le gateway et service-ida, pour que le JWT du gateway soit accepté directement par service-exe/service-not/service-mkt.
2. Faire retransmettre par le gateway le JWT brut émis par service-ida lors de la connexion, en plus du sien.

L'option 1 exige que Mobile et Web synchronisent un secret partagé et la forme exacte des claims (`roles` en tableau côté service-ida/Mobile, `role` au singulier côté gateway) — coordination cross-équipe, hors de portée d'un changement unilatéral côté gateway. L'option 2 ne touche que le gateway, ne nécessite aucun changement côté Mobile, et n'exige de conformité de forme que sur le point d'usage (l'en-tête `Authorization` envoyé à service-exe/service-not), pas sur la structure interne du JWT du gateway.

## Décision

Option 2 : le gateway retransmet le JWT brut de service-ida.

- `Actor` porte un champ `delegationToken` (le `accessToken` renvoyé par `POST /api/auth/login` de service-ida) — `ServiceIdaAuthenticationAdapter` le renseigne, `MockIdaAuthenticationAdapter` (fixture de test) porte une valeur factice.
- `JwtService.issue(Actor)` l'embarque dans le JWT du gateway sous une claim privée `idaToken`, uniquement si non nul (jamais régénéré — seul service-ida sait ce qui doit s'y trouver).
- `JwtReactiveAuthenticationManager` l'extrait à chaque requête authentifiée et le porte sur `AuthenticatedActor.delegationToken()`, disponible à tout contrôleur/adaptateur via `@AuthenticationPrincipal`.
- Usage prévu pour les futurs `RealExeAdapter`/`RealNotAdapter` (non construits ici, cf. items n°3/n°5 de la roadmap, encore bloqués par ailleurs par l'absence d'endpoint de liste côté service-exe/service-not) : envoyer `Authorization: Bearer <actor.delegationToken()>` vers ces services, jamais le JWT du gateway.

## Conséquences

- **Débloque le point n°6 de la roadmap.** Les items n°3/n°5 restent bloqués, mais uniquement par leur propre lacune (pas de endpoint de liste tenant-scopée côté service-exe/service-not) — la question JWT ne les bloque plus.
- **Surface d'exposition légèrement accrue** : si le JWT du gateway fuit, le token service-ida embarqué fuit avec lui. Le JWT du gateway porte déjà un privilège significatif (accès BUREAU/ADMIN) ; le risque marginal est jugé acceptable, mais à revisiter si le token service-ida porte un jour un privilège que le JWT du gateway ne porte pas déjà.
- Le token service-ida a une validité de 24 h côté service-ida, plus longue que celle du JWT du gateway (60 min par défaut, `fretcorridor.jwt.validity-minutes`) — le JWT du gateway expire toujours en premier, forçant une reconnexion avant que le token embarqué ne devienne obsolète. Aucun mécanisme de rafraîchissement côté gateway aujourd'hui (limitation préexistante, non traitée ici).
- Si l'équipe bascule un jour sur l'option 1 (secret partagé), ce mécanisme de retransmission devient inutile mais n'est pas un obstacle à la migration : `Actor.delegationToken()` peut simplement redevenir `null` partout, et le JWT du gateway lui-même transmis à la place.
