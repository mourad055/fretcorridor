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
KYC, capacité, missions, GPS, notifications — sprints suivants.

## Lancer en local

```bash
docker compose -f infra/docker-compose.yml -f backend/gateway/docker-compose.gateway.yml \
  -f backend/service-ida/docker-compose.service-ida.yml up -d
cd mobile/app_chauffeur_transporteur
flutter run --dart-define=API_BASE=http://localhost:8082/api/v1
```
