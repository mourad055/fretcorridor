# Brief — Réorganisation Mobile (nouveau modèle de matching + nouvelle charte graphique)

> Document source pour la suite du projet côté Mobile (App Chauffeur/Transporteur +
> App Client). Fusionne le brief initial (déposé le 29/08 dans Téléchargements) et
> l'état des lieux technique établi le même jour. Miroir de `plan_action_reorientation.md`
> (Personne 3, Moteur), qui couvre GEO/MAT/OPT/TRK — celui-ci couvre les deux apps
> Flutter et ce qui dépend d'elles côté gateway.

> ⚠️ **Prérequis avant tout développement backend de matching** : décision produit
> actée de passer d'un matching par lots (RG-045, graphe biparti, déjà construit côté
> service-mat/service-opt) à un modèle de diffusion (broadcast à tous les chauffeurs
> compatibles, premier arrivé accepté, historique conservé). Le coéquipier Moteur a
> été prévenu et doit valider l'impact sur son module avant que le nouveau mécanisme
> de matching soit implémenté — il demande la même chose dans son propre document,
> avec un ADR (`docs/adr/`) à rédiger avant de coder. **Le reste de ce brief (UI,
> itinéraires, compatibilité marchandises, historique, trajets préenregistrés, charte
> graphique) est indépendant de ce point et peut avancer sans attendre.**

> ⚠️ **Concerne aussi le Web** : la nouvelle charte graphique (couleurs, typo, logo —
> voir section dédiée ci-dessous) n'est pas propre à Mobile. Le portail Angular
> (Personne 2) doit l'appliquer aussi pour rester cohérent avec les deux apps. Ce
> document ne couvre que le périmètre Mobile (Chauffeur/Transporteur + Client) — à
> partager avec Personne 2 pour qu'elle/il traite le volet Web séparément, avec ses
> propres écrans et son propre découpage de branches/PR.

---

## Charte graphique — SOURCE UNIQUE, pixel-perfect

Correction par rapport à une version précédente de ce brief qui disait "cohérent avec
la charte graphique existante" en parlant de l'existant *codé* (thème clair actuel des
deux apps) : **c'est l'inverse**. La charte a été **refaite** — c'est elle la référence
à suivre au pixel près, pas ce qui est déjà implémenté dans le code Flutter aujourd'hui.
Le logo a également changé.

**Sources (déposées à la racine du projet, dans `FretCorridor color and type
exploration.zip`)** :
- `FretCorridor Charte Graphique.html` — charte complète (couleurs, typo, composants,
  grille, ton de voix, imagerie).
- `FretCorridor App Client.html` — maquette pixel-perfect complète, app Client.
- `FretCorridor App Transporteur (standalone).html` — maquette pixel-perfect complète,
  app Chauffeur/Transporteur.
- `logo.png` (racine du projet) — nouveau logo, 3 variantes (badge rond + wordmark
  "fretcorridor").

### Palette (tokens confirmés, liste non exhaustive — la charte contient plus de
nuances par composant, à ressortir au moment de coder chaque écran)
- **Rouge corridor** `#FC312D` — aplats/CTA/titres ≥24px uniquement, jamais en texte
  courant sur fond clair.
- **Rouge roulé** `#C81F1B` — texte sur fond clair (ratio de contraste 4,5:1).
- **Encre** (fonds sombres) : `#0A0A0B`, `#141417`, `#1E1E20`, `#26262A`.
- **Blanc cassé / fonds clairs** : `#FAFAFB`, `#FBFBFA`, `#F0F0F2`, `#ECECEF`.
- **Gris ardoise / console** (texte secondaire, bordures) : `#55555C`, `#6E6E76`,
  `#8A8A93`, `#9A9AA2`.
- **Succès / "MATCH 94%"** : `#1FA054`, `#4ADE80`.
- **Alerte** : `#E88B00`.

### Typographie
- **Onest** 700 — wordmark, titres.
- **Newsreader** — italique éditorial (ex. "Le fret du corridor, enfin fluide.").
- **Geist Mono** — toute donnée chiffrée (prix, distances, poids, %) — cohérent avec
  le ton de voix "Chiffré : tonnes, heures, francs CFA, jamais bientôt".
- Aucune des 3 n'est dans le thème Flutter actuel (`GoogleFonts.workSansTextTheme` +
  `GoogleFonts.fraunces`, `google_fonts` déjà une dépendance). Onest et Newsreader
  sont sur Google Fonts. Geist Mono à vérifier — probablement asset `.ttf` local si
  absent de Google Fonts.

### Composants / ton
- Cartes de match façon "MATCH 94%" (visible dans `Image collée.png`, racine).
- Boutons pleins rouges pour actions primaires uniquement (Publier, Réserver) — pas
  de dégradé dans le texte.
- Ton direct, chiffré, métier — pas d'emoji en interface.
- Imagerie : terrain réel (camions, lumière naturelle), jamais de stock générique ni
  d'illustration 3D fantaisiste.

### Référence de style : Yango
Les deux apps doivent suivre la logique d'interface de Yango — à interpréter comme
référence de **patterns d'interaction** (cartes de demande, flux de commande, carte
de suivi), pas comme une copie visuelle exacte : la charte FretCorridor ci-dessus
prime pour tout ce qui est couleur/typo/composant.

**Pas encore fait** : inventaire écran par écran des deux maquettes vs les écrans
Flutter existants. Nécessaire avant de chiffrer précisément le travail — à faire en
premier quand on attaque ce chantier.

---

## État des lieux technique (29/08)

- **Corrigé** : bug de connectivité réseau sur device physique — pas un problème de
  port (8082 est correct, confirmé par `docker-compose.gateway.yml` et
  `application.yml` de la gateway), mais les scripts `run.sh`/`run_dev.sh` des deux
  apps codaient en dur un nom d'interface WiFi spécifique à une machine. Détection
  rendue portable (commit `3405159`, branche `feature/retours-ux-27-08`).
- **Backend UC-MAT-02** (accepter/refuser une mission, écran "Mes propositions")
  livré de bout en bout — commits `fa06863`/`a4d1d93`/`1c4bca5`, même branche.
  Implémente aujourd'hui le modèle CDC strict (1 candidat optimal, proposition
  unique). Si le modèle diffusion (§ prérequis ci-dessus) est retenu, l'écran "Mes
  propositions" n'a **aucun changement structurel à faire** — il est déjà en
  polling, filtre sur `EN_ATTENTE`, une proposition annulée disparaît toute seule
  au prochain rafraîchissement. Seul le backend (service-opt) devra évoluer.
- Tout ce travail est **local, non poussé** sur `feature/retours-ux-27-08` à ce
  stade — ce brief suppose qu'on repasse en mode branche dédiée + push + PR par
  item (cf. Découpage suggéré) maintenant que la présentation est passée.

---

## PARTIE 1 — App Chauffeur/Transporteur

### 1.1 Matching basé sur position GPS réelle (pas seulement l'axe)
Le matching doit prendre en compte trois positions : point de récupération du colis,
point de livraison, position GPS actuelle du chauffeur. Prendre en compte aussi les
points d'arrêt existants sur la mission en cours du chauffeur.
*(Moteur — voir `plan_action_reorientation.md` §2.1)*

### 1.2 Rematching automatique sur refus
Quand un chauffeur refuse une demande, relancer automatiquement le matching pour
l'attribuer à un autre chauffeur compatible, sans intervention manuelle.
*(Moteur, dépend de 1.1)*

### 1.3 Diffusion multi-chauffeurs avec disparition croisée
Une demande client est envoyée à tous les chauffeurs compatibles en même temps. Dès
qu'un chauffeur accepte, la notification disparaît chez les autres — mais reste en
historique pour eux (traçable, pas supprimée).
*(Moteur pour le mécanisme ; Mobile : écran déjà prêt, voir État des lieux ci-dessus)*

### 1.4 Rematching sur déclaration d'un nouvel espace/capacité
Quand un chauffeur déclare une nouvelle capacité, relancer le matching en tenant
compte des demandes déjà en attente ET des nouvelles.
*(Moteur — déjà en grande partie couvert par `MatchingCycleService`, à vérifier)*

### 1.5 Historique complet chauffeur
Accès à l'historique : missions effectuées, livraisons, demandes refusées, toutes
les actions faites dans l'application. Lecture de données déjà exposées par
OPT/EXE — travail d'écran uniquement.
**Aucune dépendance Moteur — peut démarrer immédiatement.**

### 1.6 Aperçu d'itinéraire avant acceptation
Cliquer sur une demande pour voir : nouvel itinéraire complet si acceptée, km ajoutés
par le détour, km/temps ajoutés spécifiquement pour rejoindre le point de
récupération.
**Partie affichage sans dépendance Moteur** (si la donnée existe déjà) — **le calcul
lui-même (simulation d'insertion) dépend de Moteur** (`POST
/api/opt/simulation-insertion`, pas encore exposé).

### 1.7 Interaction swipe sur les demandes
Sur l'écran d'accueil, swiper les demandes reçues (pattern notification-swipe) pour
accepter/refuser. Écran actuel "Mes propositions" est à boutons Accepter/Refuser —
à faire évoluer en swipe.
**Aucune dépendance Moteur — peut démarrer immédiatement.**

### 1.8 Optimisation d'itinéraire et gestion des détours
Itinéraire optimal avec temps/distance estimés ; impact de chaque détour affiché.
**Partie affichage sans dépendance Moteur** — le calcul (ALNS, Sprint 11) existe déjà
côté Moteur en cyclique, l'exposer en temps réel synchrone est côté Moteur.

### 1.9 Compatibilité marchandises dans le matching
Le matching doit refuser les combinaisons incompatibles (ex. miroirs vs graviers/bois).
Matrice de compatibilité par catégorie, pas un cas codé en dur.
*(Moteur — MAT/OPT, filtre pré-L1)*

---

## PARTIE 2 — App Client

### 2.1 Acceptation/refus du prix proposé
Prix calculé automatiquement (poids volumétrique + distance). Le client accepte ou
refuse. Tarification déjà couverte côté Moteur (`TarificationL4Service`) — vérifier
que le poids volumétrique (pas juste le poids taxable) est bien la donnée source.
**Écran côté Mobile sans dépendance bloquante — à vérifier avec Moteur en parallèle,
pas d'attente nécessaire pour démarrer l'écran.**

### 2.2 Trajets préenregistrés
Enregistrer un trajet (départ + arrivée) sous un nom personnalisé, accessible depuis
une icône dédiée dans le profil. Nouvelle entité côté `service-mkt` ou `service-ida`
(à trancher), aucune dépendance Moteur.
**Aucune dépendance Moteur — peut démarrer immédiatement.**

### 2.3 Historique client
Accès à l'historique des demandes et actions. Lecture de données déjà exposées.
**Aucune dépendance Moteur — peut démarrer immédiatement.**

### 2.4 Suivi carte avec transition colis/chauffeur
Position GPS chauffeur + position GPS colis sur la carte. Au moment de la
récupération, le statut passe à "récupéré" et la position affichée du colis devient
celle du chauffeur. TRK expose déjà `EtapeExecuteeEvent.typeEtape=ENLEVEMENT` —
probablement déjà exploitable côté app, à vérifier avant de demander du travail
Moteur supplémentaire.
**Aucune dépendance Moteur confirmée — à vérifier en premier, puis démarrer.**

---

## Découpage suggéré pour l'exécution (Claude Code)

Vu l'ampleur, ne pas tout attaquer d'un bloc. Ordre suggéré, chaque morceau avec sa
propre branche et PR :

1. **Sans dépendance Moteur, à faire en premier** : 1.5, 1.6 (partie affichage), 1.7,
   1.8 (partie affichage), 2.1, 2.2, 2.3, 2.4 — tout ce qui est UI/consultation,
   indépendant du mécanisme de matching sous-jacent. **Appliquer la nouvelle charte
   graphique (voir plus haut) sur chaque écran touché au fil de l'eau**, plutôt qu'en
   une seule passe séparée — évite de retoucher deux fois les mêmes fichiers.
2. **Dépend de la validation du coéquipier Moteur (ADR)** : 1.1, 1.2, 1.3, 1.4, 1.9 —
   tout ce qui touche au cœur du matching. Ne pas commencer tant que le point avec le
   Moteur n'est pas fait.
3. **Inventaire écran par écran** des deux maquettes (charte graphique) — à faire en
   tout début du point 1, pas en parallèle improvisé, pour ne pas découvrir les écarts
   au fur et à mesure.

Discipline Git : branche dédiée par morceau, jamais de commit direct sur `dev`, PR
validée avant merge.

---

## Hors périmètre Mobile (pour référence)

- GPS temps réel dans le matching, multi-legs Valhalla, simulation d'insertion,
  matrice d'incompatibilité marchandises, transition TRK — Personne 3 (Moteur), voir
  `plan_action_reorientation.md`.
- Tout ce qui touche gateway/PAY/BUR/ADM au-delà du relais déjà en place — Personne 2
  (Web).

---

*Document à ajuster dès que la décision de modèle de matching (§ prérequis) est prise
en équipe, et après l'inventaire écran par écran de la charte graphique.*
