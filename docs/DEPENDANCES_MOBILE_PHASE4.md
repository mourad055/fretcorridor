# Dépendances côté Mobile — Phase 4 (`service-pay`/`service-adm`)

**Date** : 2026-08-18
**Auteur** : Mourad (volet Web), avec assistance Claude Code
**Contexte** : la Phase 4 (`feature/web-socle`) ferme le dernier chantier obligatoire de mon périmètre (EF-PAY, EF-ADM, EF-BUR — cf. `docs/CONTEXTE_SESSION_UI.md`). Ce document ne couvre que les points où l'avancement dépend d'une action côté Mobile (apps Chauffeur/Transporteur/Client, ou services `service-exe`/`service-ida`/`service-cap`), pour que rien ne reste implicite entre les deux équipes. Même format que `docs/ROADMAP_INTEGRATION_gateway.md`, sens inverse : là-bas c'est le gateway qui appelle Mobile, ici c'est Mobile qui doit appeler/publier vers `service-pay`.

> **⚠️ Numérotation.** "Phase 4" dans ce titre est ma numérotation informelle
> pour le reliquat CDC du périmètre Web — **pas** la Phase 4 officielle du
> projet (§5.4 du Plan d'Exécution, exclusivement Moteur : `MAT/OPT V3` +
> `EF-GEO-05`). Voir `docs/ROADMAP_OFFICIELLE_par_porteur.md` pour la
> numérotation officielle partagée par les 3 porteurs (Mobile y est
> présent en Phases 1-3, jamais en Phase 4).

---

## Constat général

`service-pay` expose aujourd'hui les endpoints financiers (`prise-en-charge`, `cloture`, `confirmation-livraison`, `reversement`, `garantie`, `paiement-especes`, `écritures`, `rapport`, `réconciliation`), mais **rien ne les appelle en dehors des tests**. La Javadoc de `PaiementController` le dit explicitement depuis le début de la Phase 2 : ce sont des "points d'entrée temporaires", en attendant que Mobile (`service-exe`) publie les événements réels de cycle de vie mission. Ce document rend ce constat concret et actionnable.

## Item A — Déclencher la clôture financière d'une mission (bloquant, RG-078/EF-PAY-08)

| | |
|---|---|
| **Statut** | 🟢 Résolu (2026-08-18) |
| **Constat initial** | `POST /api/v1/pay/missions/{id}/cloture` (encaissement + libération du séquestre) et `POST /api/v1/pay/missions/{id}/confirmation-livraison` (libération seule, sans encaissement — RG-078, commit `382b89d`) existaient côté `service-pay`, mais **aucun appelant réel**. |
| **Résolution** | Option (2), Kafka, retenue par Mobile — `service-exe` publie `MissionLivree` sur le topic `mission-livree` à la confirmation de l'étape LIVRAISON (commit `0c4fced`, PR #79). Contrat : `shared-contracts/asyncapi/events/mission-livree.yaml` (`missionId`, `tenantId`, `transporteurId` nullable, `preuveLivraisonReference`, `dateLivraison`, `eventId`). Consommateur construit côté `service-pay` : `MissionLivreeListener` appelle `SequestreService.liberer(...)`, rejeu Kafka (`SequestreInvalideException`) avalé et journalisé plutôt que de bloquer le groupe de consommateurs. `POST /confirmation-livraison` reste disponible (usage manuel/ops) mais n'a plus d'appelant réel dans le flux nominal. |
| **preuveLivraisonReference** | Mobile a choisi l'id de l'`EtapeMission` LIVRAISON elle-même, faute d'un vrai système de preuve (photo/signature) côté `service-exe` à ce jour — cohérent avec RG-078 (`service-pay` ne valide que la présence de la référence, jamais sa nature). |
| **Reste en attente** | `POST /cloture` (encaissement inclus) n'a toujours aucun appelant réel — aucun contrat `MissionCloturee`-équivalent ne couvre encore l'encaissement, seulement la libération. Pas bloquant pour EF-PAY-08 (reversement) puisque `confirmation-livraison` seule suffit pour le terme contractuel (EF-PAY-06) ; reste pertinent pour le cas standard encaissement réel. |
| **Porteur** | Mobile (fait) — consommateur : Web (fait) |
| **Priorité** | Haute |

## Item B — Mode de règlement affiché / moyen de paiement choisi (S14, discuté le 2026-08-18)

| | |
|---|---|
| **Statut** | 🔴 Bloqué — pas commencé |
| **Constat** | `ModePaiement` (`MONNAIE_ELECTRONIQUE`/`VIREMENT`/terme contractuel) n'est aujourd'hui enregistré qu'*a posteriori*, au moment de l'encaissement (`cloture`/webhook prestataire) — rien ne permet à un client de le choisir en amont, rien ne l'expose pour affichage Chauffeur avant encaissement. |
| **Ce qu'il faut construire** | Un nouveau concept domaine ("moyen choisi/prévu" vs "moyen effectivement encaissé"), à intégrer proprement avec ENF-FIN-01/02 et RG-078. |
| **Porteur** | Moi (conception + endpoint côté `service-pay`), après le sprint Phase 4 en cours (EF-PAY-02/09 puis EF-ADM-06) — puis Mobile pour l'intégration UI Chauffeur/Client (S14). |
| **Priorité** | Dépend du calendrier S14 côté Mobile, pas de ma Phase 4 — documenté ici pour visibilité partagée, sera reformulé en item concret une fois conçu. |

## Hors périmètre Mobile (pour éviter toute confusion)

La réconciliation quotidienne (EF-PAY-02/09, sprint suivant de ma Phase 4) compare le grand livre local au relevé d'un **prestataire de paiement externe agréé** (`PrestatairePaiementPort`, actuellement `MockPrestatairePaiementAdapter`) — ce n'est ni un service Mobile ni un service Moteur, c'est une intégration tierce hors du périmètre des 3 porteurs actuels. Mobile n'a rien à construire pour cet item.

## Rappel des items déjà suivis ailleurs

Les autres dépendances Mobile connues (adaptateurs gateway encore bloqués : `service-exe` liste tenant-scopée, `service-ida` décisions KYC admin, `service-cap` endpoint de liste par transporteur) sont déjà suivies dans `docs/ROADMAP_INTEGRATION_gateway.md` §Phase 2 — pas dupliquées ici.
