# Oracle de chargement 3D — MAT/OPT V2 (Phase 3, Sprint 16)

## 1. Périmètre exact (CDC + Plan d'exécution)

Référence CDC : §8.7 (Oracle de chargement 3D), UC-MAT-01 flux nominal étape 5,
flux d'exception E1. Référence Plan d'exécution : Phase 3, Sprint 16.

> **CDC §8.7, poste le plus incertain du plan technique** — "aucune bibliothèque
> libre ne le couvre entièrement".

### Exigences fonctionnelles couvertes

| Réf. | Exigence (résumé) | Statut avant ce sprint |
|---|---|---|
| EF-MAT-05 | Consolidation de plusieurs demandes après vérification de faisabilité | ✅ Fait (Sprint 11, `AlnsSolver`) |
| EF-MAT-06 | Séquences enlèvement-livraison | ✅ Fait (Sprint 11, `Tournee`/`EtapeTournee`) |
| EF-MAT-07 | Vérification des charges à l'essieu à **chaque état intermédiaire** de la séquence | ❌ À faire — **ce sprint** |
| EF-MAT-08 | Retour à vide | ✅ Fait (Sprint 12, `PropositionRetourAVideEvent`) |
| EF-MAT-09 | Replanification (en figeant l'exécuté) | ✅ Fait (Sprint 12, `ReplanificationService`) |
| EF-MAT-13 | Plan de chargement exploitable restitué au chauffeur | ❌ À faire — **ce sprint** (priorité **S**, pas M) |

Ce sprint ne couvre donc que **EF-MAT-07** (vérification physique à chaque étape)
et **EF-MAT-13** (restitution du plan). EF-MAT-05/06/08/09 sont déjà acquis —
l'oracle 3D vient s'insérer **après** le séquencement ALNS (L2), pas le
remplacer.

## 2. Où l'oracle s'insère dans le pipeline existant
L0 (filtre H3) → L1 (Kuhn-Munkres) → L2 (ALNS, séquencement) → [ORACLE 3D] → L3/L4 (tarification)

D'après le CDC (UC-MAT-01, flux nominal, étape 5) :

> *"Pour les affectations impliquant une consolidation, le moteur invoque
> l'oracle de faisabilité de chargement (§8.7) et **rejette celles qui ne
> sont pas physiquement ou légalement réalisables**."*

L'oracle n'intervient donc **que sur les `Tournee` consolidées** (LTL,
`Tournee`/`EtapeTournee` déjà produites par `AlnsSolver`) — jamais sur une
`Affectation` FTL simple (un seul lot, pas de consolidation, rien à vérifier
physiquement). C'est cohérent avec la distinction déjà établie dans
`PropositionRetourAVideEvent` (`tourneeId`/`affectationId` mutuellement
exclusifs) — même logique ici : l'oracle ne s'applique qu'au cas `tourneeId`.

## 3. Ce que vérifie l'oracle, exactement (CDC §8.7 + entité PlanChargement, §13)

Rappel du modèle de données (CDC §13) :

> **PlanChargement** — Positions des colis, orientations, charges par
> essieu à chaque étape — **1-1 Mission**

Trois vérifications, à **chaque état intermédiaire** de la séquence
(pas seulement au chargement initial — un déchargement partiel en cours
de tournée change la répartition des charges) :

1. **Faisabilité volumique/poids** : les colis restants tiennent-ils dans
   le volume/poids résiduel du véhicule à cet état de la séquence ?
2. **Charge par essieu** : la répartition des colis ne dépasse-t-elle pas
   `chargeMaxParEssieuTonnes` (déjà présent dans `ProfilCamionDto`, porté
   depuis Sprint 5) sur aucun essieu, à aucun état intermédiaire ?
3. **Contraintes de gerbabilité/fragilité/danger** — le CDC §13 (entité
   `Lot`/`Colis`) mentionne "gerbabilité, fragilité, classe de danger" comme
   attributs du colis ; à vérifier si ces données sont déjà publiées par
   Mobile (service-mkt) avant de les exploiter — **point à valider au
   prochain point de synchronisation hebdomadaire (Feuille de route §5.1)**,
   pas une hypothèse à coder en dur.

## 4. Dégradation gracieuse (ENF-DIS-04) — flux d'exception E1 du CDC

> **E1 — Oracle de chargement indisponible.** *"Le moteur se restreint aux
> appariements sans consolidation et le signale. Il ne propose jamais un
> groupage dont la faisabilité n'a pas été vérifiée."*

Traduction en comportement de code, cohérente avec le reste du moteur
(même principe que `ValhallaClient`, `ServiceMatClient`) :

- Si l'oracle ne peut pas conclure (bug, timeout, cas non couvert par
  l'algorithme faute de bibliothèque adaptée) → **la `Tournee` concernée
  n'est jamais confirmée**, jamais une approximation optimiste.
- Le mode dégradé doit être **signalé explicitement** (même schéma que
  `CycleMatching.modeDegrade`, `TarificationResultat.modeDegrade`) —
  jamais un échec silencieux.
- Contrairement à MAT/Valhalla (où le mode dégradé laisse quand même
  passer un résultat partiel), ici le CDC est strict : **aucune
  proposition n'est émise** pour une combinaison jugée infaisable ou
  non vérifiable. C'est la seule brique du moteur où "dégradé" veut dire
  "bloquer cette tournée précise", pas "continuer avec une valeur par
  défaut".

## 5. Modèle de données à créer

Nouvelle entité `PlanChargement` (schéma `opt`), relation 1-1 avec
`Tournee` (pas avec `Affectation` — cf. §2 ci-dessus) :

- `id`, `tourneeId` (FK), `etapeId` (FK vers `EtapeTournee` — un
  `PlanChargement` par état intermédiaire, pas un seul pour toute la
  tournée)
- Positions/orientations des colis à cet état (structure à définir —
  dépend de ce que Mobile publie réellement sur les colis, cf. §3)
- `chargesParEssieu` (JSONB, cohérent avec le principe déjà établi
  "jamais de barème codé en dur")
- `faisable` (boolean), `motifRejet` (nullable)
- `modeDegrade` (boolean) — même convention que `CycleMatching`/
  `TarificationResultat`

## 6. Ce qui reste à trancher avant de coder (ne pas deviner)

1. **Format exact des colis publiés par Mobile** — le contrat
   `capacite-declaree.yaml` ne porte que la capacité globale
   (`capaciteResiduelleKg`, `volumeResiduelM3`), pas le détail colis par
   colis. Il manque un contrat `demande-publiee` enrichi ou un nouvel
   event pour les `Lot`/`Colis` individuels — **à vérifier avec Personne 1
   avant d'écrire le moindre algorithme de bin-packing**, sinon on
   développe contre des données qui n'existent pas.
2. **Choix de bibliothèque** — le CDC dit explicitement qu'aucune
   bibliothèque libre ne couvre le problème entièrement. Ça veut dire
   qu'une partie de l'algorithme sera fait main (heuristique de
   bin-packing 3D simplifiée), pas une intégration clé en main.
3. **Granularité de "chaque état intermédiaire"** — à définir précisément :
   un état par étape de la tournée (`EtapeTournee`) semble le plus
   cohérent avec le modèle CDC, à confirmer.

## 7. Prochaine étape

Avant d'écrire du code : lever le point #1 ci-dessus (contrat colis)
avec Personne 1, puisque c'est la seule chose qui bloque réellement le
démarrage — le reste (entité, dégradation gracieuse, insertion dans le
pipeline) est déjà cadré par ce document.
