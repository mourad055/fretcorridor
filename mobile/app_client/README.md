# app_client

Porté par : Mobile (Personne 1).
Modules CDC : MKT, PAY (consult.), TRK (consult.), NOT (app client/chargeur).

Aucune logique métier n'est développée ici dans le cadre du périmètre Web (Personne 2). Voir `docs/PRD_FretCorridor_Web.md` §1.2.

## État (S11 — indicateur envoi consolidé) — branché sur le backend réel (20 août)

Phase 2, Sprint 11. Sur l'écran de suivi (`lib/screens/suivi_screen.dart`),
un bandeau informatif signale quand l'envoi du client fait partie d'une
tournée consolidée avec d'autres envois (groupage LTL) — purement
informatif, aucune action utilisateur associée.

**Réel** : `tourneeId` (nullable) est exposé par `service-exe` sur
`GET /missions/demande/{demandeId}/chronologie` depuis la PR #82 (Volet
Chauffeur, S11) — `ChronologieModel` le porte désormais, aucun endpoint
ni appel réseau supplémentaire n'a été nécessaire côté Client, la donnée
était déjà là. `lib/mock/consolidation_mock.dart` supprimé. Le rendu par
défaut (envoi non consolidé, ou avant prise en charge) reste inchangé.

## État (S14 — choix du moyen de paiement) — branché sur le backend réel (20 août)

Phase 2, Sprint 14 ("Paiements Mobile Money étendus"), Volet Client.
L'écran `lib/screens/paiement_screen.dart` (jusqu'ici un simple état
d'attente honnête, S8) propose un sélecteur MoMo / Orange Money / Espèces,
rattaché à une mission précise (accessible depuis "Suivi" —
`suivi_screen.dart`, bouton "Choisir le moyen de paiement") —
`lib/providers/choix_paiement_provider.dart`.

**Réel pour MoMo/Orange Money** : `POST /missions/{id}/moyen-paiement` sur
`service-pay` (Item B, EF-PAY-06/07, livré 18 août) — appelé directement,
pas de gateway unifiée pour ce rôle (même principe que les autres
providers de ce dépôt). Les deux options envoient toutes deux
`MONNAIE_ELECTRONIQUE`, seule granularité connue du backend.
**Espèces reste confirmé localement** : hors périmètre volontaire de ce
choix a priori — mode dégradé décidé à l'enlèvement par le chauffeur
(EF-PAY-07), jamais planifié en amont dans l'app.

## État (S15 — sélecteur d'axe) — branché sur le backend réel

Phase 2, Sprint 15 ("Second axe & sécurité"), Volet Client. Sur l'écran
de publication de demande (`lib/screens/publier_demande_screen.dart`), un
sélecteur d'axe facultatif permet de préremplir les villes de départ et
d'arrivée — `lib/providers/axes_provider.dart`. La saisie libre des villes
reste disponible et fonctionne toujours (ex. axe non couvert par les axes
existants).

Appel direct à `service-geo` (`GET /api/geo/axes?tenantId=...`, confirmé
filtré réellement par tenant en base — ENF-MUL-01) via `geoDioProvider`
(`lib/providers/dio_provider.dart`) — pas de route gateway pour le rôle
Chargeur aujourd'hui, donc pas d'intermédiaire. `tenantId` vient de la
session (`authProvider`).

## État (S19 — signalement de litige) — ⚠️ MOCK, pas de backend réel

Phase 3, Sprint 19 ("Back-office avancé, litiges"), Volet Client. Depuis
l'écran de suivi (`lib/screens/suivi_screen.dart`, visible uniquement une
fois une mission apparue), un bouton "Signaler un litige" ouvre
`lib/screens/litige_screen.dart` — motif, description, référence mission
affichée en contexte.

**🧪 Entièrement mocké** : `service-adm` (Web) n'expose aucun contrat de
signalement de litige à ce jour. L'envoi est simulé (délai court, message
de confirmation) — isolé dans `lib/providers/litige_provider.dart`, aucun
appel réseau. À remplacer par un vrai POST une fois ce contrat exposé.
