# Récap session du 23 août 2026 — Audit + corrections

## Réponse directe : oui, tout ce que l'audit du jour signalait comme actionnable est corrigé

L'audit (`AUDIT_CDC_v4_complet_2026-08-23.md`) listait 4 corrections concrètes dans sa section
"Ce qu'il reste à décider" — les 4 sont faites. Ce qui restait volontairement hors périmètre
(S13, S14 réel, S17 UI, S20 exports, Oracle 3D complet, Phase 4) l'est resté : ce sont des
manques déjà connus, phasés plus tard dans le CDC lui-même, jamais demandés aujourd'hui.

## Demandé / fait

| # | Sujet | Statut | Détail |
|---|---|---|---|
| 1 | Audit croisé du rapport du coéquipier + du CDC | ✅ Fait | 4 audits indépendants (backend, mobile, web, re-vérif. des 18 bloquants du 19/08) — `AUDIT_CDC_v4_complet_2026-08-23.md` |
| 2 | Sync `dev` local / `origin/dev` | ✅ Fait | Était en retard, fast-forward sans conflit |
| 3 | Bug capacité résiduelle (jamais réutilisée après un match) | ✅ Corrigé | service-opt + service-cap, republication d'événement, testé |
| 4 | Endpoint `service-opt` sans clé interne | ✅ Corrigé | Alignée sur le pattern `X-Internal-Service-Key` |
| 5 | Création de tenant sans vérification de rôle | ✅ Corrigé | Réservé au rôle `ADMINISTRATION` |
| 6 | Poids taxable service-mkt (2 termes en dur) | ✅ Corrigé | Résolu par axe (RG-101), aligné sur service-cap |
| 7 | S19 — litige app_client (mock) | ✅ Corrigé | Contrat service-adm étendu (motif/description + délai par défaut), branché réellement |
| 8 | S16 — plan de chargement (mock) | ✅ Corrigé | Canal Kafka déjà publié mais jamais consommé → complété (service-exe + gateway), branché sur app_chauffeur, positions/orientations fictives retirées (le Moteur ne les calcule pas) |
| 9 | S18 — sélection de tenant (mock) | ✅ Corrigé | Décision produit prise avec toi (le second bureau invite/valide) ; table d'affiliation, JWT re-scopé, branché sur app_chauffeur. Portail web dédié **non fait** (gros morceau séparé) |
| 10 | Langue FR/EN (EF-NOT-05) | 🔶 En cours | Infrastructure réelle + écrans-socle des deux apps traduits ; écrans métier en cours (cf. ci-dessous) |

## Ce qui reste (en cours au moment de ce document)

Traduction FR/EN des écrans métier — infrastructure déjà posée et prouvée, reste la conversion
écran par écran :
- **app Client** : compléter profil, litige, mes demandes, notifications, paiement, promo,
  propositions, publier une demande, suivi
- **app Chauffeur/Transporteur** : enrôlement agent, axes, capacité, KYC, mes capacités, détail
  mission, tournée multi-étapes, missions, notifications, paiement, plan de chargement, promo,
  véhicules

## Portail web "second bureau" (S18)

Pas construit cette session — c'est un nouvel écran Angular complet (gestion des invitations
côté Bureau), à faire séparément si besoin. L'API backend est déjà prête et fonctionnelle
(`POST /api/v1/bureau/affiliations`), donc n'importe qui peut la brancher sans travail backend
supplémentaire.
