# FretCorridor v4.0 — Phase 2, périmètre Moteur (Personne 3)
### Consolidation LTL, retour à vide, second axe — README de suivi

> Complète `README_module_optimisation_flux_stevetelecom.md` (Phase 1) plutôt
> que de le remplacer. Couvre les Sprints 11, 12, 15 du Plan d'exécution
> §5.2 — mon lot uniquement (MAT/OPT/GEO), rien côté Mobile/Web.
> Réf. CDC : `FSE-CDC-FRETCORRIDOR-2026-004` (v4.0), §8.6 à §8.8, §9.4.

---

## 1. Objectif de phase (Plan d'exécution §5.2)

Moteur V1 (séquencement, tournées multi-étapes, retour à vide) ; début du
moteur V2 (oracle de chargement, contraintes d'essieu, Phase 2-3 seulement) ;
ouverture d'un second axe.

**Critère de sortie** (CDC §8.13, trajectoire de maturité V1) : *"Tournées
multi-enlèvement et multi-livraison réelles"* — pas juste un algorithme qui
tourne, une preuve sur des cas réels.

---

## 2. Sprint 11 — Consolidation LTL (moteur V1) : `service-mat` + `service-opt`

### 2.1 Ce que dit le CDC — la vraie nature du problème (§8.6.1)

Point structurant à ne pas rater : **ce n'est pas un problème de tournées
classique**. La formulation retenue est le **PDPTW** (Pickup and Delivery
Problem with Time Windows), avec 4 contraintes absentes d'un VRP ordinaire :

1. **Appariement** — l'enlèvement et la livraison d'une même demande sont
   desservis par le **même** véhicule.
2. **Précédence** — l'enlèvement précède toujours la livraison.
3. **Capacité dynamique** — la contrainte porte sur le **maximum atteint le
   long du trajet**, pas sur une somme. Piège explicite du CDC : *"une
   tournée dont la somme des demandes est inférieure à la capacité peut être
   infaisable si les enlèvements se concentrent avant les livraisons"* — ne
   jamais valider juste `Σ poids ≤ capacité_max`, il faut simuler l'évolution
   de la charge point par point.
4. **Fenêtres temporelles souples**, sur l'enlèvement ET la livraison —
   **jamais dures** : *"une fenêtre dure produit une infaisabilité globale
   dès qu'un point aberrant existe... comportement inacceptable en
   exploitation, où une solution imparfaite vaut infiniment mieux qu'aucune
   solution."* → implémenter en pénalité (coût croissant avec le dépassement),
   pas en contrainte de rejet.

### 2.2 Méthode retenue (§8.6.2) — ALNS

**Recherche à grand voisinage adaptative** (Adaptive Large Neighborhood
Search) : détruire partiellement une solution (opérateur de retrait),
la reconstruire (opérateur d'insertion), accepter selon un critère type
recuit simulé, adapter dynamiquement la probabilité de sélection des
opérateurs selon leur succès passé. État de l'art sur cette classe de
problème depuis 2006 (CDC, source citée).

Couplage à prévoir dès l'architecture pour la contrainte d'essieu (Phase 2-3,
mais l'algorithme ALNS de base doit rester extensible pour ça) : ALNS pour le
routage, couplé à un algorithme de placement où chaque position candidate
violant la contrainte est rejetée itérativement.

### 2.3 Objectifs — un front de compromis, pas un optimum unique (§8.6.3)

Le CDC est explicite : *"L'orientation 'calculer le trajet le plus optimisé
pour le client' suppose un optimum univoque. Il n'y en a pas."*

| Objectif | Bénéficiaire | Conflit principal |
|---|---|---|
| Minimiser la distance totale | Transporteur, environnement | Contre le respect des fenêtres |
| Minimiser le nombre de véhicules | Transporteur | Contre le délai de chaque client |
| Minimiser le retard | Chargeurs | Contre la distance et le remplissage |
| Maximiser le remplissage | Transporteur, plateforme | Contre le délai (détours de collecte) |
| Minimiser le détour subi par un chargeur donné | Ce chargeur | Contre l'optimum collectif |

**RG-107 — Objectif composite pondéré et explicite.** Somme pondérée, coefficients
**configurables par axe et par tenant** (jamais en dur — même anti-patron
que MAT V0). Citation à retenir telle quelle pour la présentation : *"il
n'existe pas de 'trajet le plus optimisé' dans l'absolu ; il existe un trajet
optimal pour une pondération donnée, et cette pondération est une décision de
gestion, pas une propriété mathématique."*

### 2.4 EF-MAT-10 — le point qui manquait à ma checklist jusqu'ici

> **EF-MAT-10** (§9.4, priorité **M**) — *"Le système doit borner le détour
> imposé à une demande individuelle."*

Rattaché à **RG-108 — Détour individuel borné** (§8.6.3) et **RG-056 —
Détour borné** (§4, flux groupage) : *"Indépendamment de l'optimum collectif,
le détour imposé à une demande est plafonné (RG-056). Cette borne est une
contrainte d'équité : un système qui sacrifie systématiquement les mêmes
clients à l'optimum global perd ces clients, et l'optimum global devient
alors introuvable faute de demande."*

**RG-056** précise le mécanisme : *"Le détour imposé à une demande est
borné, **en distance et en délai**, par un paramètre d'axe."*

**Implémentation à prévoir** :
- Le seuil de détour (distance ET délai, deux bornes distinctes) vit dans
  `Axe.parametres` (JSONB, même pattern que `rayonAppariementKm` — ex.
  `detourMaxDistanceKm`, `detourMaxDelaiMinutes`), jamais codé en dur.
- **Contrainte dure dans l'ALNS**, pas une pénalité comme les fenêtres
  temporelles : une solution où une demande dépasse son détour maximal doit
  être **rejetée par l'opérateur d'insertion**, pas juste pénalisée dans le
  score — c'est une garantie d'équité, pas un objectif à arbitrer.
- Calcul du détour = `distance(trajet réel dans la tournée consolidée) −
  distance(trajet direct point-à-point de cette demande seule)` — nécessite
  l'appel Valhalla déjà en place (Sprint 5) pour le trajet direct de
  référence.

### 2.5 Exigences fonctionnelles couvertes (§9.4)

| Réf. | Résumé | Notes d'implémentation |
|---|---|---|
| EF-MAT-05 | Consolider plusieurs demandes sur un même véhicule après vérification de faisabilité de chargement | Dépend de L3 (Phase 2-3) pour la faisabilité complète ; en attendant, capacité vectorielle poids/volume (V0→V1 transitoire, cf §2.7) |
| EF-MAT-06 | Séquences respectant précédence enlèvement-livraison et fenêtres temporelles | Cœur de l'ALNS |
| EF-MAT-07 | Conformité des charges à l'essieu après chaque placement, à chaque état intermédiaire | Formellement L3 (oracle 3D), mais une vérification vectorielle simplifiée est le palier V1 assumé |
| EF-MAT-08 | Proposer des missions de retour aux véhicules en fin de mission | Sprint 12, cf. §3 |
| EF-MAT-09 | Replanifier une tournée engagée en figeant l'exécuté, revérifier la faisabilité résiduelle | Sprint 12, cf. §3 |
| **EF-MAT-10** | **Borner le détour imposé à une demande individuelle** | **Cf. §2.4 ci-dessus — contrainte dure ALNS** |

### 2.6 Budget de latence (CDC §8.10)

| Opération | Cible P50 | Cible P95 | Justification |
|---|---|---|---|
| Séquencement L2, une tournée | **5 s** | **30 s** | Métaheuristique bornée en temps |

À comparer au L1 déjà en place (Cycle L1 complet, 500 entités : 1s/5s) — le
séquencement est un ordre de grandeur plus lent, cohérent avec la complexité
PDPTW vs. simple affectation Kuhn-Munkres.

### 2.7 Palier de maturité — ce qu'on vise précisément ce sprint (CDC §8.13)

> **V1 — Séquencement** : *"PDPTW par ALNS, fenêtres souples, détour borné,
> replanification événementielle"* → *"Tournées multi-enlèvement et
> multi-livraison réelles"*.

**Ne pas confondre avec V2** (Oracle 3D, contraintes d'essieu vérifiées
incrémentalement, gerbabilité — Phase 2-3, EF-MAT-13, priorité **S** pas M) :
V1 se limite à la **capacité vectorielle** poids/volume pour la faisabilité de
chargement, l'oracle 3D réel vient après.

### 2.8 Dégradation gracieuse — paliers P0 à P3 (CDC §8.8)

Couplage maître/esclave : *"L2 propose, L3 vérifie, L2 réajuste."* Décidé à
froid, pas improvisé en prod :

| Palier | Déclencheur | Comportement | Signalement |
|---|---|---|---|
| **P0 — Nominal** | Budget respecté | L1 optimal, L2 ALNS complet, L3 exact | Aucun |
| **P1 — Dégradé léger** | L2 dépasse son budget | L2 s'arrête sur la meilleure solution courante | Cycle marqué |
| **P2 — Dégradé** | L3 dépasse son budget | Vérification par capacité vectorielle seule, groupage limité à deux demandes | Cycle marqué, alerte |
| **P3 — Repli** | L1 dépasse son budget | Heuristique gloutonne à rayon strictement borné | Alerte d'exploitation |

**P3 est le seul palier qui abandonne "jamais glouton" (EF-MAT-01)** — et
c'est volontaire, documenté, dernier recours seulement. À signaler
explicitement dans les logs/métriques (ENF-OBS) si ce palier est atteint en
pratique, jamais silencieusement.

---

## 3. Sprint 12 — Retour à vide & replanification : `service-opt`

| Réf. | Résumé |
|---|---|
| EF-MAT-08 | Proposition de retour à vide générée par OPT en fin de mission |
| EF-MAT-09 | Replanification **en figeant l'exécuté** — ce qui est déjà en cours ne doit jamais être recalculé rétroactivement |

Point de vigilance CDC (cohérent avec le principe déjà appliqué côté TRK,
Phase 1) : une replanification qui toucherait une étape déjà marquée
`EXECUTEE` casserait la confiance chauffeur/client — le figeage de l'exécuté
n'est pas une optimisation, c'est une garantie.

---

## 4. Sprint 15 — Second axe & sécurité : `service-geo`

| Réf. | Résumé |
|---|---|
| EF-GEO-04 | Surcouche de risque sécuritaire par segment, sur le second axe |

Réutilise le modèle `Axe.parametres` déjà en place — pas de nouvelle
structure de données, juste une nouvelle clé de configuration par segment.

---

## 5. Glossaire (CDC, annexe)

| Terme | Définition CDC |
|---|---|
| **ALNS** | Recherche à grand voisinage adaptative ; métaheuristique détruisant et reconstruisant partiellement une solution, avec adaptation dynamique des opérateurs |
| **PDPTW** | Problème de collectes et livraisons avec fenêtres temporelles |

---

## 6. Ordre de démarrage retenu pour cette phase

1. **Sprint 11 — ALNS + détour borné (EF-MAT-10)** : le plus gros morceau,
   cœur algorithmique. Coder la contrainte de détour **dès la conception**
   de l'opérateur d'insertion, pas en post-validation — c'est une contrainte
   dure du problème, pas un filtre a posteriori.
2. **Sprint 12 — Retour à vide & replanification** : dépend d'un ALNS
   fonctionnel (S11).
3. **Sprint 15 — Second axe** : indépendant, peut être avancé en parallèle
   si besoin (aucune dépendance sur S11/S12).

*Oracle 3D (V2, EF-MAT-13) volontairement hors de cette phase — Phase 2-3,
priorité S, pas M.*
