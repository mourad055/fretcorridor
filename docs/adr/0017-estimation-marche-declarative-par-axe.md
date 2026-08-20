# ADR 0017 — EF-BUR-05 : couverture d'échantillon calculée contre une estimation de marché déclarative par axe

**Statut** : Accepté (décision produit, 2026-08-17)

## Contexte

L'ADR 0016 avait laissé EF-BUR-05 (« afficher la couverture d'échantillon de tout indicateur restitué », M) bloquée : RG-087 définit la couverture comme « la part du marché réel qu'un indicateur représente », et aucune source fiable de volume total réel par axe n'existe dans le système.

Vérification faite avant de rouvrir ce sprint (relecture CDC §7.7/§9.6 et §14) :

- La seule source externe plausible d'un vrai total marché — la plateforme numérique du bureau de fret étatique, censée porter des « statistiques » (§14.1) — est documentée comme **API non vérifiée, ni même son nom confirmé**. EF-INT-05 interdit explicitement de conditionner une fonction cœur à une intégration étatique (« son absence ne doit bloquer aucune fonction »).
- Aucune autre intégration externe du §14 (paiement, communication, cartographie, flotte tiers) ne fournit de volume de fret réel par axe.
- Le CDC exige que la couverture soit **affichée**, mais ne prescrit nulle part qu'elle doive provenir d'une intégration automatisée.

## Décision

`ObservatoireService` (service-bur) accepte une **estimation déclarative du volume mensuel réel** d'un axe, saisie par un agent Bureau (enquête terrain, dires d'experts) — `EstimationMarcheAxe(tenantId, axeId, volumeMensuelEstime, source, definieParActeurId, definieLe)`, une seule estimation active par axe, remplacée à chaque redéfinition.

La couverture affichée = (missions confirmées dans une fenêtre glissante de 30 jours ÷ estimation mensuelle déclarée) × 100, exposée uniquement si une estimation existe pour l'axe — sinon absente, jamais déduite silencieusement (cohérent avec la prudence méthodologique de RG-087).

**Fenêtre glissante de 30 jours, pas une vraie période sélectionnable** : `ObservatoireService` n'a aujourd'hui aucune notion de période choisie par l'analyste (UC-BUR-02 le prévoit, jamais construit, cf. ADR 0016). Sans borne temporelle, comparer « toutes les missions depuis le lancement de l'axe » à une estimation mensuelle grimperait indéfiniment et perdrait tout sens. La fenêtre de 30 jours est le minimum nécessaire pour rendre le ratio interprétable, pas une implémentation de la sélection de période du CDC.

Endpoints ajoutés :
- `PUT /api/v1/bureau/observatoire/{axeId}/estimation-marche` (gateway) — acteurId/tenantId toujours du JWT (ENF-MUL-01), body `{volumeMensuelEstime, source}`.
- `PUT /api/v1/bur/estimation-marche` (service-bur, interne, appelé par le gateway).

## Conséquences

- EF-BUR-05 est livré : tout indicateur restitué affiche sa couverture dès qu'une estimation a été déclarée pour l'axe.
- La couverture affichée est une approximation déclarative, explicitement non vérifiée — l'UI doit la présenter comme telle (« estimation déclarative », avec la date de dernière mise à jour) pour rester fidèle à l'esprit de prudence méthodologique de RG-087, pas seulement à sa lettre.
- Aucune estimation n'est déclarée par défaut : tant qu'un agent Bureau n'a pas configuré l'axe, la couverture reste absente plutôt que fausse.
- Si une source de marché réel fiable apparaît un jour (interopérabilité régionale évoquée pour décembre 2026, §14.1, à confirmer), `EstimationMarcheAxePort` peut être ré-implémenté pour l'ingérer automatiquement sans changer `ObservatoireService` ni l'API exposée.
- La gouvernance de la donnée (qui a le droit de déclarer/modifier une estimation, à quelle fréquence la revoir) reste une question opérationnelle pour le Bureau, pas technique — non traitée ici au-delà de la traçabilité (`definieParActeurId`/`definieLe`).
