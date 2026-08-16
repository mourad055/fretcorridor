# app_client

Porté par : Mobile (Personne 1).
Modules CDC : MKT, PAY (consult.), TRK (consult.), NOT (app client/chargeur).

Aucune logique métier n'est développée ici dans le cadre du périmètre Web (Personne 2). Voir `docs/PRD_FretCorridor_Web.md` §1.2.

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
