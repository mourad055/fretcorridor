# app_client

Porté par : Mobile (Personne 1).
Modules CDC : MKT, PAY (consult.), TRK (consult.), NOT (app client/chargeur).

Aucune logique métier n'est développée ici dans le cadre du périmètre Web (Personne 2). Voir `docs/PRD_FretCorridor_Web.md` §1.2.

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
