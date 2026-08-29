# FretCorridor — Plan d'action réorienté (document unique, post-réunion chef de projet)
### Fusion du plan Moteur (Personne 3) et du brief Mobile (Personne 1) — à partager tel quel dans le groupe

> Document de travail, pas un remplacement du CDC v4.0 (`FSE-CDC-FRETCORRIDOR-2026-004`).
> Remplace les deux documents séparés qui circulaient (`plan_action_reorientation.md`
> côté Moteur, `brief-reorganisation-mobile.md` côté Mobile) — gardés comme historique,
> mais **celui-ci fait foi désormais**.

---

## 0. Répartition d'équipe

| Personne | Périmètre |
|---|---|
| **Personne 1 (Mobile)** | App Chauffeur/Transporteur + App Client (Flutter), IDA/CAP/MKT/FLT/EXE/NOT |
| **Personne 2 (Web)** | Portail Angular (Bureau/Transporteur/Admin), gateway, PAY/BUR/ADM |
| **Personne 3 (Moteur)** | GEO/MAT/OPT/TRK — aucune interface directe, consommé en API/événements |

---

## 1. Écart bloquant à trancher AVANT de coder le cœur du matching

Le CDC (EF-MKT-06/07) décrit : **une demande client reçoit au plus 3 propositions
classées, motivées, présentées au chargeur qui choisit.**

Les nouvelles instructions décrivent : **une demande est diffusée à tous les
chauffeurs compatibles ; le premier qui accepte l'obtient ; les autres voient la
notification disparaître (mais gardée en historique).**

Ce sont deux modèles différents, pas une extension l'un de l'autre :
- CDC v4.0 = le **client** choisit parmi des propositions classées par le moteur.
- Nouvelle instruction = les **chauffeurs** se disputent la demande, premier arrivé.

**À trancher en équipe, documenté dans un ADR (`docs/adr/`), avant d'implémenter le
mécanisme de matching lui-même.** Tout le reste de ce plan (UI, itinéraires,
compatibilité marchandises, historique, trajets préenregistrés, charte graphique) est
**indépendant de ce point et peut avancer sans attendre.**

---

## 2. État des lieux — ce qui est déjà fait (à ne pas refaire)

- **UC-MAT-02 (accepter/refuser une mission)** livré de bout en bout côté Mobile/Web
  cette session : service-opt (proposition unique, expiration, acceptation/refus),
  relais gateway, écran "Mes propositions" côté app Chauffeur. Implémente aujourd'hui
  le **modèle CDC strict** (1 candidat optimal par demande).
  - Le mécanisme "refus → remise en file d'attente pour le cycle suivant" **existe
    déjà** (`PropositionMissionService.refuser()` → `remettreEnFile()`,
    `MatchingCycleService` reprend automatiquement tout ce qui n'est pas `traitee`).
    Pas besoin de le reconstruire — **à adapter, pas à recréer**, si le modèle
    diffusion (§1) est retenu.
  - Si le modèle diffusion est retenu, l'écran "Mes propositions" côté Chauffeur n'a
    **aucun changement structurel à faire** : déjà en polling, filtre sur
    `EN_ATTENTE`, une proposition annulée disparaît toute seule au rafraîchissement
    suivant. Seul le backend (service-opt) devra évoluer pour gérer plusieurs
    candidats simultanés par demande — piège identifié : la remise en file de la
    **demande** doit attendre que *tous* les candidats vivants aient répondu (pas le
    premier refus), sinon risque de double-affectation sur la même demande.
- **Bug de connectivité réseau sur device physique corrigé** : pas un problème de
  port (8082 est le bon port gateway, confirmé par `docker-compose.gateway.yml`), mais
  les scripts `run.sh`/`run_dev.sh` codaient en dur un nom d'interface WiFi propre à
  une machine — détection rendue portable, fonctionne sur n'importe quelle machine
  d'équipe désormais.
- Tout ce travail est actuellement sur une branche locale (`feature/retours-ux-27-08`),
  en cours de découpage en branches dédiées + PR par sujet (voir §6).

---

## 3. Partie Chauffeur

| Besoin exprimé | Qui | Nature du travail | Dépendance |
|---|---|---|---|
| Matching sur 3 positions GPS (origine colis, destination colis, position **temps réel** du chauffeur) | **Moteur** (GEO/OPT) | Le filtre L0 utilise aujourd'hui la position **déclarée** de la capacité (statique). Il faut faire consommer par OPT la position temps réel (déjà publiée par FLT → TRK) au moment du cycle de matching — changement structurant, OPT doit interroger TRK en synchrone interne. | Aucune, mais impacte tout le reste (à faire en premier côté Moteur) |
| Refus d'une demande → rematching automatique vers un autre chauffeur | **Moteur** (mécanisme) | **Déjà en grande partie construit** (voir §2) — à adapter selon le modèle retenu (§1), pas à reconstruire. | Dépend de §1 pour la forme finale |
| Diffusion à tous les chauffeurs compatibles, premier acceptant gagne, historique conservé | **Moteur** (si modèle retenu, §1) + **Mobile** (rien à changer, voir §2) | Remplace le pattern "3 propositions au client". OPT publierait une proposition par chauffeur compatible, un événement d'acceptation invaliderait les propositions concurrentes (verrouillage atomique, éviter double-affectation). | **Bloqué par §1** |
| Nouvel espace/capacité déclaré → rematching sur demandes en attente + nouvelles | **Moteur** | **Déjà couvert** par `MatchingCycleService` (retraite tout ce qui n'est pas `traitee` à chaque cycle) — vérifier seulement le rayon d'appariement. | Aucune |
| Points d'arrêt dans le matching et l'itinéraire | **Moteur** (ValhallaClient) | Passer à un appel multi-legs (déjà anticipé dans le code, pas terminé). | Aucune |
| Aperçu itinéraire (km/temps ajoutés) avant acceptation | **Moteur** (calcul) + **Mobile** (affichage) | Nouvel endpoint synchrone on-demand `POST /api/opt/simulation-insertion` (réutilise l'ALNS existant en mode dry-run) côté Moteur ; écran de consultation côté Mobile, indépendant du calcul. | Affichage : aucune. Calcul réel : dépend de l'endpoint Moteur |
| Incompatibilité marchandises (ex. miroirs vs graviers/bois) | **Moteur** (MAT/OPT) | Règle de filtrage dure avant le calcul de coût, matrice de compatibilité en config (jamais codée en dur) — étend le pattern déjà existant EF-MKT-10. | Aucune, peut être fait en parallèle |
| Optimisation d'itinéraire + gestion des détours | **Moteur** (déjà largement construit, ALNS Sprint 11) | Reste à exposer en temps réel synchrone plutôt qu'en cycle différé de 30s. | Aucune |
| Interfaces façon Yango + charte graphique | **Mobile** | Voir §5 | Aucune, peut démarrer immédiatement |
| Écran swipe des demandes | **Mobile** | Écran actuel "Mes propositions" est à boutons — à faire évoluer en swipe | Aucune |
| Historique complet chauffeur (missions, livraisons, refus, actions) | **Mobile** | Lecture de données déjà exposées par OPT/EXE, affichage uniquement | Aucune |

---

## 4. Partie Client

| Besoin exprimé | Qui | Nature du travail | Dépendance |
|---|---|---|---|
| Prix sur poids volumétrique + distance, accepté/refusé par le client | **Moteur** (Tarification L4, déjà construite) + **Mobile** (écran) | Vérifier que le poids volumétrique (pas juste taxable) est la donnée source ; écran de consultation indépendant | Aucune, écran peut démarrer en parallèle |
| Suivi carte : position chauffeur = position colis une fois récupéré | **Moteur** (TRK) | `EtapeExecuteeEvent.typeEtape=ENLEVEMENT` existe déjà — probablement déjà exploitable, à vérifier avant de recoder | À vérifier avant de démarrer |
| Interfaces façon Yango + charte graphique | **Mobile** | Voir §5 | Aucune |
| Trajets préenregistrés nommés | **Mobile** | Nouvelle entité (service-mkt ou service-ida, à trancher), aucune dépendance Moteur | Aucune |
| Historique demandes/actions | **Mobile** | Lecture de données déjà exposées | Aucune |

---

## 5. Charte graphique — transversal aux TROIS périmètres

**Changement majeur, pas une évolution mineure.** Nouvelle charte (couleurs, typo,
logo) qui remplace l'existant codé — à appliquer pixel-perfect sur les deux apps
Mobile **et** le portail Web.

Sources (déposées à la racine du dépôt, dans
`FretCorridor color and type exploration.zip` — en cours de versionnement, voir §6) :
- `FretCorridor Charte Graphique.html` — charte complète.
- `FretCorridor App Client.html` / `FretCorridor App Transporteur (standalone).html`
  — maquettes pixel-perfect des deux apps mobile.
- `logo.png` — nouveau logo (3 variantes : badge rond + wordmark).

### Palette (tokens confirmés)
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
ET Web) — à faire en tout début de ce chantier, pas en improvisé au fil de l'eau.

---

## 6. Ordre de démarrage recommandé (par périmètre)

**Moteur** (Personne 3) :
1. Trancher l'écart §1 (condition bloquante pour dimensionner OPT/MAT).
2. Position GPS temps réel dans le matching — impacte tout le reste.
3. Refus → rematching (adapter l'existant, pas reconstruire — voir §2).
4. Multi-legs Valhalla + points d'arrêt.
5. Endpoint simulation d'insertion.
6. Matrice d'incompatibilité marchandises (indépendant, parallélisable).
7. Transition TRK "colis récupéré" (vérifier l'existant avant de recoder).

**Mobile** (Personne 1) :
1. Versionner charte graphique + ce document (voir §7) pour que tout le monde y ait accès.
2. Inventaire écran par écran des maquettes vs existant.
3. Sans dépendance Moteur, en parallèle : historique chauffeur, historique client,
   trajets préenregistrés, écran swipe, écrans de consultation (itinéraire, prix,
   suivi carte) — appliquer la nouvelle charte au fil de l'eau sur chaque écran touché.
4. Bouton refus / logique diffusion — bloqué par §1.

**Web** (Personne 2) : à définir avec la personne concernée — la charte graphique (§5)
la concerne directement, le reste de ce document ne couvre pas son périmètre en détail.

**Discipline Git commune aux trois périmètres** : branche dédiée par sujet, jamais de
commit direct sur `dev`, PR validée avant merge.

---

## 7. À faire pour que ce document soit utilisable par toute l'équipe

Pas encore fait au moment de la rédaction — sans ça, les fichiers cités ci-dessus ne
sont visibles que sur une seule machine :
- Committer + pousser ce document, la charte graphique (zip), le logo et les
  captures de référence dans le dépôt.
- Découper `feature/retours-ux-27-08` (travail déjà fait, encore local) en branches
  dédiées + PR par sujet.

---

*Document à ajuster dès que la décision du §1 est prise en équipe, et après
l'inventaire écran par écran du §5/§6.*
