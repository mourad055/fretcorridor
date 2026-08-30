# FretCorridor — Plan d'action réorienté (document unique, source de référence équipe)
### Fusion du plan Moteur, du brief Mobile, et des retours du 29-30/08 — à partager tel quel dans le groupe

> Document de travail, pas un remplacement du CDC v4.0 (`FSE-CDC-FRETCORRIDOR-2026-004`).
> Remplace tous les documents séparés qui ont circulé (`plan_action_reorientation.md` et
> sa copie `(1).md` côté Moteur — vérifiées identiques, rien de nouveau dedans —,
> `brief-reorganisation-mobile.md` côté Mobile) — gardés comme historique, mais
> **celui-ci fait foi désormais**.

---

## 0. Répartition d'équipe — qui fait quoi, dans tout ce document

| Personne | Périmètre | Dossiers |
|---|---|---|
| **Personne 1 (Mobile)** | App Chauffeur/Transporteur + App Client (Flutter) | `mobile/app_chauffeur_transporteur/`, `mobile/app_client/`, `backend/service-ida/cap/mkt/flt/exe/not` |
| **Personne 2 (Web)** | Portail Angular (rôles Bureau/Transporteur/Admin) | `web/`, `backend/gateway`, `backend/service-pay/bur/adm` |
| **Personne 3 (Moteur)** | Matching, planification, optimisation, géospatial — aucune interface directe | `backend/service-mat/opt/trk/geo` |

Chaque section ci-dessous porte désormais un tag **Qui** explicite — c'est la correction demandée : la version précédente ne mentionnait quasiment jamais Personne 2 (Web).

---

## 1. Écart de modèle de matching — TRANCHÉ (ADR 0019)

**Qui : Moteur (décision), impact Mobile + Web.**

Le CDC (EF-MKT-06/07) décrit : une demande client reçoit au plus 3 propositions
classées, présentées au chargeur qui choisit. Les nouvelles instructions décrivaient
l'inverse : diffusion à tous les chauffeurs compatibles, premier acceptant gagne.

**Décision actée le 29/08** : modèle **diffusion-course** retenu, documenté dans
`docs/adr/0019-diffusion-course-vs-3-propositions.md`. Déjà implémenté côté Moteur
(commit `a2d3a92` sur `dev`, 66 tests OPT + 22 tests TRK) :

- `AffectationL1Service` diffuse une proposition à **tous** les chauffeurs compatibles
  (plus de "3 propositions au client").
- `AffectationConfirmationService.confirmerSiProposee` résout la course de façon
  atomique (compare-and-swap en base) — premier arrivé gagne, aucune double-affectation
  possible même si deux acceptations arrivent hors ordre Kafka.
- Deux contrats Kafka **Mobile → OPT** formalisés dans `shared-contracts/asyncapi/events/`
  (AsyncAPI 3.0.0), **publiés par service-mkt** (Mobile) :
  - `demande-acceptee.yaml` — chauffeur accepte. Champs : `eventId`, `affectationId`
    (clé de résolution de la course), `demandeId`, `capaciteId`, `transporteurId`
    (nullable, tolérance identique à `CapaciteDeclareeEvent`).
  - `demande-refusee-par-chauffeur.yaml` — chauffeur refuse. Mêmes champs,
    **`transporteurId` obligatoire** (sert à exclure ce chauffeur du prochain cycle
    pour cette demande, via `DemandeEnAttente.transporteursExclus`, migration V26).
- Refus → exclusion + remise en file : le chauffeur refusant est ajouté à
  `transporteurs_exclus` et jamais re-diffusé la même demande ; `MatchingCycleService`
  filtre les candidats exclus à chaque cycle.

**Conséquence directe pour Personne 1 (Mobile)** : les événements Kafka sont émis
côté **service-mkt**, pas via un appel REST gateway → service-opt. La PR #140
(UC-MAT-02, construite avant cette décision) implémentait le modèle CDC strict avec
un relais REST gateway — **obsolète, à fermer**, voir §3 pour le détail.

---

## 2. État des lieux (30/08)

**Déjà livré côté Moteur** (commit `a2d3a92`, `dev`) — couvre presque tout ce qui
était listé comme dépendant du Moteur dans les sections 4/5 :
- Position GPS temps réel du chauffeur dans le matching L0 (`ServiceTrkClient`).
- Refus → rematching automatique avec exclusion (`transporteurs_exclus`).
- Diffusion-course + résolution atomique (§1).
- Multi-legs Valhalla + points d'arrêt (`PolylineUtil`, `ValhallaRequestMapper`).
- Endpoint simulation d'insertion (`POST /api/opt/simulation-insertion`, JWT requis).
- Matrice d'incompatibilité marchandises (`CompatibiliteMarchandisesService`).
- TRK "colis récupéré = position chauffeur" (`ColisRecuperation`, `GET /api/trk/suivi/{missionId}`).

**Déjà livré côté Mobile** (branches dédiées, PR ouvertes sur `dev`, voir §8) :
- CRUD véhicule + upload photo carte grise (#135).
- Traduction titres notifications FR/EN (#136).
- Validation téléphone cohérente (#137).
- Formulaire capacité en popup (#138).
- Détection IP réseau portable, scripts `run.sh`/`run_dev.sh` (#139).
- **UC-MAT-02, modèle CDC strict (#140) — À FERMER**, superseded par §1. L'écran
  "Mes propositions" (UI, countdown, motifs de refus) reste réutilisable, mais son
  câblage data doit être refait pour appeler service-mkt (Kafka) au lieu de la
  gateway (REST) — travail restant, pas encore commencé.

**Côté Web** : des changements CSS/composants sont déjà présents sur `dev`
(`web/src/styles.css`, plusieurs composants `admin`/`bureau`/`transporteur`,
commit `9593246` et le merge `a2d3a92`) — à vérifier avec Personne 2 si c'est déjà
un début d'application de la nouvelle charte ou un sujet distinct, avant de dupliquer
le travail.

**Fichier `logo.png`** : déposé à la racine le 29/08 (nouveau logo, 3 variantes badge
rond + wordmark), retiré depuis (suppression volontaire). Vérifié : `web/public/assets/logo.png`
(déjà dans le dépôt) est l'**ancien** logo ("Fret" noir + "CORRIDOR" rouge, silhouette
de camion) — pas une version de secours du nouveau. Le nouveau logo doit être renvoyé
avant de commencer le remplacement (§7, §9).

---

## 3. Conception des maquettes d'interface — AVANT le codage

**Qui : Mobile + Web, chacun pour son périmètre. Étape obligatoire avant toute
implémentation d'écran, demandée explicitement en retour d'équipe.**

Avant de coder un écran touché par la nouvelle charte ou par une des remarques
ci-dessous :
1. Produire une maquette (wireframe ou maquette haute-fidélité, selon la complexité
   de l'écran) qui applique la charte graphique (§5) et le pattern d'interaction
   Yango pertinent (cf `FretCorridor App Client.html` / `FretCorridor App
   Transporteur (standalone).html` comme référence de comportement, pas de copie).
2. Faire valider la maquette avant d'écrire le code Flutter/Angular — évite de
   coder deux fois le même écran (une fois avec l'ancien style, une fois avec le
   nouveau).
3. Priorité aux écrans déjà identifiés comme à refaire : "Mes propositions"
   (rewiring §1 + nouvelle charte en une seule passe, voir §2), écran d'accueil,
   écran de suivi, écran de connexion.

**Pas encore fait** : aucune maquette produite à ce jour pour les écrans Mobile ou
Web au-delà de ce qui existe déjà dans le zip (`FretCorridor color and type
exploration.zip`, racine du dépôt) — cette étape est la première du chantier charte
graphique, avant tout code.

---

## 4. Partie Chauffeur (App Chauffeur/Transporteur)

| Besoin exprimé | Qui | Nature du travail | Statut |
|---|---|---|---|
| Matching sur 3 positions GPS (origine, destination, position temps réel chauffeur) | **Moteur** | `ServiceTrkClient` interroge TRK au moment du cycle | ✅ Livré (`a2d3a92`) |
| Refus → rematching automatique | **Moteur** | Exclusion via `transporteurs_exclus`, remise en file ciblée | ✅ Livré |
| Diffusion à tous les chauffeurs, premier acceptant gagne | **Moteur** (mécanisme) + **Mobile** (publier les événements) | Voir §1 | ✅ Moteur livré / ⏳ Mobile à refaire (câblage) |
| Nouvel espace/capacité déclaré → rematching | **Moteur** | Déjà couvert par `MatchingCycleService` | ✅ Confirmé |
| Points d'arrêt dans l'itinéraire | **Moteur** | Multi-legs Valhalla | ✅ Livré |
| Aperçu itinéraire avant acceptation | **Moteur** (calcul) + **Mobile** (écran) | Endpoint `simulation-insertion` prêt | ✅ Moteur livré / ⏳ Écran à faire |
| Incompatibilité marchandises | **Moteur** | `CompatibiliteMarchandisesService` | ✅ Livré |
| Optimisation d'itinéraire + détours | **Moteur** | ALNS déjà exposé en synchrone | ✅ Livré |
| Interfaces façon Yango + charte graphique | **Mobile** | Toucher en priorité les écrans à fort trafic : `home_screen.dart`, `login_screen.dart`, `propositions_mission_screen.dart` (déjà réécrit pour UC-MAT-02, PR #140 — sera refait de toute façon pour §1, autant faire charte + rewiring ensemble). Maquette d'abord (§3). | ⏳ À faire |
| Écran swipe des demandes | **Mobile** | `propositions_mission_screen.dart` actuel liste les propositions avec boutons Accepter/Refuser en colonne — sous diffusion-course, un chauffeur peut recevoir plusieurs propositions simultanées pour des demandes différentes (diffusion à tous les compatibles), donc une pile swipeable (type `Dismissible`/`CardSwiper`) est plus adaptée qu'une liste boutonnée. Le provider (`proposition_mission_provider.dart`) devra de toute façon être réécrit pour consommer service-mkt au lieu de la gateway REST (§1) — même chantier. | ⏳ À faire |
| Historique complet chauffeur | **Mobile** | Aucun écran d'historique consolidé n'existe aujourd'hui (`missions_screen.dart` ne montre que les missions actives/en cours). Lecture de données déjà exposées par service-opt (affectations passées) et service-exe (étapes exécutées) — nouvel écran, pas de nouvelle donnée à calculer côté backend. | ⏳ À faire, aucune dépendance |

---

## 5. Partie Client (App Client)

| Besoin exprimé | Qui | Nature du travail | Statut |
|---|---|---|---|
| Prix sur poids volumétrique + distance, accepté/refusé | **Moteur** (Tarification L4) + **Mobile** (écran) | Vérifié verrouillé sur poids volumétrique + distance (commit `bbe422f`, test dédié) | ✅ Moteur livré / ⏳ Écran à faire |
| Suivi carte : position chauffeur = position colis récupéré | **Moteur** (TRK) | `ColisRecuperation` + `GET /api/trk/suivi/{missionId}` | ✅ Livré |
| Interfaces façon Yango + charte graphique | **Mobile** | `home_placeholder_screen.dart` — son nom dit tout, jamais designé, priorité évidente. `welcome_screen.dart` (déjà refait avec vidéo hero Pexels) à re-thémer plutôt qu'à refaire. Maquette d'abord (§3). | ⏳ À faire |
| Trajets préenregistrés nommés | **Mobile** | Aucun écran ni entité existants (`publier_demande_screen.dart` ne connaît que la saisie libre). Nouvelle entité côté service-mkt (aligné avec `Demande`) ou service-ida (aligné avec le profil) — à trancher selon où vit le reste du profil client. Raccourci de saisie uniquement, pré-remplit les mêmes champs qu'une publication classique. | ⏳ À faire, aucune dépendance Moteur |
| Historique demandes/actions | **Mobile** | `mes_demandes_screen.dart` existe déjà mais liste les demandes actives (`propositions_screen.dart` gère les propositions reçues) — pas d'écran "historique" consolidé au sens strict. Lecture de données déjà exposées côté service-mkt. | ⏳ À faire, aucune dépendance |

---

## 6. Partie Web (Portail Angular)

**Qui : Personne 2 — la section la moins détaillée de ce document, faute de retour
direct de Personne 2 à ce jour. À compléter avec elle/lui.**

Ce qui est identifié comme la concernant directement :
- **Charte graphique (§5), transversale** — nouvelle palette/typo/logo à appliquer
  sur le portail Angular (rôles Bureau/Transporteur/Admin), cohérent avec les deux
  apps mobile. Composants centraux à toucher en priorité plutôt qu'écran par écran :
  `web/src/app/layout/shell/`, `shell-nav/`, `shell-sidebar/` (coquille visuelle
  commune à tous les rôles) et `web/src/app/shared/components/brand-logo/`
  (remplacement du logo).
- Des changements CSS sont déjà présents sur plusieurs composants (`admin/`,
  `bureau/`, `transporteur/paiement/`) dans les commits récents de `dev` — à vérifier
  avec Personne 2 si c'est déjà un début d'application de la charte, pour ne pas
  dupliquer le travail (voir §2).
- Aucun autre besoin exprimé dans les remarques collectées (présentation, WhatsApp)
  ne mentionne explicitement le Web — à confirmer que c'est bien voulu, pas un oubli.

---

## 7. Charte graphique — transversale aux TROIS périmètres

**Changement majeur, pas une évolution mineure.** Nouvelle charte (couleurs, typo,
logo) qui remplace l'existant codé — à appliquer pixel-perfect sur les deux apps
Mobile **et** le portail Web, après passage par une maquette (§3).

Sources — tout ce qui a été envoyé dans le groupe WhatsApp / déposé à la racine du
dépôt le 29/08, maintenant versionné (branche `docs/plan-reorientation-charte-graphique`) :
- `FretCorridor Charte Graphique.html` / `fretcorridor charte graphique.pdf` — charte
  complète (couleurs, typo, composants, grille, ton de voix, imagerie).
- `FretCorridor color and type exploration.zip` — contient en plus les maquettes
  pixel-perfect (`FretCorridor App Client.html`, `FretCorridor App Transporteur
  (standalone).html`) et les explorations de style.
- `Image collée.png` / `Image collée (2).png` — bannière hero et affiche de lancement
  (posts).
- **`logo.png` — retiré depuis, voir §2 pour le statut.**

### Palette (tokens confirmés — liste non exhaustive, la charte contient plus de
nuances par composant, à ressortir au moment de maquetter chaque écran)
- **Rouge corridor** `#FC312D` — aplats/CTA/titres ≥24px, jamais en texte courant sur
  fond clair.
- **Rouge roulé** `#C81F1B` — texte sur fond clair (contraste 4,5:1).
- **Encre** (fonds sombres) : `#0A0A0B`, `#141417`, `#1E1E20`, `#26262A`.
- **Blanc cassé / fonds clairs** : `#FAFAFB`, `#FBFBFA`, `#F0F0F2`, `#ECECEF`.
- **Gris ardoise/console** : `#55555C`, `#6E6E76`, `#8A8A93`, `#9A9AA2`.
- **Succès** ("MATCH 94%") : `#1FA054`, `#4ADE80`. **Alerte** : `#E88B00`.

### Typographie
- **Onest** 700 — wordmark, titres.
- **Newsreader** — italique éditorial.
- **Geist Mono** — toute donnée chiffrée (prix, distances, poids, %).

### Composants / ton
- Cartes de match façon "MATCH 94%", boutons pleins rouges pour actions primaires
  uniquement, jamais de dégradé dans le texte.
- Ton direct, chiffré, métier — pas d'emoji en interface.
- Imagerie terrain réel, jamais de stock générique ni d'illustration 3D.
- **Yango = référence de patterns d'interaction** (cartes de demande, flux de
  commande, carte de suivi), **pas une copie visuelle** — la charte FretCorridor
  ci-dessus prime pour couleur/typo/composant.

**Pas encore fait** : inventaire écran par écran des maquettes vs l'existant (Mobile
ET Web) — premier échantillon capturé le 29/08 (écran d'accueil + onboarding App
Client), pas exhaustif. À compléter avant de maquetter (§3) chaque écran.

---

## 8. Suivi des Pull Requests ouvertes

| PR | Sujet | Qui | Statut |
|---|---|---|---|
| #134 | Ce document + assets charte graphique | Mobile | Ouverte |
| #135 | CRUD véhicule + upload photo | Mobile | Ouverte |
| #136 | Traduction titres notifications | Mobile | Ouverte |
| #137 | Validation téléphone | Mobile | Ouverte |
| #138 | Formulaire capacité en popup | Mobile | Ouverte |
| #139 | Détection IP réseau portable | Mobile | Ouverte |
| #140 | UC-MAT-02 (modèle CDC strict) | Mobile | **À fermer** — superseded par §1 |

---

## 9. Ordre de démarrage recommandé (par périmètre)

**Moteur** (Personne 3) : terminé pour l'essentiel (§2) — reste à valider les
contrats Kafka avec Personne 1 (déjà fait dans ce document, §1) et à figer les
versions une fois le flux Mobile implémenté.

**Mobile** (Personne 1) :
1. Fermer la PR #140 (§2, §8).
2. Maquettes (§3) des écrans prioritaires — avant tout code.
3. Rebrancher "Mes propositions" sur service-mkt/Kafka **et** appliquer la nouvelle
   charte en une seule passe (§3, note de séquencement).
4. Sans dépendance Moteur, en parallèle : historique chauffeur, historique client,
   trajets préenregistrés, écran swipe, écrans de consultation (itinéraire, prix,
   suivi carte).
5. Confirmer à Personne 3 (Moteur) l'implémentation des contrats Kafka pour figer
   les versions (demande explicite de Personne 3).

**Web** (Personne 2) : à définir avec la personne concernée (§6) — la charte
graphique la concerne directement, le reste de ce document ne couvre pas son
périmètre en détail faute de retour à ce jour.

**Discipline Git commune aux trois périmètres** : branche dédiée par sujet, jamais de
commit direct sur `dev`, PR validée avant merge.

---

*Document à ajuster après la conception des maquettes (§3) et le rebranchement de
"Mes propositions" sur les contrats Kafka (§1, §9).*
