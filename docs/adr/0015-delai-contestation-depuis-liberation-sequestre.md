# ADR 0015 — Le délai de contestation (EF-PAY-08) part de la libération du séquestre, pas d'une date de livraison distincte

**Statut** : Accepté (décision produit, 2026-08-12)

## Contexte

Le CDC (§7.6, UC-PAY-02) distingue clairement deux instants : l'encaissement (UC-PAY-01, avant exécution) et la livraison prouvée (UC-PAY-02, précondition du reversement). Le délai de contestation part de la livraison, pas de l'encaissement — « la mission est à l'état "livrée" ; les preuves sont enregistrées ; le délai de contestation est écoulé ».

`service-pay` ne reçoit aujourd'hui aucun signal de livraison distinct. L'endpoint `POST /missions/{missionId}/cloture` (Sprint 8, Phase 1) conflate encaissement et fin de mission en un seul appel — son propre commentaire de code le documente comme un point d'entrée temporaire, en attendant un événement Kafka `MissionCloturee` que Mobile/EXE n'a pas encore construit (bloqué, hors du périmètre Web — cf. `docs/CONTEXTE_SESSION_UI.md` §3).

Construire un vrai concept de « date de livraison » distinct aurait exigé soit un nouvel endpoint sans appelant connu aujourd'hui, soit une dépendance vers un événement qui n'existe pas encore côté Mobile — de la portée pour un mécanisme qui resterait inerte.

## Décision

L'ordonnanceur `ReversementAutomatiqueService` (EF-PAY-08) calcule le délai de contestation à partir de `Sequestre.libereLe()` — horodatage déjà existant, posé à la clôture (`SequestreService.liberer`). C'est une simplification assumée : en Phase 1/2, « clôture » et « livraison » sont un seul et même instant côté `service-pay`.

Conséquence directe : `Sequestre` porte désormais `tenantId`/`transporteurId` (connus seulement à la clôture, `null` tant que `DECLENCHE`) — nécessaires pour que l'ordonnanceur puisse construire un reversement sans intervention humaine, alors qu'aujourd'hui ces informations ne sont fournies qu'au moment de l'appel REST explicite `POST .../reversement`.

## Conséquences

- Le vrai premier ordonnanceur du périmètre (`@Scheduled`, contrairement à `EscaladeService` côté service-adm resté « à la demande ») peut être livré sans attendre Mobile/EXE.
- **Écart assumé vis-à-vis du CDC** : si un jour un chargeur ou un litige survient *après* la clôture mais concerne un fait antérieur à une vraie livraison distincte, le délai aura déjà commencé à courir depuis la clôture — pas depuis la livraison réelle. Le mécanisme de suspension sur litige actif (EF-PAY-08, ADR implicite du Sprint 4) reste le filet de sécurité : tant qu'un dossier LITIGE est ouvert, aucun reversement n'est émis, quel que soit le délai écoulé.
- Le jour où EXE publie un événement de livraison exploitable, il suffira de faire porter `libereLe`-équivalent par ce nouvel événement plutôt que par `Sequestre.liberer` — le reste de l'ordonnanceur (filtre, garde litige, calcul du solde) n'a pas besoin de changer.
- Délai configurable (`fretcorridor.pay.ordonnanceur-reversement.delai-contestation-heures`, 48h par défaut en développement), jamais codé en dur — RG-079 exige qu'il soit « plafonné, publié, et suivi comme indicateur ».
