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

## État (S10 — console de flotte)

Registre de véhicules réel — `lib/providers/vehicule_provider.dart`,
`lib/screens/vehicules_screen.dart`. Nouveau `POST/GET /api/v1/vehicules`
(gateway) → `VehiculeController` (service-flt, nouveau : ni entité ni
endpoint Véhicule n'existait avant ce sprint, malgré l'ownership théorique
de service-flt sur "Flotte, véhicules" au Plan d'Exécution §4.1).

Traité avant le S4 (capacité) dans cette série de commits : la déclaration
de capacité a besoin d'un `vehiculeId` réel, donc le registre doit exister
avant que l'écran capacité puisse compiler/fonctionner (voir S4 ci-dessous).

## Lancer en local

```bash
docker compose -f infra/docker-compose.yml -f backend/gateway/docker-compose.gateway.yml \
  -f backend/service-ida/docker-compose.service-ida.yml up -d
cd mobile/app_chauffeur_transporteur
flutter run --dart-define=API_BASE=http://localhost:8082/api/v1
```
