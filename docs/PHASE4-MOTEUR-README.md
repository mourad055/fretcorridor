# FretCorridor v4.0 — Phase 4, périmètre Moteur (Personne 3)
### Anticipation et régionalisation — Moteur V3, transfrontalier — README de suivi

> Complète les README Phase 1/2/3 plutôt que de les remplacer. Couvre le
> palier V3 du moteur (CDC §8.13) et EF-GEO-05 — mon lot uniquement
> (MAT/OPT/GEO/TRK), rien côté Mobile/Web.
> Réf. CDC : `FSE-CDC-FRETCORRIDOR-2026-004` (v4.0), §3.3 (C2), §8.5.3,
> §8.11.2/8.11.3, §8.13, §9.9, §15 (ENF-RGPD-06), §16, §17.
>
> Chaque citation ci-dessous a été vérifiée directement dans le texte du
> CDC avant rédaction — pas de résumé approximatif repris d'une source
> tierce.

---

## 1. Objectif de phase (CDC §16, §17)

> *"4 — Anticipation et régionalisation : Moteur V3 : apprentissage sur
> traces propres, prévision de demande ; premiers axes transfrontaliers
> avec gestion des conventions bilatérales."*

**Durée : "À définir"** — contrairement aux Phases 1-3, aucun sprint fixe
n'est prévu. Le CDC traite explicitement cette phase par jalons de
faisabilité, pas par calendrier (cohérent avec le principe déjà appliqué en
Phase 0 pour les verrous).

**Critère de sortie officiel (§17, tableau récapitulatif)** :
> *"Économie unitaire positive à un taux de commission de 8 % ; conformité
> multi-juridiction établie."*

Deux volets bien distincts à ne pas mélanger dans le code : (A) l'
**apprentissage** (V3, interne au Moteur, aucune dépendance directe à un
nouveau pays) et (B) le **transfrontalier** (EF-GEO-05, dépend de partenaires
extérieurs et de conventions juridiques réelles). Rien n'impose de les
livrer ensemble.

---

## 2. Volet A — Moteur V3 : apprentissage (CDC §8.11.2, §8.11.3, §8.13)

### 2.1 Garde-fou de démarrage — non négociable

> *"Introduire de l'apprentissage sans données d'exploitation revient à
> apprendre du bruit."* (§8.5.3, section "Sur l'apprentissage automatique")

Ce n'est pas une formule rhétorique : c'est une **condition de démarrage**.
Le §8.13 est explicite — le palier V3 vient *"après que les données
d'exploitation suffisantes auront été accumulées"*. Concrètement : ne pas
lancer le moindre entraînement tant qu'un volume de traces réelles minimal
n'est pas confirmé — le CDC ne chiffre pas ce seuil, donc **à définir en
équipe avant de coder**, pas une valeur à inventer soi-même.

### 2.2 Ce que l'apprentissage calibre, pas ce qu'il remplace

> *"L'apprentissage ne remplace pas l'optimiseur combinatoire : il en
> calibre les coûts."* (§8.5.3)

Le moteur reste l'ALNS/Kuhn-Munkres déjà en place (Phases 1-2). V3 n'ajoute
pas un nouvel algorithme de décision — il **recalibre deux choses
précises** :

1. **Coûts d'arête** — la fonction de coût composite `CoutSolution`
   (Sprint 11, déjà en place) a 7 termes, tous déjà nommés dans le CDC
   (§8.5.3) : `km_approche`, `km_detour`, `ecart_temporel`,
   `gain_remplissage`, `fiabilite`, `risque_axe`, `valeur_retour`. **Deux
   de ces termes sont déjà prévus mais pas encore branchés côté code** :
   `risque_axe` (existe maintenant côté GEO depuis Sprint 15,
   `Axe.parametres.risqueSecuritaire` — à lire depuis `CoutSolution`) et
   `valeur_retour` (*"le seul terme prospectif... c'est celui qui distingue
   un moteur mature d'un moteur naïf"*, §8.5.3) — c'est spécifiquement ce
   dernier terme que V3 vient enrichir par apprentissage.
2. **Post-traitement d'ETA** — *"prédit non pas l'heure d'arrivée de bout
   en bout, mais le résidu entre la sortie du moteur d'itinéraires et
   l'arrivée réellement observée"* (§8.11.2 point 5). Concrètement :
   `EtaCalculator` (déjà en place depuis Phase 1, TRK) reste la base ;
   V3 ajoute une correction apprise sur le résidu, pas un remplacement.

### 2.3 Incohérence réelle dans le CDC — à trancher, pas à ignorer

Deux passages se contredisent sur le palier du post-traitement d'ETA :

- §8.11.2 point 5 : *"Elle appartient au palier **V2** (§8.13)."*
- §8.13 (tableau normatif) : *"**V3** — Post-traitement d'ETA par
  apprentissage sur traces propres..."*

**Le tableau §8.13 fait autorité** (c'est la référence normative de
phasage citée partout ailleurs dans le document, y compris par §8.11.2
lui-même). Retenu : **V3**, donc Phase 3-4 comme indiqué dans la colonne
"Phase" du tableau (*"3-4"*, pas une phase unique) — le post-traitement
d'ETA peut légitimement démarrer dès que la Phase 3 a accumulé assez de
traces, sans attendre formellement l'ouverture de la Phase 4.

### 2.4 RG-117 — Perte asymétrique (§8.11.3)

> *"Le modèle d'ETA doit être entraîné avec une perte asymétrique,
> pénalisant davantage la sous-estimation que la surestimation."*
> Justification du CDC, à retenir telle quelle pour toute présentation :
> *"arriver une heure après l'heure annoncée coûte au client une attente
> improductive et à la plateforme une réclamation ; arriver une heure avant
> coûte peu."*

Implication directe pour le code : ne jamais entraîner ce modèle avec une
fonction de perte symétrique standard (MSE/MAE) — la fonction de perte
doit pondérer différemment selon le signe de l'erreur.

### 2.5 Prévision de demande (§8.13, une phrase, jamais détaillée ailleurs)

> *"prévision de demande par axe et par créneau"*

C'est la seule mention de ce volet dans tout le CDC — aucune section
dédiée, aucun RG, aucun EF ne le détaille davantage. **À ne pas sur-
spécifier soi-même** : le CDC laisse volontairement ce point ouvert. Le
travail avant de coder est de définir ce périmètre en équipe (quel horizon
de prévision, quel usage — alimenter `MatchingCycleService` en
pré-positionnement suggéré ? informer le Bureau via BUR ?), pas une
supposition solitaire côté Moteur.

### 2.6 Exigence transversale liée — ENF-OBS-04 (déjà citée en Phase 1-2)

> *"Reproductibilité des exécutions du moteur à partir des entrées, de la
> configuration et de la graine aléatoire."*

S'applique intégralement à un modèle appris : la version du modèle entraîné
devient elle-même une donnée de configuration versionnée (même principe que
`ModelePonderation`/`BaremeTarification` déjà en place) — jamais un
artefact ML flottant sans version tracée.

---

## 3. Volet B — EF-GEO-05 : transfrontalier (§9.9, §3.3 C2, §15)

### 3.1 Texte exact de l'exigence

> **EF-GEO-05** (§9.9, priorité **M**) — *"Le système doit gérer plusieurs
> pays et plusieurs conventions bilatérales de répartition, chacune avec
> sa propre clé."*

### 3.2 RG-052 — Répartition conventionnelle (§9.9, corrigée en §3.3 C2)

> *"Sur les axes transfrontaliers soumis à convention bilatérale, le
> moteur doit respecter la clé de répartition applicable à cette
> convention précise — les clés diffèrent selon les pays. La clé est une
> donnée de configuration par convention, **jamais une constante**. En
> trafic intra-camerounais, aucune clé de nationalité ne s'applique."*

Chiffres réels cités par le CDC (§3.3, correction factuelle C2 — le CDC
v3.0 avait une clé unique 60/40 fausse, corrigée ici) :
- **Cameroun–RCA** (convention Douala, 22 décembre 1999) : **60 %
  centrafricains / 40 % camerounais**
- **Tchad–Cameroun** (convention Douala, 13 avril 1999) : **65 % tchadiens
  / 35 % camerounais** — *"l'axe le plus dense du réseau"*

**Rattaché à G4 (garde-fou non négociable, §4.5)** :
> *"Aucun barème tarifaire, ratio de conversion volumétrique, quota de
> répartition ou limite réglementaire n'est codé en dur : tout est
> configuration versionnée et auditée."*
> Vérification prévue par le CDC : *"Revue de code ; test de
> non-régression sur la configuration."*

C'est exactement le même patron que `Axe.parametres` (JSONB) déjà en place
depuis Phase 1 pour `rayonAppariementKm`, et depuis Phase 2 pour
`detourMaxDistanceKm`/`risqueSecuritaire` — **aucune nouvelle mécanique à
inventer**, juste une nouvelle clé de configuration versionnée par
convention.

### 3.3 Écart de modèle de données confirmé — à corriger avant de coder

Le CDC modélise `Hub` (§13) ainsi :
> *"Nœud du réseau : ville, plateforme, point de consolidation"*

**Aucun champ pays.** Or EF-GEO-05 exige de *"gérer plusieurs pays"* — sans
savoir à quel pays appartient un hub, impossible de déterminer quelle
convention bilatérale s'applique à un axe donné (ex. un axe Douala–Bangui
doit être reconnu comme relevant de la convention Cameroun–RCA, pas d'une
autre). C'est un vrai manque du modèle actuel, pas une supposition — à
combler par un champ `pays` sur `Hub` avant tout code de répartition.

### 3.4 Contrainte transversale bloquante — ENF-RGPD-06 (§15)

> *"Localisation par défaut des données sur le territoire national, et
> absence de transfert transfrontalier tant qu'aucune autorisation ne peut
> être obtenue. [...] C'est une contrainte d'architecture assumée : elle
> interdit le recours à certains services d'infrastructure hors territoire
> et doit être prise en compte dès le choix d'hébergement."*

Implication concrète pour GEO/TRK : même si un axe relie deux pays sur le
plan **logistique**, les **données** (positions GPS, journal d'audit,
identité) doivent rester hébergées sur le territoire national tant qu'
aucune autorisation formelle n'existe. Ce n'est pas un détail
d'infrastructure à traiter plus tard — le CDC le dit explicitement : *"où
elle est peu coûteuse, plutôt qu'après, où elle l'est beaucoup."* À
signaler dès maintenant à l'équipe infra (hors périmètre Moteur pour
l'implémentation d'hébergement, mais le Moteur ne doit jamais publier une
position/trace vers un service situé hors du territoire tant que
l'autorisation n'est pas confirmée).

---

## 4. Ce qui reste à trancher avant de coder (ne pas deviner)

1. **Seuil de données minimal avant d'activer l'apprentissage** (§2.1) —
   non chiffré par le CDC, à définir en équipe.
2. **Périmètre exact de la prévision de demande** (§2.5) — le CDC ne
   détaille rien au-delà d'une phrase, à cadrer avant de coder quoi que ce
   soit dessus.
3. **Champ `pays` sur `Hub`** (§3.3) — migration nécessaire côté GEO avant
   toute logique de répartition transfrontalière.
4. **Premier axe transfrontalier réel à modéliser** — le CDC ne prescrit
   pas un axe précis pour la Phase 4 (contrairement à Douala–Bafoussam
   explicitement cité pour la Phase 2). Les conventions existantes
   couvrent Cameroun–RCA et Tchad–Cameroun (§3.3) — à choisir avec l'équipe
   selon la même logique que Sprint 15 (densité, pas un axe arbitraire).
5. **Autorisation de transfert transfrontalier de données** (ENF-RGPD-06) —
   dépendance externe, hors périmètre Moteur, mais bloquante pour toute
   fonctionnalité qui impliquerait un partage de position/trace au-delà du
   territoire national.

---

## 5. Ordre de démarrage proposé

1. **Volet B d'abord si un axe transfrontalier réel est visé** (champ
   `pays` sur `Hub`, clé de répartition dans `Axe.parametres`, entité de
   traçabilité de la répartition appliquée — cf. contrat ci-dessous) :
   indépendant de l'apprentissage, réutilise des patrons déjà connus
   (JSONB versionné, journal d'audit type `journal_audit_risque`).
2. **Volet A (apprentissage) seulement une fois le seuil de données
   tranché** (§4.1) — sinon risque réel de coder un modèle qui *"apprend
   du bruit"*, exactement ce que le CDC met en garde.

*Rappel : ces deux volets sont indépendants dans le code. Ne pas les
coupler artificiellement — un axe transfrontalier peut fonctionner sans
apprentissage (V0/V1 suffisent pour le matching de base), et
l'apprentissage peut démarrer sur les axes existants (Douala–Yaoundé,
Douala–Bafoussam) sans attendre le transfrontalier.*
