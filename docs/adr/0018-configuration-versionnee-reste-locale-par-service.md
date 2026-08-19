# ADR 0018 — Le catalogue de configuration versionnée (EF-ADM-06) reste porté par `service-adm` seul, sans y faire migrer les paramètres métier des autres services

**Statut** : Accepté (décision d'architecture, 2026-08-18)

## Contexte

EF-ADM-06 (M) : « Le système doit offrir une console de configuration versionnée et auditée pour l'ensemble des paramètres métier. » Le mécanisme (`ConfigurationPort`/`ConfigurationService`/`ConfigurationVersionnee`, `service-adm`) existe déjà depuis EF-ADM-03, mais n'a jamais servi qu'à une seule clé réelle : `grille-decision`. La console web (`admin/configurations`) n'était qu'une recherche par clé libre, sans catalogue.

En parallèle, deux paramètres métier réels vivent aujourd'hui en `@Value` (Spring, figés au démarrage, non versionnés, non audités) hors de `service-adm` :
- `fretcorridor.bur.seuil-agregation` (`service-bur`, RG-085 — seuil d'agrégation minimale de l'observatoire) — décision initiale prise à l'ADR 0016/EF-BUR-05.
- `fretcorridor.pay.ordonnanceur-reversement.delai-contestation-heures` (`service-pay`, RG-079 — délai de contestation avant reversement automatique).

Une lecture littérale d'EF-ADM-06 (« l'ensemble des paramètres métier ») invite à faire migrer ces deux paramètres dans le mécanisme `ConfigurationPort` de `service-adm`, pour qu'ils apparaissent dans le même catalogue.

## Décision

**Ne pas migrer** ces deux paramètres dans ce sprint. Le mécanisme `ConfigurationPort` reste local à `service-adm`, qui n'a et n'a toujours eu qu'une dépendance vers lui-même pour ce besoin.

Migrer `seuil-agregation` ou `delai-contestation-heures` exigerait que `service-bur`/`service-pay` appellent `service-adm` en synchrone — sur un chemin qui aujourd'hui ne dépend d'aucun autre service :
- `seuil-agregation` est lu à chaque calcul d'indicateur de l'observatoire (chemin de lecture fréquent, potentiellement chaud).
- `delai-contestation-heures` est lu à chaque exécution de l'ordonnanceur de reversement automatique (toutes les 15 min).

Introduire cette dépendance romprait la même discipline qui a motivé l'ADR 0016 côté `service-bur`, et irait à l'encontre de l'esprit d'ENF-DIS-04 (l'indisponibilité d'un service ne doit jamais bloquer le fonctionnement autonome d'un autre) déjà observé ailleurs dans le gateway (EF-INT-05, mocks `Mock*Adapter`).

À la place, ce sprint construit un **catalogue** au sens propre : `ConfigurationPort.toutesLesVersionsCourantes()` (nouveau), exposé par `GET /api/v1/configurations` (`service-adm`) puis `GET /api/v1/admin/configurations` (gateway), consommé par une vraie page catalogue côté web (liste des clés déjà configurées, plus besoin de connaître leur nom à l'avance). Le catalogue reste honnête : il ne liste que ce qui est réellement géré par `service-adm` (aujourd'hui : `grille-decision`), pas une façade sur des `@Value` distants.

## Conséquences

- EF-ADM-06 est livré pour le périmètre que `service-adm` gouverne réellement — pas la totalité littérale « de l'ensemble des paramètres métier » de la plateforme.
- `seuil-agregation` (`service-bur`) et `delai-contestation-heures` (`service-pay`) restent des `@Value`, non versionnés, non audités, modifiables seulement par redéploiement — un vrai écart au texte du CDC, documenté ici plutôt que masqué.
- Si ce texte doit être satisfait à la lettre, la voie cohérente n'est pas de centraliser dans `service-adm`, mais que **chaque service porte son propre mécanisme de configuration versionnée local** (même forme que `ConfigurationPort`, dupliquée, pas partagée en synchrone) — une décision d'architecture à part entière, à trancher explicitement plutôt qu'en effet de bord d'un sprint catalogue.
- Rien n'empêche d'ajouter dans ce catalogue, plus tard, tout paramètre que `service-adm` viendrait à posséder réellement (ex. si `delai-contestation-heures` était un jour un paramètre décidé par un agent Admin plutôt que par configuration de déploiement de `service-pay`).
