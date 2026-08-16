# app_client

Porté par : Mobile (Personne 1).
Modules CDC : MKT, PAY (consult.), TRK (consult.), NOT (app client/chargeur).

Aucune logique métier n'est développée ici dans le cadre du périmètre Web (Personne 2). Voir `docs/PRD_FretCorridor_Web.md` §1.2.

## État (S11 — indicateur envoi consolidé) — ⚠️ MOCK, pas de backend réel

Phase 2, Sprint 11. Sur l'écran de suivi (`lib/screens/suivi_screen.dart`),
un bandeau informatif signale quand l'envoi du client fait partie d'une
tournée consolidée avec d'autres envois (groupage LTL) — purement
informatif, aucune action utilisateur associée.

**🧪 Entièrement mocké** : ni `ChronologieModel` (service-exe) ni aucun
contrat backend n'expose aujourd'hui cette information. Le mock est isolé
dans `lib/mock/consolidation_mock.dart` (commentaire explicite), dérivé
du `missionId` déjà connu par la chronologie existante — aucun appel
réseau supplémentaire. Le rendu par défaut (envoi non consolidé, ou avant
prise en charge) reste identique à avant ce sprint.

## État (S14 — choix du moyen de paiement) — ⚠️ MOCK, pas de backend réel

Phase 2, Sprint 14 ("Paiements Mobile Money étendus"), Volet Client.
L'écran `lib/screens/paiement_screen.dart` (jusqu'ici un simple état
d'attente honnête, S8) propose désormais un sélecteur MoMo / Orange
Money / Espèces avec confirmation — `lib/providers/choix_paiement_provider.dart`.

**🧪 Entièrement mocké** : le prestataire Mobile Money agréé n'est pas
encore intégré côté service-pay (Web). La sélection et la confirmation
restent entièrement locales, aucun appel réseau. À remplacer par une
vraie initiation de paiement dès que service-pay l'exposera — voir
commentaire en tête de `choix_paiement_provider.dart`.

## État (S15 — sélecteur d'axe) — ⚠️ MOCK, pas de backend réel

Phase 2, Sprint 15 ("Second axe & sécurité"), Volet Client. Sur l'écran
de publication de demande (`lib/screens/publier_demande_screen.dart`), un
sélecteur d'axe facultatif permet de préremplir les villes de départ et
d'arrivée — `lib/mock/axe_mock.dart`. La saisie libre des villes reste
disponible et fonctionne toujours (ex. axe non couvert par la démo).

**🧪 Entièrement mocké** : aucun endpoint `/axes` n'est exposé côté app
Client aujourd'hui (service-geo n'est branché que côté app
Chauffeur/Transporteur, S3) et aucun écran/provider "axe" n'existait ici
avant ce sprint. La liste d'axes est isolée dans `lib/mock/axe_mock.dart`,
aucun appel réseau. À remplacer par un vrai fetch dès que service-geo
exposera les axes pour ce module.
