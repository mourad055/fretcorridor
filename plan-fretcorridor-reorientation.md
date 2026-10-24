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
(commits `a2d3a92`, `e3d153d`, `d836381` sur `dev`, 79 tests OPT + 22 tests TRK) :

- `AffectationL1Service` diffuse une proposition à **tous** les chauffeurs compatibles
  (plus de "3 propositions au client").
- `AffectationConfirmationService.confirmerSiProposee` résout la course de façon
  atomique (compare-and-swap en base) — premier arrivé gagne, aucune double-affectation
  possible même si deux acceptations arrivent hors ordre Kafka.
- Deux contrats Kafka **Mobile → OPT** formalisés dans `shared-contracts/asyncapi/events/`
  (AsyncAPI 3.0.0), **publiés par service-cap** (Mobile) — corrigé le 30/08 (commits
  `32a168f` côté contrats, `0b4e726` côté ce plan, Moteur) : la première version
  disait "service-mkt" par choix de nommage par défaut côté Moteur, écart relevé
  côté Mobile et confirmé par les deux. service-mkt est 100% côté client/chargeur
  (`DemandeController`, catalogue, aucun canal chauffeur, conforme EF-MKT-*) ;
  service-cap est le porteur des capacités et actions du chauffeur (EF-CAP-01/02/03/07/08),
  déjà émetteur de `capacite-declaree` via son `CapEventPublisher` — réutilisé ici :
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
côté **service-cap**, pas via un appel REST gateway → service-opt. La PR #140
(UC-MAT-02, construite avant cette décision) implémentait le modèle CDC strict avec
un relais REST gateway — **obsolète, à fermer**, voir §3 pour le détail. À ne pas
confondre avec le GET interne `service-opt → /api/opt/affectations/proposees`
(back-to-back protégé X-Internal-Service-Key, consommé par **service-cap**), qui
lui est légitime et livré (§2) : c'est l'écran "Mes propositions" qui s'appuie dessus
(lecture) en plus de publier les événements d'acceptation/refus vers service-cap.

---

## 1bis. Tarification par chauffeur — TRANCHÉ le 15/10, remplace l'hypothèse du §5

**Qui : décision produit (Responsable Flysoft, M. Claude Kuaté, WhatsApp 02/09 et
05/09) + Mobile (maquette + écrans) + Moteur (adaptation du moteur de tarification,
pas encore commencée).**

Le §5 ci-dessous (ligne "Prix sur poids volumétrique + distance") décrit encore
l'ancien modèle : un **barème unique partagé**, déjà implémenté côté `service-opt`
(`TarificationL4Service`/`BaremeTarification`, indexé par `axeId` + `typeVehicule`
uniquement, cascade à 4 niveaux, régimes `FORFAITAIRE_VEHICULE`/`POIDS_TAXABLE`,
commission plateforme, prix plancher, facteur de tension marché). **Ce modèle
contredit frontalement** la directive reçue du Responsable Flysoft : chaque
chauffeur configure son propre tarif (F/kg) par axe et par catégorie de
marchandise (Standard/Fragile/Ultra fragile), le client voit un intervalle
min/max calculé sur les chauffeurs actuellement compatibles.

**Décision actée le 15/10** : on garde l'idée de M. Kuaté (tarif par chauffeur).
C'est au Moteur d'adapter `TarificationL4Service`/`BaremeTarification` en
conséquence — **chantier non commencé**, nécessite une discussion de conception
avec Personne 3 (quel service calcule le prix final, qui stocke le tarif par
chauffeur, comment `Affectation.prixTransport` est alimenté). Spécification
complète : `spec-tarification-par-chauffeur.md` (racine du dépôt).

**Découverte du 15/10, qui change aussi la résolution géographique de l'axe** :
`service-geo` a déjà un référentiel **Hub + Axe + H3** bien plus abouti que prévu
(indexation hexagonale façon Uber) — `Hub` (GPS précis + index H3), `Axe` (lien à
sens unique entre deux hubs, 3 états indépendants visibilité/matching/paiement,
risque sécuritaire, répartition conventionnelle transfrontalière), déjà consommé
par OPT (`GET /api/geo/axes/actifs-matching`, `ServiceGeoClient`). **Le tarif par
chauffeur doit donc se rattacher à un `axeId` de ce référentiel existant**, pas à
une paire de villes inventée par le chauffeur — déjà pris en compte côté Mobile
(§ ci-dessous), reste à trancher côté Moteur pour le calcul de prix lui-même.

**Déjà livré côté Mobile, en simulation locale, en attendant le contrat backend**
(PR #142, #143, mergées) :
- **App Transporteur** : écran "Mes tarifs" (`mes_tarifs_screen.dart` +
  `tarif_axe_provider.dart`) — réutilise le vrai référentiel d'axes
  (`axesProvider`, déjà branché sur `GET /axes`), 3 tarifs F/kg par axe réel,
  actif/pause, retrait. Stockage **local uniquement** (aucun contrat backend pour
  "le tarif d'un chauffeur sur un axe" aujourd'hui).
- **App Client** : catégorie de marchandise (Standard/Fragile/Ultra fragile) à la
  publication, transmise au backend (`categorieMarchandise`, champ sans effet tant
  qu'il n'est pas consommé) ; écran Estimation (intervalle de prix simulé à partir
  du poids taxable **réel** de la demande) ; écran de blocage des fonds en 2 états
  (pré-autorisation puis capture, cohérent avec la demande "paiement bloqué, puis
  seul le prix du chauffeur qui accepte est prélevé") ; écran de diffusion avec
  compte à rebours 15 min et republication.
- **CNI du destinataire** : capture photo facultative à la publication (App
  Client) et étape de vérification à la livraison (App Transporteur,
  `mission_detail_screen.dart`) — les deux tolérants à l'absence de donnée, aucun
  endpoint d'upload/propagation réel à ce jour.
- Maquette complète (10 écrans + 2 prototypes récapitulatifs) construite et testée
  sur Claude Design, validée avant le code Flutter — cf. §3 pour la discipline
  maquette-avant-code, respectée ici.

**Mise à jour du 19/10 — conception technique TRANCHÉE avec Personne 3** :
OPT reste le seul moteur de prix (`TarificationL4Service`, commission/plancher/
tension inchangés). service-cap devient la source du **taux** : au L1, OPT
appelle service-cap en back-to-back (`GET /api/cap/tarifs/detail`) pour le
tarif de chaque candidat, nouveau régime `TARIF_CHAUFFEUR` (coût variable =
tarif × poids taxable, pas de facteur distance explicite). L'intervalle
client est exposé côté service-cap (`GET /api/cap/tarifs/intervalle`, ouvert,
pas de donnée personnelle). **Livré côté Mobile** (PR #145, mergée) : entité
`TarifChauffeur` + CRUD "Mes tarifs" + les deux endpoints de lecture interne
ci-dessus. **Livré aussi** : `categorieMarchandise` sur `Demande` et le
contrat `demande-publiee` (PR #146, mergée) — `app_client` l'envoyait déjà
depuis la PR #143, désormais pris en compte côté backend. **Reste côté
Moteur** (annoncé par lui, pas commencé à ce jour) : le client back-to-back
cap→opt et le régime `TARIF_CHAUFFEUR` dans `TarificationL4Service`.

**Résolution du mapping axe — TRANCHÉE ET LIVRÉE le 24/10.**
Le Moteur a livré `GET /api/geo/axes/resoudre?origineLatitude=&origineLongitude=
&destinationLatitude=&destinationLongitude=&tenantId=` côté `service-geo`
(PR #151, réutilise le zonage H3 déjà en place, contrat OpenAPI
`shared-contracts/openapi/geo-api.yaml` v1.2.0). 200 → `AxeResponse` ; 204 →
aucun axe (zone non couverte, hubs identiques, axe inactif) ; 400 →
coordonnées hors bornes WGS84. Endpoint public (`permitAll`), pas de JWT.

**Décision finale, clarifiée par le Moteur le 21/10 (corrige la décision du
19/10 ci-dessus, devenue obsolète) : tout le géocodage reste côté
`service-mkt`, jamais côté mobile.** `publier_demande_screen.dart` (App
Client) continue d'envoyer uniquement des noms de ville en texte libre,
**aucun changement mobile nécessaire** pour ce point. Raison : la politique
d'usage Nominatim (max 1 req/s, cache obligatoire, User-Agent identifiant,
licence ODbL) n'est tenable que centralisée au backend, jamais depuis
chaque téléphone (IP/débit non maîtrisables) — point que le Moteur a
soulevé et qu'on n'avait pas identifié initialement.

**Résolution à 3 niveaux, livrée dans `ServiceGeoClient.resoudreAxe`
(service-mkt, PR #148 + branchement final le 24/10)** :
1. Matching exact par nom de hub, normalisé accents/casse (`GET
   /api/geo/axes/actifs-matching`, déjà existant) — couvre l'immense
   majorité des cas, aucun appel externe.
2. Si échec : géocodage Nominatim de `villeDepart` et `villeArrivee`
   séparément (`NominatimGeocodingClient`, cache + limiteur de débit
   1 req/s, côté backend uniquement).
3. Si les deux coordonnées sont obtenues : appel à
   `GET /api/geo/axes/resoudre` avec ces coordonnées. 204 → dégradation
   gracieuse (`AXE_NON_DESSERVI`, déjà existant), jamais d'exception.

**Chantier du mapping GPS→axe entièrement clos.** Plus aucune dépendance
Moteur en attente côté Mobile à ce jour.

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
- **Lecture des propositions du chauffeur (complément 30/08, commits `e3d153d` +
  `d836381`)** : toute la partie "le chauffeur sait qu'une proposition lui est
  diffusée" manquait côté Moteur (rien côté Mobile à l'époque, constat relevé 30/08) :
  - `GET /api/opt/affectations/proposees?transporteurId=` (clé X-Internal-Service-Key,
    back-to-back par service-cap) → propositions PROPOSEE d'un transporteur, enrichies
    de `origineNom`/`destinationNom`/`statut`/`expireA` (migration V27 `transporteur_id`).
  - Événement **`proposition-diffusee-chauffeur`** (topic Kafka, publié par service-opt,
    partition par `transporteurId`) — notif en 2ᵉ temps, en bonus du GET.
  - **Timeout 15 min** (config `fretcorridor.opt.proposition-expiration-ms`, défaut
    900000) : `ExpirationPropositionService` (tâche planifiée) passe EXPIREE une
    proposition PROPOSEE dont `expireA` est dépassé, puis remet la demande en file si
    elle n'a plus aucune proposition active ni confirmée (migration V28 : `origine_nom`,
    `destination_nom`, `expire_a`). Contrat AsyncAPI `proposition-diffusee-chauffeur.yaml`
    à jour.

**Déjà livré côté Mobile** (branches dédiées, PR ouvertes sur `dev`, voir §8) :
- CRUD véhicule + upload photo carte grise (#135).
- Traduction titres notifications FR/EN (#136).
- Validation téléphone cohérente (#137).
- Formulaire capacité en popup (#138).
- Détection IP réseau portable, scripts `run.sh`/`run_dev.sh` (#139).
- **UC-MAT-02, modèle CDC strict (#140) — FERMÉE** (superseded par §1). L'écran
  "Mes propositions" (UI, countdown, motifs de refus) reste réutilisable, mais son
  câblage data doit être refait pour publier vers **service-cap** (Kafka,
  `CapEventPublisher`) au lieu d'appeler la gateway (REST) — travail restant, pas
  encore commencé (§9).

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
| Écran swipe des demandes | **Mobile** | `propositions_mission_screen.dart` actuel liste les propositions avec boutons Accepter/Refuser en colonne — sous diffusion-course, un chauffeur peut recevoir plusieurs propositions simultanées pour des demandes différentes (diffusion à tous les compatibles), donc une pile swipeable (type `Dismissible`/`CardSwiper`) est plus adaptée qu'une liste boutonnée. Le provider (`proposition_mission_provider.dart`) devra de toute façon être réécrit pour publier vers service-cap au lieu d'appeler la gateway REST (§1) — même chantier. | ⏳ À faire |
| Historique complet chauffeur | **Mobile** | Aucun écran d'historique consolidé n'existe aujourd'hui (`missions_screen.dart` ne montre que les missions actives/en cours). Lecture de données déjà exposées par service-opt (affectations passées) et service-exe (étapes exécutées) — nouvel écran, pas de nouvelle donnée à calculer côté backend. | ⏳ À faire, aucune dépendance |

---

## 5. Partie Client (App Client)

| Besoin exprimé | Qui | Nature du travail | Statut |
|---|---|---|---|
| ~~Prix sur poids volumétrique + distance, accepté/refusé~~ | **Moteur** (Tarification L4) + **Mobile** (écran) | **SUPERSEDED le 15/10, voir §1bis** : remplacé par un intervalle de prix calculé sur les tarifs individuels des chauffeurs compatibles, pas un barème unique. Poids volumétrique inchangé (toujours correct), distance n'intervient plus comme facteur explicite du calcul. | ⚠️ Voir §1bis |
| Suivi carte : position chauffeur = position colis récupéré | **Moteur** (TRK) | `ColisRecuperation` + `GET /api/trk/suivi/{missionId}` | ✅ Livré |
| Interfaces façon Yango + charte graphique | **Mobile** | `home_placeholder_screen.dart` — son nom dit tout, jamais designé, priorité évidente. `welcome_screen.dart` (déjà refait avec vidéo hero Pexels) à re-thémer plutôt qu'à refaire. Maquette d'abord (§3). | ⏳ À faire |
| Trajets préenregistrés nommés | **Mobile** | Tranché le 21/10 : entité `TrajetPreenregistre` côté `service-mkt` (aligné avec `Demande`). PR #150. | ✅ Livré |
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
ET Web) — échantillon capturé le 29-30/08 (rendu Chrome headless des fichiers HTML
du zip), pas exhaustif. À compléter avant de maquetter (§3) chaque écran.

Captures faites à ce jour et ce qu'elles confirment :
- **App Client** : accueil + onboarding ("Vous envoyez pour...?", 3 profils :
  Moi-même/Mon commerce/Mon entreprise) + saisie numéro avec clavier virtuel simulé.
- **App Transporteur, écran carte** : LE screen de référence pour le rebranchement
  diffusion-course (§9) — affiche simultanément plusieurs propositions ("DEMANDES
  COMPATIBLES · 3") en cartes empilées avec % de match ("MATCH 94%"), compte à
  rebours par carte ("expire 5 min"), delta km/temps par rapport à la mission en
  cours (+3 KM +12 MIN — exactement l'aperçu d'itinéraire de la §4), lien
  "Itinéraire →", et l'indication explicite **"balayer = refuser"** (confirme le
  pattern swipe demandé, item 4). Bandeau "MISSION EN COURS" toujours visible en
  haut pendant qu'on swipe les nouvelles propositions.
- **App Transporteur, écran Historique** : onglets Tout/Missions/Refus & manquées.
  Entrée type observée : *"Attribuée à un autre — Un transporteur a accepté avant
  vous · la notification a disparu, trace conservée"* — correspond exactement au
  comportement attendu par ADR 0019 (les affectations concurrentes expirent, restent
  en historique). Bon gabarit direct pour l'item "historique complet chauffeur" (§4).
- **App Transporteur, écran Trajets** : trajet en cours (jauge de capacité avec
  légende "Chargé au départ"/"Accepté en route"), CTA "Déclarer un espace" (item
  Chauffeur "nouvel espace/capacité déclaré"), texte explicatif confirmant le
  matching continu par GPS/points d'arrêt/marchandise.
- **App Transporteur, écran Profil** : contient un bloc **"Marchandises acceptées —
  filtre du matching"** avec des chips activables (Denrées/Cartons/Mobilier/Fûts &
  bidons/Vrac minéral/Dangereuse) — c'est l'écran de configuration manquant pour
  `CompatibiliteMarchandisesService` (item 1.9) : le chauffeur pilote lui-même les
  exclusions, pas juste une règle serveur invisible. Aussi : stats (missions
  réalisées, % conformité, litiges), état des documents véhicule (carte grise,
  assurance, visite technique).
- **App Client, écran Accueil** : bloc "Refaire un envoi" — cartes horizontales de
  trajets déjà faits, bouton "Refaire · dates seules" — c'est très proche de l'item
  "trajets préenregistrés" (§5), sauf que c'est basé sur l'historique réel plutôt que
  sur un nom donné à l'avance ; à clarifier si les deux mécanismes coexistent ou si
  un seul suffit. Aussi : suivi en direct avec barre de progression + ETA sur la
  carte d'accueil, système de palier gamifié ("Statut Argent, 12/15 avant Or").
- **App Client, onboarding** : parcours en paliers explicite (Découvrir → Publier →
  Payer & réserver, débloqués progressivement) — cohérent avec "Explorer d'abord —
  sans compte" de l'écran de bienvenue.

---

## 8. Suivi des Pull Requests ouvertes

| PR | Sujet | Qui | Statut |
|---|---|---|---|
| #134 | Ce document + assets charte graphique | Mobile | ✅ Mergée |
| #135 | CRUD véhicule + upload photo | Mobile | ✅ Mergée |
| #136 | Traduction titres notifications | Mobile | ✅ Mergée |
| #137 | Validation téléphone | Mobile | ✅ Mergée |
| #138 | Formulaire capacité en popup | Mobile | ✅ Mergée |
| #139 | Détection IP réseau portable | Mobile | ✅ Mergée |
| #140 | UC-MAT-02 (modèle CDC strict) | Mobile | ❌ Fermée — superseded par §1 (remplacée par #141) |
| #141 | UC-MAT-02/diffusion-course — accepter/refuser une proposition | Mobile | ✅ Mergée |
| #142 | App Transporteur — Mes tarifs par axe + vérification identité livraison | Mobile | ✅ Mergée |
| #143 | App Client — catégorie marchandise + CNI destinataire + parcours Estimation/Diffusion/Paiement | Mobile | ✅ Mergée |
| #144 | Doc : plan mis à jour (tarification par chauffeur, référentiel geo.axe) | Mobile | ✅ Mergée |
| #145 | service-cap — entité TarifChauffeur + CRUD + lecture interne | Mobile | ✅ Mergée |
| #146 | service-mkt — categorieMarchandise sur Demande + demande-publiee | Mobile | ✅ Mergée |
| #147 | Doc : conception tarification tranchée avec le Moteur, décision géocodage | Mobile | ✅ Mergée |
| #148 | service-mkt — normalisation accents/casse + géocodage Nominatim de repli | Mobile | ✅ Mergée |
| #149 | Mobile — onglets En cours/Terminées (Mes missions, Mes demandes) | Mobile | ✅ Mergée |
| #150 | service-mkt/app_client — trajets préenregistrés nommés | Mobile | ✅ Mergée |
| #151 | service-geo — exposer la résolution GPS vers axe | Moteur | ✅ Mergée |
| #152 | service-mkt — branchement final sur GET /api/geo/axes/resoudre | Mobile | Ouverte |

---

## 9. Ordre de démarrage recommandé (par périmètre)

**Moteur** (Personne 3) : terminé pour l'essentiel (§2), y compris la lecture des
propositions chauffeur (GET `/proposees` + `proposition-diffusee-chauffeur` +
timeout 15 min, commits `e3d153d`/`d836381`). Le flux "Mes propositions" côté
Mobile peut donc appeler **service-opt** (GET, back-to-back) et/ou consommer
**service-cap** (Kafka) — les deux chemins sont disponibles. Reste à valider/figer
les versions des contrats Kafka une fois le câblage Mobile réellement intégré
(demande explicite de Personne 3).

**Mobile** (Personne 1) — mis à jour le 21/10 :
1. ~~Fermer la PR #140~~ ✅ fait (remplacée par #141, mergée).
2. ~~Maquettes (§3) des écrans prioritaires~~ ✅ fait — 10 écrans + 2 prototypes
   récapitulatifs sur Claude Design, testés (Chrome headless, aucun bug), avant
   tout code Flutter.
3. ~~Rebrancher "Mes propositions" sur service-cap/Kafka~~ ✅ fait (#141).
4. ~~Tarification par chauffeur + CNI destinataire + parcours
   Estimation/Diffusion/Paiement~~ ✅ fait en simulation locale (#142, #143) — voir
   §1bis pour ce qui reste côté Moteur.
5. ~~Historique chauffeur, historique client, trajets préenregistrés~~ ✅ fait
   (#149, #150). L'écran "swipe des demandes" du §4 est abandonné au profit du
   modèle liste-de-cartes déjà construit (plus simple, déjà conforme à la
   diffusion-course) — à confirmer que ça convient toujours,
   sinon le reprendre en swipe.
6. ~~Conception technique du calcul de prix par chauffeur~~ ✅ tranchée et livrée
   (§1bis, #145/#146).
7. ~~Mapping axe géographique~~ ✅ tranché et livré le 24/10 — Personne 3 a
   livré `GET /api/geo/axes/resoudre` (#151), branché côté service-mkt
   (résolution à 3 niveaux : nom de hub → Nominatim → coordonnées, §1bis).
   Aucun changement mobile nécessaire, le formulaire de publication reste
   inchangé.

**Web** (Personne 2) : à définir avec la personne concernée (§6) — la charte
graphique la concerne directement, le reste de ce document ne couvre pas son
périmètre en détail faute de retour à ce jour.

**Discipline Git commune aux trois périmètres** : branche dédiée par sujet, jamais de
commit direct sur `dev`, PR validée avant merge.

---

*Mise à jour du 15/10 : maquettes (§3) et rebranchement "Mes propositions" (§1, §9)
faits. Nouveau point bloquant à ajuster après discussion avec le Moteur : le calcul
de prix par chauffeur et le mapping géographique de l'axe (§1bis).*
