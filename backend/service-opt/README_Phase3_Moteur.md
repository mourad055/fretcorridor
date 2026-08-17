# FretCorridor v4.0 — Phase 3 : Densification et observatoire
### Périmètre Moteur (Personne 3) — Sprint 16 uniquement

> Suite de `README_Phase2_Moteur.md`. Périmètre nettement plus restreint cette
> phase : un seul sprint (S16, Oracle de chargement 3D) t'est attribué dans
> le Plan d'exécution §5.3/§8 — les 4 autres sprints (S17-S20) sont portés
> par Web/Mobile. Ce document couvre ce qui te concerne, et liste le reste
> pour information/coordination uniquement.
> Réf. : CDC `FSE-CDC-FRETCORRIDOR-2026-004` · Plan d'exécution V4.2 §5.3, §8, §9 · Feuille de route V4.2 §3.3

---

## 1. Objectif de la phase (rappel Plan d'exécution §5.3, §9)

> Moteur V2 complet (oracle 3D) ; observatoire de marché en production ;
> extension aux axes suivants selon sécurité et densité ; second tenant
> institutionnel.

**Jalon de sortie de phase** : second tenant sous licence, part de kilomètres
à vide en baisse mesurée, litiges sous les délais plafonds.

Sur ces trois critères de sortie, **aucun n'est directement porté par ton
lot** — le tien (oracle 3D) est une condition de qualité amont (chargement
faisable = moins de missions annulées/replanifiées), pas un livrable mesuré
par le jalon lui-même. Bon à savoir pour prioriser si le temps manque : ton
sprint n'est pas sur le chemin critique du jalon Go/No-Go de fin de phase.

---

## 2. Sprint 16 — Oracle de chargement 3D (moteur V2) : `service-opt`

Le poste de charge le plus incertain de tout le projet, déjà signalé comme
tel depuis le README Phase 1 : **aucune bibliothèque libre ne couvre le 3D
avec contraintes d'essieu** (Plan d'exécution §1, tableau stack technique).
Développement interne obligatoire.

### 2.1 Ce que couvre ce palier (rappel EF-MAT-13, palier V2)

D'après le README Phase 2 (§2.7) : *"V1 se limite à la capacité vectorielle
poids/volume pour la faisabilité de chargement, l'oracle 3D réel vient
après."* Ce sprint lève cette limitation : la vérification passe de "le poids
total tient dans la capacité" à "chaque colis a une position 3D réelle,
compatible avec les colis déjà chargés, à chaque état intermédiaire de la
séquence".

| Réf. | Exigence (résumé) | Notes |
|---|---|---|
| EF-MAT-13 | Plan de chargement exploitable restitué au chauffeur | Palier V2, priorité **S** (pas M) — déjà entamé en version simplifiée en Phase 2 (positions/orientations dans l'entité `PlanChargement`, cf. Plan d'exécution §3, modèle de données) |
| EF-MAT-07 (relecture) | Conformité des charges à l'essieu **à chaque état intermédiaire** | Ce sprint est celui qui rend cette exigence *réellement* vérifiée en 3D — en Phase 2 c'était une vérification vectorielle simplifiée, assumée comme palier transitoire |

### 2.2 Anti-patron à ne toujours pas commettre

Rappel explicite du CDC, déjà cité en Phase 1 et Phase 2, encore plus critique ici : **vérifier les charges à l'essieu uniquement sur le chargement complet est interdit**. L'oracle 3D doit simuler l'évolution du placement **à chaque étape** de la séquence (chaque enlèvement/livraison change la disposition physique du chargement), pas seulement valider l'état final.

### 2.3 Position dans le pipeline et budget de latence
V1 (Phase 2) : L0 filtrage → L1 affectation → L2 séquencement PDPTW/ALNS → Valhalla → L4 tarification
V2 (Phase 3) : L0 filtrage → L1 affectation → L2 séquencement → L3 oracle 3D → Valhalla → L4 tarification
Budget de latence L3 (déjà cité en Phase 2, rappelé ici car directement applicable) : **50 ms / 200 ms avec cache, jusqu'à 2 s sans cache**. Un ordre de grandeur plus serré que L2 (5s/30s) — cohérent avec le fait que L3 doit s'exécuter par état intermédiaire, potentiellement plusieurs fois par tournée.

### 2.4 Dégradation gracieuse — palier déjà anticipé en Phase 2

Le tableau des paliers P0-P3 du README Phase 2 (§2.8) couvre déjà ce cas :

| Palier | Déclencheur | Comportement |
|---|---|---|
| **P2 — Dégradé** | L3 dépasse son budget | Vérification par **capacité vectorielle seule** (retour au comportement V1), **groupage limité à deux demandes** |

Ce palier n'est donc pas à inventer ce sprint — juste à câbler réellement sur le nouveau code L3, avec le même principe déjà appliqué ailleurs (ENF-DIS-04) : l'indisponibilité/lenteur de l'oracle ne bloque jamais la mission, elle dégrade la qualité du plan de chargement de façon signalée, jamais silencieuse.

### 2.5 Recommandation méthodologique (cohérente avec le principe déjà suivi en V0/V1)

Vu le niveau d'incertitude technique le plus élevé du projet (aucune lib disponible), applique la même discipline que pour le moteur V0 et l'ALNS V1 : **prototype isolé avant intégration**, avec des **jalons de faisabilité intermédiaires** plutôt qu'un développement d'un seul tenant jusqu'à intégration complète. C'est explicitement recommandé par le CDC pour ce poste précis (cf. Plan d'exécution, tableau de charge indicative : *"prévoir jalons de faisabilité intermédiaires"*).

**Point ouvert à traiter avant de coder** : le CDC ne précise pas l'algorithme de placement 3D retenu (contrairement à L1/Kuhn-Munkres et L2/ALNS, tous deux nommés explicitement). Ni l'un ni l'autre document fourni (CDC, Plan d'exécution, Feuille de route) ne tranche entre une heuristique de bin-packing 3D classique (ex. wall-building, extreme points) et autre chose — à clarifier au point de synchronisation hebdomadaire avant de s'engager sur une approche, plutôt que de deviner.

---

## 3. Hors de ton périmètre en Phase 3 (pour information/coordination)

| Sprint | Module | Contenu | Porteur |
|---|---|---|---|
| S17 | BUR | Observatoire de marché : agrégats par axe, seuil d'agrégation, couverture d'échantillon | Personne 2 (Web) |
| S18 | IDA + gateway | Second tenant institutionnel : isolation renforcée, marque blanche | Personne 1 (Mobile, IDA) + Personne 2 (Web, gateway) |
| S19 | ADM | Back-office litiges : grille de décision versionnée, recours par opérateur différent, escalade automatique | Personne 2 (Web) |
| S20 | ADM + BUR | Exports PDF/Excel, rapprochement réglementaire | Personne 2 (Web) |

### Point de vigilance sur S18 (second tenant) — impact potentiel indirect sur GEO

`Axe` a déjà une isolation par `tenantId` réelle (`ENF-MUL-01`, filtrage en base via `AxeRepository.findByTenantId`, ajouté en Phase 2 pour S15). Un second tenant institutionnel pourrait donc déjà fonctionner côté GEO sans changement — **mais ni MAT ni OPT ni TRK n'ont de notion de tenant à ce jour** dans tout ce qu'on a construit ensemble jusqu'ici (ils sont appelés en synchrone interne par le même porteur, et les événements entrants comme `DemandePubliee`/`CapaciteDeclaree` n'ont jamais porté de `tenantId` dans les contrats vus). **À vérifier explicitement avant que S18 démarre** : est-ce que le second tenant doit être isolé au niveau du moteur aussi (deux tenants ne doivent jamais voir leurs demandes/capacités mélangées dans un même cycle de matching), ou est-ce que l'isolation GEO/IDA/gateway suffit parce que le moteur ne fait que réagir à des événements déjà scopés en amont ? Ni le CDC ni le Plan d'exécution fournis ne tranchent explicitement ce point pour le module Moteur — à poser au point hebdomadaire plutôt qu'à supposer dans un sens ou l'autre.

---

## 4. Garde-fous applicables (rappel, rien de nouveau ce sprint)

Les mêmes qu'en Phase 1/2 restent en vigueur, en particulier :
- **ENF-DIS-04** : l'oracle 3D ne bloque jamais une mission, il dégrade (palier P2 déjà défini, §2.4)
- **ENF-OBS-04** : reproductibilité des exécutions — s'applique aussi à l'algorithme de placement 3D si une composante heuristique/stochastique est retenue
- **Anti-patron essieu** : vérification à chaque état intermédiaire, jamais au chargement complet seul (§2.2)

---

## 5. Ordre de démarrage retenu

1. **Clarifier l'algorithme de placement 3D** avec l'équipe (point ouvert §2.5) avant d'écrire du code d'intégration — mais le prototypage isolé peut commencer sur une approche candidate sans attendre cette validation, dans le même esprit que le prototypage V0 en Phase 0.
2. **Prototype isolé** avec jalons de faisabilité intermédiaires, testé sur des cas synthétiques avant tout branchement au pipeline L0→L4.
3. **Intégration L3** dans le pipeline existant, avec le palier P2 câblé dès le départ (pas ajouté après coup).

*Aucune dépendance entre ton sprint et S17-S20 — tu peux avancer entièrement en parallèle, sous réserve de la clarification tenant du §3 si elle s'avère te concerner.*
