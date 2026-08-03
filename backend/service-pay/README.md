# service-pay

Porté par : Web (Personne 2) — mon périmètre.

Orchestration du paiement : interface avec le prestataire de paiement agréé, grand livre miroir, séquestre logique, réconciliation, contrôles ENF-FIN-01/02/03.

**Invariant non négociable** : FretCorridor n'écrit que dans un grand livre miroir, jamais de détention de fonds. Voir `docs/PRD_FretCorridor_Web.md` §0 et §5.5.
