# openapi

Spécifications OpenAPI 3 de chaque endpoint REST exposé, publiées au moment de l'implémentation (jamais après).

## Convention de routage réelle (périmètre Moteur, vérifiée sur le code)

Chaque service expose ses routes sous `/api/{module}/...` — **sans préfixe de version** (`/api/v1/...` n'existe dans aucun service du périmètre Moteur à ce jour) :

| Service | Préfixe réel |
|---------|--------------|
| GEO | `/api/geo/axes`, `/api/geo/hubs`, `/api/geo/zonage` |
| MAT | `/api/mat/couts` |
| OPT | `/api/opt/affectations`, `/api/opt/affectation-l1` (test), `/api/opt/filtrage-l0` (test) |

Ces routes sont consommées en synchrone interne (même porteur, Moteur) et ne passent donc pas par la gateway (portée par Web, §4.1 du plan d'exécution) — la question d'un éventuel préfixe `/api/v1/` imposé par la gateway pour les routes exposées cross-porteur reste à trancher avec Web/l'équipe avant toute implémentation, pas encore vérifiée contre le CDC dans cette session.

Erreurs au format RFC 7807.
