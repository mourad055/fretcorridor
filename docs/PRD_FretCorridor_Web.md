# PRD — FretCorridor V4.0 — Portail Web & Services associés (gateway, PAY, BUR, ADM)

**Statut** : Prêt pour exécution — Phase 1 (MVP)
**Référence CDC** : FSE-CDC-FRETCORRIDOR-2026-004 (v4.0)
**Documents sources** (à lire intégralement avant tout développement, dossier `/docs`) :
- `CDC_FretCorridor_v4_FSE2026004.pdf` — cahier des charges v4.0 (110 pages, source d'autorité)
- `FretCorridor_Plan_Execution_V4.docx` — plan d'exécution technique (architecture, sprints, dépendances)
- `FretCorridor_Feuille_de_Route_V4.docx` — feuille de route d'apprentissage et écrans par volet

**Porteur de ce périmètre** : Personne 2 — Web (application unique Angular, 3 rôles) + microservices `gateway`, `service-pay`, `service-bur`, `service-adm`.

---

## 0. Rappel du principe d'équipe (ne pas dévier)

Le dépôt est un **monorepo unique** partagé par 3 personnes. Ce PRD ne couvre que le périmètre de la Personne 2. Les autres périmètres (Mobile : 2 apps Flutter + `service-ida`/`service-cap`/`service-mkt`/`service-flt`/`service-exe`/`service-not` ; Moteur : `service-mat`/`service-opt`/`service-trk`/`service-geo`) sont **scaffoldés en placeholders** dans le même dépôt pour que les coéquipiers puissent y déposer leur travail dans un dossier déjà structuré, mais **ne doivent pas être implémentés** dans le cadre de ce PRD. Ne jamais écrire de logique métier dans les dossiers hors périmètre.

L'invariant produit non négociable, valable pour tout ce périmètre (en particulier `service-pay`) :
> FretCorridor n'est **jamais** dépositaire des fonds. `service-pay` écrit uniquement dans un grand livre miroir ; le séquestre réel est détenu par un prestataire de paiement agréé externe. Tout chemin de code qui créerait une écriture de trésorerie sur un compte FretCorridor est un défaut bloquant (ENF-FIN-01/02).

---

## 1. Périmètre du produit

### 1.1 Dans le périmètre (à implémenter)
- **`web/`** — une application Angular unique (PWA), à **trois rôles** distincts affichés selon le rôle de l'acteur connecté : **Bureau de fret**, **Transporteur**, **Administration**. Pas trois applications séparées : un seul shell, des feature modules chargés selon le rôle, garde de routes RBAC.
- **`backend/gateway`** — passerelle API : authentification JWT, résolution multi-tenant, RBAC, rate limiting, routage vers tous les microservices (y compris ceux d'autres porteurs, via leurs contrats publiés dans `shared-contracts/`).
- **`backend/service-pay`** — orchestration du paiement : interface avec le prestataire de paiement agréé, grand livre miroir, séquestre logique, réconciliation, contrôles ENF-FIN-01/02/03.
- **`backend/service-bur`** — interfaces bureaux de fret : rattachement chargeurs/transporteurs, tableaux de bord de supervision, reporting agrégé (version minimale en Phase 1, enrichie en Phase 3 — cf. §7).
- **`backend/service-adm`** — back-office : configuration versionnée, gestion des tenants, supervision globale, rôles administrateurs, journal d'audit.

### 1.2 Hors périmètre (scaffold uniquement, ne pas implémenter)
- `mobile/app_chauffeur_transporteur/`, `mobile/app_client/` (Personne 1)
- `backend/service-ida`, `backend/service-cap`, `backend/service-mkt`, `backend/service-flt`, `backend/service-exe`, `backend/service-not` (Personne 1)
- `backend/service-mat`, `backend/service-opt`, `backend/service-trk`, `backend/service-geo` (Personne 3)

Pour ces dossiers : créer uniquement l'arborescence + un `README.md` de 3-4 lignes indiquant le porteur prévu et le module CDC concerné. Zéro dépendance applicative dessus au-delà de ce que `shared-contracts/` documente en interface (DTO/événements).

### 1.3 Ce que ce PRD ne couvre PAS dans cette itération
- Le contenu détaillé des écrans Mobile (traité par la Personne 1).
- Le moteur de matching et son algorithmique (traité par la Personne 3).
- Les Phases 2, 3, 4 du plan d'exécution (groupage, oracle 3D, observatoire complet, régionalisation) — hors roadmap ci-dessous, qui s'arrête à la fin de la Phase 1 (Sprint 10).
- La sélection effective du prestataire de paiement agréé (verrou V2 du CDC, en cours côté direction) : `service-pay` doit être développé contre une **interface abstraite** (port hexagonal) avec un adaptateur *sandbox/mock* en attendant l'intégration réelle.

---

## 2. Utilisateurs et rôles (CDC §5.1)

| Rôle | Nature | Ce qu'il fait dans le portail web | Périmètre de données |
|---|---|---|---|
| **Bureau de fret** | Institution, tenant | Supervise les flux de son territoire, consulte l'observatoire, exporte des rapports | Son tenant uniquement, données agrégées pour les tiers |
| **Transporteur** | Personne morale ou physique | Consulte sa flotte, ses missions, son grand livre (ses écritures), configure ses connecteurs | Sa flotte, ses missions, ses écritures uniquement |
| **Administrateur** | Interne Flysoft | Modère, arbitre les litiges, configure la plateforme, consulte le journal d'audit | Transverse à tous les tenants, avec traçabilité de chaque accès |

Règle d'habilitation non négociable (CDC RG-002) : tout accès est évalué sur 3 questions — le rôle est-il détenu ? le rôle porte-t-il ce droit ? la ressource est-elle dans le périmètre de ce rôle pour ce demandeur ? Une réponse négative à l'une des trois **interdit** l'accès. Ne jamais implémenter de contrôle d'accès qui court-circuite la 3ᵉ question (c'est la source de fuite la plus fréquente selon le CDC).

Deux lignes de la matrice des droits du CDC (§5.3) à respecter strictement dans ce périmètre :
- Le **Bureau** n'a jamais accès à la position d'un véhicule hors mission (pas de surveillance généralisée).
- L'**observatoire de marché** est anonymisé pour les transporteurs : ils voient les tendances, jamais les positions ou prix nominatifs d'un concurrent.

---

## 3. Objectifs mesurables de ce périmètre pour sortir de la Phase 1

Repris et adaptés du CDC §16 et §18.1, restreints à ce que ce périmètre doit démontrer :

1. Un administrateur peut se connecter, faire remonter et clôturer un dossier de modération de bout en bout (file de travail, dossier consolidé, décision versionnée, journal d'audit) — **critère d'acceptation MVP #8 dérivé**.
2. Un bureau de fret peut se connecter, voir la supervision cartographique et tabulaire des missions de son territoire, et exporter un flux filtré.
3. Un transporteur peut se connecter, voir sa flotte, ses missions en cours, et son grand livre de paiement (ses écritures uniquement).
4. Le contrôle automatisé **ENF-FIN-01** (aucun chemin de code ne crée d'écriture de trésorerie sur un compte FretCorridor) est en place et bloquant en CI **dès le premier commit touchant `service-pay`**, pas ajouté a posteriori.
5. La réconciliation quotidienne (`service-pay`) lève une alerte bloquante sur tout écart et l'isole (ENF-FIN-03) — testée avec un scénario d'écart injecté.
6. La suite de tests d'isolation multi-tenant (ENF-SEC-04) s'exécute à chaque livraison sur `gateway`, `service-pay`, `service-bur`, `service-adm` et échoue la build si un rôle accède à une ressource hors périmètre.

---

## 4. Architecture cible de ce périmètre

### 4.1 Position dans l'architecture globale
Voir `docs/FretCorridor_Plan_Execution_V4.docx` §4 pour le schéma complet. Rappel du sous-ensemble concerné :

```
Portail Web (Angular, PWA, 1 app / 3 rôles)
        │
        ▼
   API Gateway  (JWT, RBAC, multi-tenant, rate limiting)
        │
        ├──► service-pay   (grand livre miroir, séquestre logique)
        ├──► service-bur   (supervision, observatoire — version minimale en Phase 1)
        └──► service-adm   (config versionnée, tenants, audit)

   [lecture seule, via gateway, contrats publiés par les autres porteurs]
        ├──► service-ida (Mobile)   — validation KYC, indice de conformité
        ├──► service-mkt (Mobile)   — capacités/demandes publiées
        ├──► service-exe (Mobile)   — chronologie de mission
        ├──► service-geo (Moteur)   — axes, hubs, zonage (cartographie)
        ├──► service-trk (Moteur)   — position/ETA (carte de suivi)
        └──► service-opt (Moteur)   — missions appariées (vue bureau)
```

### 4.2 Principes d'architecture non négociables (hérités du CDC §12.1 et adaptés Spring Boot)
- Architecture hexagonale dans chaque service Spring Boot : `domain/` ne dépend d'aucune infrastructure (`infrastructure/jpa`, `infrastructure/rest`, `infrastructure/messaging`). C'est ce qui rend ENF-FIN-01 testable automatiquement par analyse du domaine, pas par grep.
- Communication **asynchrone par défaut** (Kafka) entre `service-pay`/`service-bur`/`service-adm` et tout service porté par une autre personne. Aucun appel synchrone direct vers `service-ida`, `service-mkt`, `service-mat`, `service-opt`, etc. — seule la consultation initiée par le **client web via la gateway** peut être synchrone (REST classique).
- Multi-tenant par construction dès le socle : chaque requête est résolue à un tenant, chaque table métier porte une colonne/schema d'isolation, vérifiée par une suite de tests d'intrusion automatisée (ENF-SEC-04).
- Configuration versionnée en base (jamais en fichier) pour tout barème, seuil ou règle métier (anti-patron explicite CDC §12.4).
- Angular : Signals pour la gestion d'état (pas NgRx), architecture par feature modules chargés selon le rôle (`features/bureau`, `features/transporteur`, `features/admin`), PWA avec service worker.

### 4.3 Modèle de données de ce périmètre (extrait du CDC §13)

| Entité | Service propriétaire | Description | Relations |
|---|---|---|---|
| Écriture miroir | service-pay | Grand livre en partie double, référence prestataire, état | n-1 Mission |
| Séquestre | service-pay | État logique reflétant le cantonnement chez le prestataire | 1-1 Mission |
| Réconciliation | service-pay | Rapprochement périodique avec le prestataire | n-1 Écriture miroir |
| Tenant | service-adm | Espace isolé : bureau institutionnel ou périmètre commercial | 1-n Acteur, 1-n Axe |
| Configuration versionnée | service-adm | Paramètre versionné : clé, valeur, périmètre, auteur, date | n-1 Tenant |
| JournalAudit | service-adm | Trace inviolable des actions sensibles, append-only | réf. Tenant, Acteur, ressource |
| Bureau de fret | service-bur | Institution supervisant un territoire | 1-1 Tenant |
| Rattachement | service-bur | Lien chargeur/transporteur ↔ bureau | n-1 Bureau, n-1 Acteur |
| Reporting bureau | service-bur | Agrégats par bureau/corridor (seuil d'agrégation minimal) | n-1 Bureau |

**Aucune de ces entités ne doit contenir de champ représentant un solde réel ou un ordre de virement.** `service-pay` ne modélise que des écritures miroir et des états logiques — l'exécution financière réelle est entièrement déléguée au prestataire externe.

---

## 5. Exigences fonctionnelles détaillées (Phase 1)

### 5.1 Authentification & RBAC (gateway + shell Angular)
- FE-WEB-01 (M) — Écran de connexion unique (téléphone + code), aucune indication du rôle avant authentification réussie.
- FE-WEB-02 (M) — Après connexion, résolution du rôle et du tenant depuis le token JWT ; redirection vers le feature module correspondant.
- FE-WEB-03 (M) — Garde de routes RBAC : une route du module Admin renvoie une 403 propre (pas une erreur technique) si le rôle connecté est Transporteur ou Bureau.
- FE-WEB-04 (M) — Aucune route ne doit être atteignable en modifiant l'URL manuellement sans passer la garde de rôle (test E2E obligatoire, cf. §8).

### 5.2 Rôle Bureau de fret
- FE-BUR-01 (M) — Supervision cartographique et tabulaire des missions du territoire du tenant (consomme `service-geo`/`service-trk`/`service-opt` en lecture via gateway).
- FE-BUR-02 (M) — Filtrage, détail et export (CSV a minima en Phase 1) des flux supervisés.
- FE-BUR-03 (S, version minimale Phase 1) — Aperçu observatoire : volumes et missions par axe. La version complète (seuil d'agrégation EF-BUR-04, couverture d'échantillon EF-BUR-05) est planifiée Phase 3 et **n'est pas requise** pour la sortie de Phase 1, mais l'API `service-bur` doit déjà être conçue pour ne jamais restituer une statistique sur un effectif inférieur au seuil configuré, dès le premier endpoint (pour ne pas avoir à retrofitter la sécurité des données plus tard).
- FE-BUR-04 (M) — Toute consultation de donnée individuelle par un Bureau est journalisée nominativement (ENF-SEC-02) — déclenché même en Phase 1 minimale.

### 5.3 Rôle Transporteur
- FE-TRP-01 (M) — Vue flotte (lecture, données issues de `service-flt`, Mobile) : véhicules, chauffeurs, disponibilités.
- FE-TRP-02 (M) — Vue missions en cours / à venir (lecture, `service-exe`, Mobile).
- FE-TRP-03 (M) — Grand livre de paiement : uniquement ses propres écritures (`service-pay`, périmètre strict par acteur).
- FE-TRP-04 (S) — Écran de configuration de connecteur flotte tiers (l'intégration réelle est portée par `service-flt`, Mobile ; le web n'expose que l'écran de configuration, en amont de la Phase 2).

### 5.4 Rôle Administration (back-office)
- FE-ADM-01 (M) — File de travail priorisée (modération, incidents, litiges) — `service-adm`.
- FE-ADM-02 (M) — Dossier consolidé : mission + parties + chronologie + preuves + écritures (agrège en lecture `service-exe`, `service-pay`, `service-ida`).
- FE-ADM-03 (M) — Console de configuration versionnée et auditée des paramètres métier.
- FE-ADM-04 (M) — Gestion des tenants (bureaux de fret, entités multi-tenant).
- FE-ADM-05 (M) — Journal d'audit consultable et exportable, en lecture seule, append-only côté backend.
- FE-ADM-06 (M) — Dashboard de validation KYC (lecture/action sur `service-ida`, Mobile, via gateway) — Sprint 2.

### 5.5 Paiement (`service-pay`)
- FE-PAY-01 (M) — Écriture en grand livre miroir de chaque mouvement déclenché par la clôture de mission (`MissionCloturee`, consommé depuis le bus).
- FE-PAY-02 (M) — Séquestre logique déclenché à la prise en charge, libéré à la clôture — jamais l'inverse (contrôle ENF-FIN-02 bloquant en CI).
- FE-PAY-03 (M) — Contrôle automatisé ENF-FIN-01 : aucun chemin de code ne peut créer une écriture de trésorerie sur un compte FretCorridor. Ce contrôle est un test automatisé exécuté en CI, pas une revue manuelle.
- FE-PAY-04 (M) — Réconciliation périodique avec l'adaptateur du prestataire de paiement (mock en Phase 1) ; tout écart lève une alerte bloquante et isole l'écriture concernée (ENF-FIN-03).
- FE-PAY-05 (M) — Vérification cryptographique et traitement idempotent de toute notification entrante (webhook) du prestataire (EF-PAY-05 du CDC), même sur l'adaptateur mock.

---

## 6. Exigences non fonctionnelles applicables à ce périmètre

| Réf. CDC | Exigence | Application dans ce périmètre |
|---|---|---|
| ENF-PRF-01 | Interactions critiques < 2s au 95ᵉ centile sur 3G | Toutes les pages du portail web |
| ENF-SEC-01 | Chiffrement transit/repos, auth forte back-office, moindre privilège | gateway, tous les services de ce périmètre |
| ENF-SEC-02 | Journal d'audit append-only, inviolable | service-adm (JournalAudit), déclenché par toute consultation Bureau/Admin |
| ENF-SEC-04 | Suite de tests d'isolation à chaque livraison, tentant des accès transverses | Obligatoire en CI sur gateway + les 3 services, échec bloquant |
| ENF-SEC-05 | Secrets centralisés, jamais en dur | Tous les services |
| ENF-FIN-01/02/03 | Aucune détention de fonds, aucun reversement sans encaissement, réconciliation bloquante | service-pay — cœur du périmètre |
| ENF-MUL-01 | Isolation stricte tenant/pays, vérifiée par tests automatisés | gateway, service-bur, service-adm |
| ENF-OBS-01/02/03 | Traçage distribué, alerting SLO, IaC, CI/CD | Tous les services de ce périmètre |
| ENF-I18N-01 | FR/EN à parité, y compris messages d'erreur | Portail web complet |
| ENF-A11Y-01 | Contraste AA, cibles tactiles ≥48pt, navigation clavier | Portail web complet |

---

## 7. Contrats d'API et d'événements (à publier dans `shared-contracts/`)

Ce périmètre **consomme** des événements publiés par d'autres porteurs et **publie** les siens. Toute évolution de contrat est versionnée (SemVer sur le nom du topic ou le champ `version` du payload) — ne jamais casser un contrat consommé par un autre porteur sans un cycle de dépréciation documenté dans `docs/`.

### 7.1 Événements consommés par ce périmètre
| Événement | Émetteur | Consommé par | Usage |
|---|---|---|---|
| `MissionCloturee` | service-exe (Mobile) | service-pay | Déclenche l'écriture en grand livre miroir |
| `PropositionEmise` | service-opt (Moteur) | service-bur (lecture) | Alimente la vue « missions appariées » du Bureau |
| `CapaciteDeclaree` / `DemandePubliee` | service-cap / service-mkt (Mobile) | service-bur (lecture, agrégats) | Observatoire minimal Phase 1 |
| Notifications de webhook prestataire de paiement | Externe | service-pay | Réconciliation, vérifiée cryptographiquement et idempotente |

### 7.2 Événements publiés par ce périmètre
| Événement | Émis par | Consommateurs prévus | Usage |
|---|---|---|---|
| `EcritureMiroirCreee` | service-pay | (observabilité, futurs modules) | Traçabilité financière |
| `TenantConfigure` | service-adm | gateway (cache de résolution tenant) | Invalidation de cache multi-tenant |
| `DecisionModerationPrise` | service-adm | (à documenter en Phase 2) | Traçabilité litiges |

### 7.3 API REST exposées via la gateway
Documenter chaque endpoint en OpenAPI 3 dans `shared-contracts/openapi/` dès sa création (pas en fin de sprint). Convention : `/api/v1/{module}/...`, erreurs au format RFC 7807, header `X-Idempotency-Key` obligatoire sur toute mutation.

---

## 8. Stratégie de tests (obligatoire à chaque sprint, aucune exception)

Aucun sprint n'est considéré terminé si les tests suivants ne sont pas verts en CI :

### 8.1 Backend (Spring Boot — JUnit5, Mockito, Testcontainers)
- **Tests unitaires** sur le domaine (`domain/`) : ≥ 80% de couverture sur la logique métier, sans mock de framework — le domaine est isolé donc testable en pur Java.
- **Tests d'intégration** avec Testcontainers (PostgreSQL réel, Kafka réel si le flux est testé) pour chaque service : persistance, publication/consommation d'événements.
- **Tests de contrat** : chaque endpoint REST validé contre son schéma OpenAPI ; chaque événement publié validé contre son schéma AsyncAPI.
- **Test spécifique ENF-FIN-01/02** sur `service-pay` : test automatisé qui échoue si un chemin de code permet une écriture de trésorerie FretCorridor, ou un reversement sans encaissement préalable. Ce test doit exister **avant** la première fonctionnalité de paiement, pas après.
- **Test spécifique ENF-SEC-04** : suite d'isolation multi-tenant qui tente des accès transverses depuis chaque rôle (Bureau, Transporteur, Admin) sur chaque service de ce périmètre.

### 8.2 Frontend (Angular — Jest + Angular Testing Library)
- **Tests unitaires** sur les composants et services (garde de routes, résolution de rôle, formulaires) : ≥ 80% de couverture sur `core/` et `shared/`.
- **Tests d'intégration** sur chaque feature module (bureau/transporteur/admin) avec mock des appels HTTP (HttpTestingController).
- **Tests de contrat frontend** : les DTO consommés sont typés depuis les schémas OpenAPI générés (pas de `any`).

### 8.3 End-to-end (Playwright)
- Un scénario E2E critique par rôle et par sprint livrant un écran (ex. Sprint 1 : connexion + redirection par rôle ; Sprint 10 : parcours modération complet).
- Un scénario E2E dédié à la garde de routes : tentative d'accès direct par URL à une route hors rôle → 403 propre, jamais de fuite de données dans la réponse réseau (vérifié via interception réseau Playwright).

### 8.4 Definition of Done (s'applique à chaque sprint de la roadmap §9)
Un sprint n'est **done** que si, dans cet ordre : (1) le code compile et lint passe sans warning nouveau, (2) tous les tests ci-dessus sont verts en local, (3) la CI GitHub Actions est verte sur la pull request, (4) le contrat d'API/événement concerné est publié dans `shared-contracts/`, (5) un commit (ou une série de commits atomiques) est poussé avec un message conventionnel, (6) ce PRD ou le CHANGELOG est mis à jour si le périmètre a évolué.

---

## 9. Roadmap Phase 1 (MVP) — Sprints 1 à 10, périmètre de ce PRD uniquement

Chaque sprint = 2 semaines indicatives, avancement par jalon plutôt que par date fixe (cf. Plan d'exécution §8). Colonne « Sortie » = critère vérifiable de fin de sprint.

| Sprint | Contenu (ce périmètre uniquement) | Services/écrans livrés | Sortie (Definition of Done spécifique) |
|---|---|---|---|
| **S1** | Socle repo + authentification | `gateway` (JWT, RBAC de base) ; shell Angular + écran de connexion + garde de routes par rôle | Connexion réussie redirige vers le bon feature module ; tentative d'accès à une route hors rôle → 403 ; CI verte (build+tests+lint) sur gateway et web |
| **S2** | KYC — vue Admin | Dashboard rôle Admin de validation KYC (lecture/action sur `service-ida` via gateway, mock de service-ida si pas encore disponible côté Mobile) | Un admin voit une liste de KYC en attente et peut la faire passer à un état « validé »/« rejeté », action journalisée |
| **S3** | Cartographie — vue Bureau | Carte des axes (lecture `service-geo` via gateway, mock si pas encore disponible côté Moteur) — début `service-bur` (squelette hexagonal) | Un Bureau voit une carte des axes de son tenant ; isolation tenant testée (Bureau A ne voit pas les axes du tenant B) |
| **S4** | Vue Transporteur — capacités | Vue capacités publiées, lecture seule (`service-mkt`/`service-cap` via gateway, mock) | Un Transporteur voit ses capacités déclarées (mockées), aucune capacité d'un autre transporteur visible |
| **S5** | Vue Bureau — missions appariées | Vue missions appariées (lecture `service-opt` via gateway, mock) + premier endpoint `service-bur` réel (agrégat simple) | Un Bureau voit les missions de son territoire ; `service-bur` a son premier test d'intégration Testcontainers vert |
| **S6** | Suivi temps réel — vue Bureau | Carte de suivi temps réel (lecture `service-trk` via gateway, mock) | Positions affichées avec horodatage et âge visible (jamais une position sans âge, règle CDC RG-043) |
| **S7** | Chronologie de mission | Chronologie de mission — vue Bureau (supervision) + vue Transporteur (ses missions) | Un Transporteur ne voit que ses missions ; un Bureau voit celles de son territoire ; test d'isolation vert |
| **S8** | **Paiement — cœur du périmètre** | `service-pay` complet Phase 1 : grand livre miroir, séquestre logique, adaptateur mock du prestataire ; écran solde/historique (Transporteur) + rapport financier (Bureau/Admin, lecture) | Test ENF-FIN-01/02 vert et bloquant en CI ; réconciliation avec écart injecté lève une alerte ; aucune écriture réelle de fonds possible (prouvé par test) |
| **S9** | Notifications — centre web | Centre de notifications côté web (canal email, lecture `service-not` via gateway, mock) | Un Bureau voit ses notifications email dans le centre web |
| **S10** | **Back-office complet** | `service-adm` complet Phase 1 : file de travail, dossier consolidé, configuration versionnée, gestion tenants, journal d'audit ; portail Administration complet | Parcours E2E complet : un admin traite un dossier de bout en bout, décision journalisée, escalade automatique testée sur dépassement de délai |

**Critère de sortie de Phase 1 pour ce périmètre** : les 10 sprints ci-dessus sont « done » au sens du §8.4, la CI est verte sur `main`, et les 3 scénarios E2E rôle (Bureau, Transporteur, Admin) passent de bout en bout sur un environnement Docker Compose local.

---

## 10. Règles d'exécution pour l'agent (non négociables)

1. **Lire avant d'écrire** : lire intégralement le CDC v4 (`docs/CDC_FretCorridor_v4_FSE2026004.pdf`), le Plan d'exécution V4 et la Feuille de route V4 avant la moindre ligne de code. En cas de contradiction entre ce PRD et le CDC, le CDC prévaut — signaler la contradiction avant de trancher.
2. **Débogage avant remontée** : en cas d'erreur, de test qui échoue ou de comportement inattendu, l'agent effectue lui-même tout le débogage raisonnable (logs, isolation du cas, reproduction minimale, correction) avant de remonter un blocage à l'utilisateur. Un blocage n'est remonté que si la cause requiert une décision produit/métier ou un accès externe indisponible (ex. verrou V2 non levé).
3. **Commit systématique** : un commit par étape terminée (chaque écran, chaque endpoint, chaque test ajouté) — jamais un gros commit fourre-tout en fin de sprint. Message conventionnel : `feat(service-pay): ajoute le controle ENF-FIN-01`.
4. **Scope strict** : ne jamais implémenter de logique dans les dossiers hors périmètre (§1.2). Si une fonctionnalité de ce périmètre dépend d'un service non encore développé par un autre porteur, créer un adaptateur mock documenté (`// TODO(mobile): remplacer par l'API réelle de service-ida`) plutôt que d'implémenter le service manquant.
5. **Contrats avant code** : publier/mettre à jour le contrat OpenAPI/AsyncAPI dans `shared-contracts/` avant ou en même temps que l'implémentation qui l'expose.
