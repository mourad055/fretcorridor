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
