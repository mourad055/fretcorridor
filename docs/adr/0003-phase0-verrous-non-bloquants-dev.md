# ADR 0003 — Démarrage du développement sans confirmation formelle de levée des verrous V1/V2

**Statut** : Accepté (décision explicite du sponsor, 2026-08-03)

## Contexte

Le CDC v4.0 (§16), le Plan d'Exécution (§7, §9) et le PRD (§10, règle d'exécution n°1) posent comme principe : « aucun développement de plateforme avant la levée des verrous de la Phase 0 », dont deux sont bloquants :
- V1 — lettre d'intention du Bureau de Gestion du Fret Terrestre (BGFT) sur un partenariat technologique et une licence de supervision.
- V2 — lettre d'intention d'un prestataire de paiement agréé en zone CEMAC, couvrant séquestre et reversement.

## Décision

Interrogé explicitement sur l'état de ces verrous avant le lancement de l'Étape 1, le sponsor a confirmé vouloir démarrer le développement immédiatement plutôt que d'attendre une confirmation formelle documentée de V1/V2.

## Conséquences

- Le développement du socle et des sprints démarre sans preuve documentée de V1/V2 dans ce dépôt.
- L'invariant financier (G1, ENF-FIN-01/02/03) reste appliqué dès le premier commit touchant `service-pay`, indépendamment de cette décision — ce garde-fou n'est pas une politique contournable par procédure (RG-075).
- `service-pay` est développé contre l'adaptateur *sandbox/mock* du prestataire de paiement (PRD §1.3), en attendant un partenaire réel.
- Si les lettres V1/V2 sont formalisées ultérieurement, les documenter ici en mise à jour de cet ADR plutôt qu'en créer un nouveau.
