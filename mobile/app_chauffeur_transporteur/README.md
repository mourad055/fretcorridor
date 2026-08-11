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

## Lancer en local

```bash
docker compose -f infra/docker-compose.yml -f backend/gateway/docker-compose.gateway.yml \
  -f backend/service-ida/docker-compose.service-ida.yml up -d
cd mobile/app_chauffeur_transporteur
flutter run --dart-define=API_BASE=http://localhost:8082/api/v1
```
