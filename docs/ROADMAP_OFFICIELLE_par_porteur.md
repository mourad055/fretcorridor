# Roadmap officielle par porteur — extraite du Plan d'Exécution V4.2

**Date** : 2026-08-18
**Source** : `docs/FretCorridor_Plan_Execution_V4_2.docx` §5 (modules par phase) et §9
(récapitulatif). Extraction fidèle, pas de réinterprétation — en cas de doute,
le `.docx` fait foi.

## Clarification terminologique (important)

Le "Phase 1/2/3/4" utilisé dans les échanges de cette session pour le
périmètre Web (`service-pay`/`service-adm`/`service-bur`) est une
**numérotation informelle**, propre au reliquat CDC de ce périmètre — **pas**
la phase officielle du projet. La phase officielle ci-dessous est **commune
aux 3 porteurs** (Mobile, Web, Moteur) et scande le projet entier en sprints
fixes (sauf Phase 4). Ne pas confondre les deux quand on échange avec un
autre porteur — c'est cette confusion qui a produit le malentendu avec le
lead Mobile sur "S14".

## Vue d'ensemble (§9 du Plan d'Exécution)

| Phase | Modules | Sprints | Durée indicative |
|---|---|---|---|
| 0 — Validation | Verrous V1-V5 (hors développement) | — | 10-12 semaines |
| 1 — MVP un axe | IDA · CAP · MKT · MAT/OPT V0 · TRK · EXE · PAY · FLT · BUR · GEO · NOT · ADM | S1 → S10 | 5-6 mois |
| 2 — Groupage + 2ᵉ axe | MAT/OPT V1 · FLT (connecteurs) · PAY (étendu) · GEO (2ᵉ axe) | S11 → S15 | 4-5 mois |
| 3 — Densification | MAT/OPT V2 (oracle 3D) · BUR (observatoire) · ADM (litiges) · INT | S16 → S20 | 4-6 mois |
| 4 — Anticipation | MAT/OPT V3 · GEO-05 (transfrontalier) | À définir par jalons | À définir |

**Phase 4 est exclusivement Moteur** (apprentissage/prévision de demande,
régionalisation transfrontalière). Aucun module Mobile n'y figure.

## Sprints touchant les apps Mobile (Chauffeur/Transporteur et Client) ou leurs services backend (IDA, CAP, MKT, EXE, NOT, FLT)

### Phase 1 — MVP (S1 → S10)

| Sprint | Fonctionnalité | Backend | App Chauffeur/Transporteur | App Client |
|---|---|---|---|---|
| S1 | Authentification & RBAC | gateway + `service-ida` | Écran connexion (téléphone+code), choix du rôle | Écran connexion, inscription légère chargeur |
| S2 | Profils & KYC | `service-ida` : Acteur/Organisation, upload MinIO | Formulaire KYC gradué (niveaux 1-2) + mode Agent | Formulaire profil chargeur |
| S3 | Réseau & Axes (GEO) | `service-geo` (Moteur) | Écran axes disponibles, verrous visibles | — |
| S4 | Capacité & Marketplace | `service-cap` + `service-mkt` (Mobile) | Écran déclaration de capacité | Écran publication de demande |
| S5 | Matching V0 | `service-mat`/`service-opt` (Moteur) | — | Écran propositions (≤3), acceptation/refus |
| S6 | Suivi GPS | `service-flt` (Mobile, ingestion) + `service-trk` (Moteur) | Service GPS arrière-plan, synchro offline | Carte de suivi temps réel |
| S7 | Exécution de mission | `service-exe` (Mobile) — consomme `AffectationConfirmee` | Écran mission en cours, capture preuve hors ligne | Chronologie de mission (lecture seule) |
| S8 | Paiement orchestré | `service-pay` (Web) | Écran solde / historique des gains | Écran paiement à l'acceptation |
| S9 | Notifications | `service-not` (Mobile) : FCM + WhatsApp BSP + SMS | Réception push (nouvelle mission, alertes) | Réception push (proposition, statut) |
| S10 | Back-office & flotte | `service-flt` (Mobile, console) + `service-adm` (Web) | Console de flotte simplifiée | — |

### Phase 2 — Groupage et second axe (S11 → S15)

| Sprint | Fonctionnalité | Backend | App Chauffeur/Transporteur | App Client |
|---|---|---|---|---|
| S11 | Consolidation LTL (moteur V1) | `service-mat`/`service-opt` (Moteur) | Écran mission multi-étapes | Indicateur "envoi consolidé" |
| S12 | Retour à vide & replanification | `service-opt` (Moteur) | Notification de mission retour | — |
| S13 | Connecteurs flotte tiers | `service-flt` (Mobile) | — | — |
| **S14** | **Paiements Mobile Money étendus** | **`service-pay` (Web)** : MTN MoMo + Orange Money, mode espèces signalé | **Affichage du mode de règlement reçu** | **Choix du moyen de paiement (MoMo / Orange / espèces)** |
| S15 | Second axe & sécurité | `service-geo` (Moteur) | Sélecteur d'axe | Sélecteur d'axe (destination) |

**S14 en détail, puisque c'est le sujet de la question du lead Mobile** :
le backend (endpoints `service-pay`) est explicitement assigné à **Web**
dans le plan officiel — cohérent avec ce qu'on s'est dit : je le construis,
Mobile intègre l'affichage/le choix côté apps une fois l'endpoint prêt. Les
deux prestataires nommés sont MTN MoMo et Orange Money (pas juste
"MONNAIE_ELECTRONIQUE" générique) — à garder en tête pour la conception.

### Phase 3 — Densification et observatoire (S16 → S20)

| Sprint | Fonctionnalité | Backend | App Chauffeur/Transporteur | App Client |
|---|---|---|---|---|
| S16 | Oracle de chargement 3D | `service-opt` (Moteur) | Restitution du plan de chargement | — |
| S17 | Observatoire de marché | `service-bur` (Web) | — | — |
| S18 | Second tenant institutionnel | `service-ida` + gateway | Sélection de tenant au login | — |
| S19 | Back-office avancé (litiges) | `service-adm` (Web) | Déclaration d'incident enrichie | Signalement de litige |
| S20 | Conformité & exports | `service-adm` + `service-bur` (Web) | — | — |

Phase 3 est majoritairement Moteur/Web — le seul vrai sprint Mobile est
**S18** (support multi-tenant, `service-ida` + sélection au login).

### Phase 4 — Anticipation et régionalisation

Aucun sprint fixe, aucun module Mobile. Contenu exclusif : `MAT/OPT V3`
(apprentissage sur traces propres, prévision de demande) et `EF-GEO-05`
(conventions bilatérales transfrontalières) — tous les deux Moteur.

## À retenir pour la coordination avec Mobile

- Le seul point de contact direct entre mon périmètre et Mobile sur les
  sprints à venir est **S14** (paiements Mobile Money étendus) — traité en
  parallèle dans `docs/DEPENDANCES_MOBILE_PHASE4.md` (dont le titre utilise
  ma numérotation informelle, pas la phase officielle — à garder en tête).
- Le prochain vrai point de contact Mobile après S14 est **S18** (second
  tenant), en Phase 3 officielle.
- Toute mention future de "S<n>" par un autre porteur se réfère à cette
  numérotation officielle — vérifier ici avant de répondre plutôt que de
  supposer une correspondance avec mon propre séquencement.
