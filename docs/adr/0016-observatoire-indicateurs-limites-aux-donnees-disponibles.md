# ADR 0016 — L'observatoire de marché (EF-BUR-03) se limite aux indicateurs calculables avec les données déjà matérialisées

**Statut** : Accepté (décision produit, 2026-08-17)

## Contexte

`docs/CONFORMITE_web-socle.md` affirmait EF-BUR-03 déjà livré en Phase 1. Vérification faite en ouvrant la Phase 3 (sprint EF-BUR-05) : c'était inexact. Seul existait `AgregationMissionsService`, un comptage brut de missions par axe (EF-BUR-04, seuil d'agrégation), pas raccordé au gateway, sans aucun des indicateurs de marché du CDC.

Le CDC (§7.7, UC-BUR-02) décrit un observatoire riche : volumes par axe et par nature, prix médian et dispersion par axe et par type de véhicule, délais de parcours et leur variabilité, taux de retour à vide estimé, taux d'appariement et délai moyen jusqu'à appariement, saisonnalité, déséquilibres directionnels.

Le modèle de données réellement disponible (`MissionAppariee`, matérialisé depuis l'événement Kafka `AffectationConfirmee`) ne porte que : `axeId`, `transporteurId`, `origineNom`/`destinationNom`, `prixTransport`, `devise`, `confirmeeLe`. Aucune donnée de nature de marchandise, type de véhicule, délai de parcours réel (livraison vs enlèvement), retour à vide, ou date de publication de la demande d'origine.

## Décision

L'observatoire (`ObservatoireService`, service-bur) calcule uniquement les trois indicateurs que le modèle actuel permet de calculer honnêtement :

- **Volumes** — nombre de missions par axe (`nombreMissions`).
- **Prix observés** — médiane et dispersion (écart interquartile, plus robuste aux valeurs extrêmes qu'un écart-type sur un prix) par axe.
- **Déséquilibre directionnel** — part du trafic dans le sens dominant (0,5 = équilibré, jusqu'à 1,0 = tout dans un seul sens), déduite de `origineNom`/`destinationNom`.

Tous les trois restent gated par le même seuil d'agrégation (RG-085/EF-BUR-04) — masqués ensemble, pas indicateur par indicateur, puisqu'ils partagent le même effectif sous-jacent.

**Volontairement non construits, pas oubliés** :
- Segmentation « par nature » / « par type de véhicule » — aucun champ correspondant sur `MissionAppariee`.
- Délais de parcours et variabilité, taux de retour à vide — aucune donnée d'enlèvement/livraison réelle capturée à ce jour (seul `confirmeeLe`, l'horodatage de confirmation d'appariement, existe).
- Taux d'appariement et délai moyen jusqu'à appariement — exigerait l'horodatage de publication de la demande d'origine, non porté par `AffectationConfirmee`.
- Saisonnalité, comparaison de périodes, rapport périodique automatique — relèvent d'une couche d'analyse temporelle au-dessus des indicateurs de base, hors portée de ce sprint.

## Conséquences

- EF-BUR-03 est livré pour un sous-ensemble honnête du périmètre CDC, pas la totalité d'UC-BUR-02.
- EF-BUR-05 (couverture d'échantillon, RG-087 — « part du marché réel ») reste bloquée : aucune donnée de dénominateur externe (marché réel total) n'existe dans le système, avec ou sans ces trois indicateurs. Reste un sprint à part entière, non résolu par cette décision.
- Si Mobile/Moteur enrichissent un jour `AffectationConfirmee` (ou un nouvel événement) avec nature de marchandise, type de véhicule, ou horodatages d'enlèvement/livraison réels, `ObservatoireAxe`/`ObservatoireService` s'étendent sans changer le mécanisme de seuil ni l'API existante.
- `AgregationMissionsService`/`MissionRepositoryPort`/`BureauAgregatController` (ancien comptage, non raccordé au gateway) restent en l'état, non utilisés par ce nouveau code — pas supprimés, pas dans le périmètre de ce sprint.
