# app_chauffeur_transporteur

Porté par : Mobile (Personne 1).
Modules CDC : IDA, CAP, MKT, FLT, EXE, NOT (app chauffeur/transporteur, mode Agent inclus).

## État (S1)

Connexion (téléphone + code) fonctionnelle de bout en bout contre le gateway réel
(`POST /api/v1/auth/login`, port 8082) — voir `lib/providers/auth_provider.dart`.
Nécessite le correctif gateway ajoutant les rôles `CHAUFFEUR`/`TRANSPORTEUR`/
`CHAUFFEUR_PROPRIETAIRE`/`AGENT`/`CHARGEUR` (`backend/gateway/.../domain/Role.java`
et `ServiceIdaAuthenticationAdapter`), sans quoi le gateway rejette ces rôles.

Pas encore fait : choix de rôle/mode Agent à l'écran de connexion (UC-IDA-03),
capacité, missions, GPS, notifications — sprints suivants.

## État (S2)

KYC gradué niveau 1 (particulier ou entreprise) fonctionnel de bout en bout —
voir `lib/providers/kyc_provider.dart` et `lib/screens/kyc_screen.dart`.
Affiché automatiquement après connexion, ré-accessible depuis l'écran
d'accueil (icône profil).

Nécessite le nouveau relais gateway `GET/PUT /api/v1/kyc/profil/**` →
service-ida (`backend/gateway/.../infrastructure/rest/ida/ProfilController.java`
+ `RealIdaProfilAdapter`) — cette route n'existait pas avant ce sprint, seule
la route admin (`/api/v1/admin/kyc/**`) était câblée.

Pas encore fait, faute de contrat backend (écart documenté, pas un oubli) :
- **Niveau 2** (pièces justificatives, upload MinIO) : `service-ida` marque
  explicitement ce niveau hors périmètre actuel.
- **Mode Agent** (enrôlement terrain) : aucun endpoint côté `service-ida`.

## État (S2 bis — mode Agent, UC-IDA-03/EF-IDA-06)

Enrôlement assisté par agent fonctionnel de bout en bout — voir
`lib/providers/agent_enrolement_provider.dart` et
`lib/screens/agent_enrolement_screen.dart`. Accessible depuis l'accueil
(bouton flottant) uniquement pour le rôle `AGENT`.

- Position + horodatage capturés à l'initiation (`geolocator`).
- File d'attente offline chiffrée (`flutter_secure_storage`) si pas de
  réseau à l'envoi — synchronisée automatiquement au retour de connexion
  (`connectivity_plus`) ou manuellement (icône de synchro).
- Le code d'activation (OTP) est envoyé par SMS à la personne enrôlée, pas
  à l'agent — c'est elle qui saisit le code et choisit son PIN à l'écran
  suivant, jamais l'agent (RG-019).

Écart volontaire par rapport à l'ancien repo v3
(`github.com/estie-glo/fretcorridor`) : là-bas l'agent choisissait
lui-même le PIN initial du chauffeur, ce qui viole RG-019.

Pas encore fait : priorité d'agent sur la file KYC admin (nécessite
service-adm), rémunération de l'agent conditionnée à l'activité réelle
RG-020 (nécessite service-pay) — voir `EnrolementAgent.acteurCreeId`
côté `service-ida`, point d'ancrage pour une future implémentation.

## État (S3 — axes)

`GET /axes` (`lib/providers/axes_provider.dart`, `lib/screens/axes_screen.dart`)
— nouvelle route gateway mobile (`/api/v1/axes`, tout acteur authentifié),
service-geo réel derrière. Verrous (matching/paiement inactifs) affichés,
jamais masqués (RG-012).

## État (S4 — capacité)

Déclaration de capacité fonctionnelle de bout en bout —
`lib/providers/capacite_provider.dart`, `lib/screens/capacite_screen.dart`.
`RealCapaciteDeclarationAdapter` remplace le TODO explicitement adressé à
`@estie-glo` dans `MockCapAdapter` (gateway) — mais seulement pour l'écriture
(`POST /api/v1/capacites`) : la vue de lecture Bureau/Transporteur
(`CapacitePort.listerParTransporteur`) reste mockée, aucun endpoint GET
équivalent n'existe côté `service-cap`.

Le `vehiculeId` requis par `CapaciteCreationRequest` vient du registre réel
de la flotte (S10, traité juste avant dans cette série de commits) plutôt
que d'un identifiant généré localement sur l'appareil.

## État (S6 — suivi GPS)

Envoi périodique de positions fonctionnel — `lib/providers/position_provider.dart`,
`lib/screens/suivi_gps_screen.dart`, relayé par `EnvoiPositionController`
(gateway, `/api/v1/positions`) vers `service-flt` (`POST /api/positions`,
déjà prévu côté service-flt : *"Sera appelée par l'app Chauffeur/Transporteur"*).

**Écart temporaire, dépendance connue sur le S7** : `service-flt` exige un
`missionId` valide, mais rien ne relie aujourd'hui une mission à son
chauffeur côté `service-exe` (aucun champ transporteur/chauffeur sur
`Mission`). L'écran demande donc l'identifiant de mission manuellement — à
remplacer par un déclenchement automatique une fois l'écran "mission en
cours" (S7) disponible.

## État (S7 — exécution de mission) — ⚠️ BLOQUÉ, PAS TERMINÉ

**Ce sprint n'est PAS livrable en l'état.** L'infrastructure ci-dessous
est écrite et testée (tests unitaires/intégration gateway et service-exe),
mais **"Mes missions" reste vide en conditions réelles** tant qu'une
dépendance externe (équipe Moteur, hors périmètre Mobile) n'est pas
résolue. Ne pas considérer ce sprint comme terminé tant que ce point n'est
pas réglé avec l'équipe Moteur.

Chronologie de mission côté chauffeur —
`lib/providers/mission_provider.dart`, `lib/screens/missions_screen.dart`,
`lib/screens/mission_detail_screen.dart`. Le suivi GPS (S6) démarre/s'arrête
automatiquement selon le statut (prise en charge/en transit → suivi actif,
livraison → arrêt), plutôt que la saisie manuelle temporaire de
`suivi_gps_screen.dart`.

Backend : `service-exe` n'exposait qu'un endpoint lecture seule côté Client
(`GET /missions/demande/{id}/chronologie`) — l'entité `Mission` n'avait
aucun lien avec un chauffeur (*"EF-EXE-02 complet... reporté à la
construction de l'app Chauffeur"*, commentaire du code). Ajouté :
- `AffectationConfirmeeListener` (Kafka) : `service-exe` consomme désormais
  `AffectationConfirmee` (même pattern que `service-bur`) pour créer la
  mission avec son `transporteurId`.
- `GET /missions/mes`, `GET /missions/{id}`, `POST /missions/{id}/etapes`
  (prise en charge/en transit/livraison/incident, avec vérification de
  propriété — un chauffeur ne peut agir que sur ses propres missions).

**🚫 Dépendance bloquante (pas un bug de ce dépôt)** : `AffectationConfirmee`
porte `transporteurId` à `null` en pratique aujourd'hui
(`AffectationL1Service`, `service-opt`, Moteur) — et en remontant la
chaîne, `service-cap` (`CapaciteCreationRequest`, S4) ne capture lui-même
aucun identifiant d'acteur à la déclaration. Tant que cette chaîne
(`service-cap` → `service-opt` → `service-exe`) n'est pas réparée,
**"Mes missions" restera vide en conditions réelles**, même si
l'infrastructure ci-dessus est prête et testée. `service-opt` n'est pas
mon périmètre (Moteur) — **à coordonner avec l'équipe Moteur avant de
considérer ce sprint comme terminé.**

## État (S8 — paiement)

Solde et historique des gains — `lib/providers/paiement_provider.dart`,
`lib/screens/paiement_screen.dart`. Lecture seule (ENF-FIN-01 : aucune
écriture depuis le mobile), consomme `service-pay` déjà réel côté gateway
(`PayReadPort`/`ServicePayWebClientAdapter`, construits avant ce sprint).

Seul ajout nécessaire : `GET /api/v1/paiement` (gateway), miroir de
`/api/v1/transporteur/paiement` sans la restriction au rôle TRANSPORTEUR —
celle-ci aurait exclu un CHAUFFEUR côté mobile.

## État (S9 — notifications)

Centre de notifications (liste, badge non lues, marquer comme lue) —
`lib/providers/notification_provider.dart`, `lib/screens/notifications_screen.dart`.
Nouvelle route gateway `GET/PATCH /api/v1/notifications/mes/**`
(`NotificationMobileController`) vers `service-not` réel.

**Écart assumé** : réception "tirée" uniquement (l'app va chercher ses
notifications) — pas de push FCM effectif (réception hors application,
notification système). Aucun projet Firebase (google-services.json /
GoogleService-Info.plist) n'est disponible pour ce dépôt ; `service-not`
a bien une entité `FcmToken` prête côté backend, mais rien ne peut envoyer
un vrai push sans ces identifiants.

## État (S10 — console de flotte)

Registre de véhicules réel — `lib/providers/vehicule_provider.dart`,
`lib/screens/vehicules_screen.dart`. Nouveau `POST/GET /api/v1/vehicules`
(gateway) → `VehiculeController` (service-flt, nouveau : ni entité ni
endpoint Véhicule n'existait avant ce sprint, malgré l'ownership théorique
de service-flt sur "Flotte, véhicules" au Plan d'Exécution §4.1).

Traité avant le S4 (capacité) dans cette série de commits : la déclaration
de capacité a besoin d'un `vehiculeId` réel, donc le registre doit exister
avant que l'écran capacité puisse compiler/fonctionner (voir S4 ci-dessous).

## État (S15 — sélecteur d'axe) — ⚠️ Second axe MOCK, pas de backend réel

Phase 2, Sprint 15 ("Second axe & sécurité"), Volet Chauffeur. L'écran
Axes (S3) permet désormais de sélectionner un axe actif (appui pour
sélectionner) — `lib/providers/axes_provider.dart` (`selectionner`),
`lib/screens/axes_screen.dart`. Sélection purement locale, aucun contrat
backend pour "l'axe actif d'un chauffeur" aujourd'hui.

**🧪 Second axe mocké** : `service-geo` (Moteur) n'expose aujourd'hui
qu'un seul axe réel par tenant (`GET /axes`, S3, inchangé). Un second axe
fictif (`lib/mock/axe_mock.dart`, badge "Démonstration" visible à l'écran)
est ajouté à la liste réelle pour permettre de construire et valider le
sélecteur multi-axes dès maintenant — à retirer dès que `/axes` renverra
plusieurs axes en conditions réelles.

## Lancer en local

```bash
docker compose -f infra/docker-compose.yml -f backend/gateway/docker-compose.gateway.yml \
  -f backend/service-ida/docker-compose.service-ida.yml up -d
cd mobile/app_chauffeur_transporteur
flutter run --dart-define=API_BASE=http://localhost:8082/api/v1
```
