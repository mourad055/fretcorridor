# Récap session du 24 août 2026 — Test live paiement (avant présentation)

## Bugs corrigés en direct pendant le test

| # | Sujet | Détail |
|---|---|---|
| 1 | Gateway → service-flt : URL par défaut fausse | `application.yml` de la gateway pointait sur `localhost:8083` (port interne du conteneur Docker), injoignable hors conteneur — le port hôte réel est `8092`. Gateway relancée avec `FRETCORRIDOR_SERVICE_FLT_URL=http://localhost:8092`. À corriger en dur dans `application.yml` si le lancement local (hors docker-compose) redevient la norme. |
| 2 | `service-cap` : suppression d'une capacité déjà décrémentée | `DELETE /capacites/{id}` échouait (contrainte FK `decrement_log_capacite_id_fkey`) dès qu'une capacité avait déjà été utilisée par un match. Corrigé dans `CapaciteService.supprimer()` : le journal d'idempotence (`decrement_log`) est nettoyé avant la suppression de la capacité. |
| 3 | `service-adm`/`service-pay` en local : mauvais port Postgres | Défaut `localhost:5432`, base réelle sur `5434` sur cette machine (port non-standard, cf. `docs/adr/0006`). Relancés avec `SPRING_DATASOURCE_URL` pointant sur 5434. |
| 4 | Donnée de test corrompue : poids max véhicule | Un véhicule créé pendant le test avait `profil_poids_max_tonnes = 50000` (saisi en kg au lieu de tonnes) → `numeric field overflow` à chaque déclaration de capacité pour ce véhicule (colonne `NUMERIC(6,2)` côté `cap.capacite`). Corrigé directement en base (50 tonnes). Pas un bug de code — juste une saisie de test. |
| 5 | `service-mat` : validation des critères de coût rejetait tout candidat réaliste | `CoutController.validerValeursCriteres()` imposait `[0,1]` aux 7 critères, y compris `KM_APPROCHE`/`ECART_TEMPOREL` qui sont des mesures physiques brutes (km, heures), pas des scores normalisés. Résultat : le calcul de coût L1 (Kuhn-Munkres) échouait en 400 dès qu'un candidat était à plus d'1 km ou 1h — cassait le matching pour quasiment tout scénario réel, pas juste les tests. **Corrigé et poussé sur `dev`** (commit `0a6441a`, avec le fix #2 ci-dessus). |

## Noté pour plus tard (pas fait aujourd'hui, faute de temps avant la présentation)

**CRUD complet sur "Mes véhicules"** (app Chauffeur/Transporteur) : aujourd'hui l'écran ne permet que de déclarer (créer) et lister. Manque : voir le détail d'un véhicule, le modifier, le supprimer. C'est ce qui a empêché de corriger la saisie erronée (#4) directement dans l'app — il a fallu un fix en base. Demandé par l'utilisatrice le 24/08, reporté explicitement à une session ultérieure.

**Double-décrément de la capacité résiduelle sur un match rang 1 (auto-confirmé)** : quand le matching confirme automatiquement le meilleur candidat (rang 1), `AffectationL1Service` (service-opt) réserve déjà la capacité (`serviceCapClient.reserver(..., missionId)`, cf commentaire "BUG CORRIGE, audit de suivi 23 août" dans le code). Mais la `Proposition` correspondante reste visible côté Marketplace en `EN_ATTENTE`, et quand le chargeur l'accepte manuellement, `DemandeService.accepterProposition()` (service-mkt) réserve la capacité **une deuxième fois** (`serviceCapClient.reserver(..., propositionId)`) — deux entrées distinctes dans `cap.decrement_log` (clés d'idempotence différentes : mission puis proposition) pour un seul match. Repéré le 24/08 sur une demande de 1 kg (résiduel passé de 9000 à 8998 au lieu de 8999) — sur un volume réaliste, l'écart serait doublé et visible. Fix probable : soit ne pas créer de `Proposition` "EN_ATTENTE" pour un rang 1 déjà auto-confirmé, soit faire vérifier à `accepterProposition()` qu'aucune affectation n'existe déjà pour cette demande avant de réserver. À investiguer proprement (touche la logique rang 1 vs rang 2/3), reporté à une session ultérieure sur demande explicite de l'utilisatrice.

## Suite le 25/08 (matinée, avant présentation)

| # | Sujet | Détail |
|---|---|---|
| 6 | Mobile : `IntlPhoneField` rejette silencieusement des numéros valides | La librairie applique une validation de longueur par défaut par pays, même sans `validator` explicite. Un rejet fait échouer `Form.validate()` sans jamais appeler le provider — bouton connexion/inscription inerte, ancien message d'erreur jamais effacé (symptôme confondu avec "erreur réseau" toute la matinée). Corrigé (`disableLengthCheck: true`) sur les 5 écrans concernés (login/inscription des deux apps + destinataire dans publier-demande). **Poussé sur `dev`** (commit `dcd9134`). |
| 7 | Mobile : `Geolocator.getCurrentPosition()` sans délai ni repli | Bloquait indéfiniment "Position GPS indisponible" en intérieur (déclaration de capacité + envoi de position pendant une mission). Ajout d'un `timeLimit` (8s) + repli sur `getLastKnownPosition()` dans `capacite_provider.dart` et `position_provider.dart`. **Poussé sur `dev`** (commit `dcd9134`, même commit que #6). |

**Non résolu — noté pour plus tard** : même après le fix #7, la position du chauffeur reste absente côté "Suivi" app Client alors que la mission passe bien en "Prise en charge" (confirmé côté serveur, `EtapeExecutee` publié). Aucune trace d'appel Geolocator dans les logs du téléphone juste après l'action — la cause exacte (pourquoi `demarrerSuivi()` ne semble pas déclencher `_envoyerUnePosition()` en pratique) n'a pas été identifiée faute de temps. Ne bloque pas le scénario de paiement (le bouton "Choisir le moyen de paiement" reste disponible), donc priorisé pour une session ultérieure.

## Suite le 26/08 — diagnostic GPS résolu, correction validation téléphone

| # | Sujet | Détail |
|---|---|---|
| 8 | Validation téléphone : correction excessive puis retour aux bons réglages | Le fix #6 (`disableLengthCheck: true`) désactivait *toute* validation de longueur, y compris la validation correcte par pays que fait `IntlPhoneField` par défaut. Corrigé en supprimant l'override sur les 5 écrans (login/inscription des deux apps + destinataire dans publier-demande) — la librairie applique déjà la bonne longueur par pays nativement (CM = 9 chiffres après +237, etc.), il ne fallait pas la contourner. **Poussé sur `dev`** (commit `3371713`). |
| 9 | Suivi GPS "jamais de position envoyée" — cause réelle : backend arrêté, pas un bug de code | Diagnostic approfondi avec traces temporaires (`debugPrint`) ajoutées à chaque étape de `_envoyerUnePosition()` dans `position_provider.dart`. Résultat : dès que le backend (12 services) et le tunnel `adb reverse` sont réellement up, le suivi fonctionne immédiatement — `getLastKnownPosition()` renvoie une position en ~20ms, l'envoi HTTP réussit (`_envoyer -> true`), confirmé par des lignes en base (`service_flt.positions`) toutes les 30s. Une ligne trouvée à 08:47 (avant le test explicite de ce matin) prouve que le suivi était déjà opérationnel dès que le backend a été relancé — la coupure du 25/08 était donc due à un arrêt du backend (mémoire libérée pour un rebuild, jamais relancé), pas à un défaut du code Flutter/Riverpod. Les fixes précédents (repli `getLastKnownPosition`, correctif Riverpod `initState`/`Future.microtask`) restent corrects et utiles mais n'étaient pas la cause racine du symptôme observé le 25/08. Traces de debug retirées après confirmation. **Poussé sur `dev`**. |

## Contexte : scénario de test suivi (`plan-mockups-2026-08-22.md`)

Session concentrée sur le scénario de test paiement de bout en bout (publier demande →
déclarer capacité → matching → accepter proposition → prise en charge → suivi → paiement).
Contraintes de la machine (7,5 Go RAM) et du réseau (WiFi changé en cours de session, USB
Android instable) ont nécessité plusieurs redémarrages du backend et des apps ; rien de lié
au code métier sauf les 4 points ci-dessus.
