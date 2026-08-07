# Service de Matching (MAT)

Calcul du coût composite multi-critère pour l'affectation des demandes aux capacités.

## Fonctionnalités clés (MVP Phase 1)

- **Modèles de pondération versionnés** (EF-MAT-04)
  - Version figée, immuable (une ligne = une version)
  - Un modèle actif à la fois
  - Critères paramétrables en base (code libre, pas d'enum Java)
- **Calcul du coût composite** :
  - Somme pondérée des critères normalisés (0..1)
  - Pondération égale en mode dégradé (aucun modèle actif)
  - Persistance d'un `CycleMatching` par paire demandé×capacité
- **Traçabilité complète** (EF-MAT-11)
  - Détails des coûts stockés en JSONB
  - Version du modèle utilisé
  - Flag `modeDegrade`
- **Mode dégradé gracieux** (EF-MAT-12, ENF-DIS-04)
  - Ne plante pas si aucun modèle de pondération n'est actif
  - Pondération égale sur tous les critères fournis

## Exigences CDC couvertes

| Réf. | Description |
|------|-------------|
| EF-MAT-04 | Coût composite multi-critères, pondérations configurables et versionnées |
| EF-MAT-11 | Traçabilité reconstituable de chaque décision de matching |
| EF-MAT-12 | Signalement explicite du mode dégradé |
| §8.5.3 | Fonction de coût multi-critères |

## Dépendances

- `Spring Boot 3.3.4` + `Spring Data JPA`
- `PostgreSQL` (stockage JSONB pour détails des coûts)
- `common-libs` (DTOs partagés)

## API exposées

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| `/api/mat/couts/calculer-lot` | POST | Calcule les coûts pour un lot de candidats face à une demande |

## Consommé par

- **OPT** (affectation L1 – synchrone interne, même porteur)

## Contrat OpenAPI

`shared-contracts/openapi/mat-api.yaml`

## Statut

✅ MVP Phase 1 – Fonctionnel
