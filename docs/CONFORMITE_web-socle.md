# Conformité `feature/web-socle` — CDC v4 et Plan d'Exécution V4.2

**Date** : 2026-08-08
**Auteur** : Mourad (volet Web), avec assistance Claude Code
**Objectif** : vérifier, exigence par exigence et avec preuve à l'appui (fichier/ligne, test), que le travail livré sur `feature/web-socle` respecte le CDC v4.0 (FSE-CDC-FRETCORRIDOR-2026-004) et le Plan d'Exécution V4.2, pour le périmètre porté par le volet Web (gateway, service-pay, service-bur, service-adm, app Angular).

Méthode : lecture des exigences EF-PAY/EF-BUR/EF-ADM/EF-IDA/ENF-* du CDC (§9-§10) et de leur phasage dans le Plan d'Exécution (§5), confrontée au code réellement présent sur la branche (tests inclus, pas seulement les intentions des messages de commit).

---

## 1. Ce qui est bien respecté, avec preuve

### 1.1 Invariant financier (§1.3, §12.5 du CDC) — ENF-FIN-01/02/03

C'est l'exigence la plus critique du CDC (« FretCorridor ne détient jamais de fonds de mission ») et elle est correctement vérifiée par des tests automatisés, pas seulement par convention :

- **ENF-FIN-01** (aucun chemin de code ne crée une écriture de trésorerie FretCorridor) : test ArchUnit qui interdit toute classe nommée `*FretCorridor(Compte|Solde|Tresorerie)*` dans `com.fretcorridor.pay`, et verrouille `TypeCompte` à exactement 3 variantes, toutes tierces (`COMPTE_TRANSPORTEUR`, `COMPTE_CHARGEUR`, `COMPTE_SEQUESTRE_PRESTATAIRE`).
  → [`EnfFin01Test.java`](../backend/service-pay/src/test/java/com/fretcorridor/pay/domain/EnfFin01Test.java)
- **ENF-FIN-02** (aucun reversement sans encaissement) : `GrandLivreService.enregistrerReversement` refuse tout reversement sans encaissement préalable enregistré, refuse un montant supérieur à l'encaissement, refuse un second reversement une fois l'encaissement consommé. 5 tests couvrant les cas limites.
  → [`EnfFin02Test.java`](../backend/service-pay/src/test/java/com/fretcorridor/pay/domain/EnfFin02Test.java)
- **ENF-FIN-03** (réconciliation quotidienne, alerte bloquante sur écart) : présent (`ReconciliationService`, `EnfFin03Test`, `AlerteReconciliation`).
- Correspond à l'anti-patron explicitement proscrit par le CDC §12.4 (« permettre un reversement avant encaissement, par quelque chemin que ce soit — RG-075 ») : ce chemin est fermé et testé, pas seulement documenté.

### 1.2 Isolation multi-tenant et RBAC (ENF-MUL-01, EF-IDA-08, §5.5/ENF-SEC-04)

Suite de tests d'isolation réelle, pas symbolique — vérifiée sur 7 contrôleurs (`geo`, `cap`, `trk`, `opt`, `exe`, `not`, `adm`). Exemple : [`AxeControllerIsolationTest.java`](../backend/gateway/src/test/java/com/fretcorridor/gateway/infrastructure/rest/geo/AxeControllerIsolationTest.java) authentifie deux Bureaux de tenants distincts (Douala, N'Djamena) et vérifie qu'aucun ne voit les axes de l'autre, puis vérifie qu'un rôle Transporteur reçoit `403` sur une route Bureau. C'est exactement la « suite de tests d'isolation qui tente activement, à chaque livraison, des accès transverses délibérées » exigée par ENF-SEC-04.

Côté web, la garde de routes RBAC (`roleGuard`, Feuille de route §3.4 🔴) redirige vers `/login` si non authentifié et vers `/403` si le rôle ne correspond pas, sans jamais déclencher la requête HTTP protégée — testé (`role.guard.spec.ts`).

### 1.3 Modules Phase 1 correctement scopés et livrés

Le Plan d'Exécution (§5.1) place en Phase 1 exactement : EF-PAY-01/03 (encaissement orchestré, séquestre), EF-PAY-02/04 (grand livre, reversement bloqué), EF-PAY-05 (webhooks), EF-BUR-01/02/03 (supervision, export, observatoire), EF-ADM-01/02/06 (file de travail, dossier consolidé, config versionnée). Vérifiés présents :

- `SequestreService.declencher/liberer` — séquestre logique fonctionnel (EF-PAY-01/03).
- File de travail priorisée + dossier consolidé + `ConfigurationVersionnee` — présents (EF-ADM-01/02/06).
- Supervision cartographique/tabulaire Bureau (axes, missions, positions, chronologie) — présente (EF-BUR-01/03).

### 1.4 Position toujours datée (anti-patron RG-043)

Le CDC proscrit explicitement (§12.4) « afficher une position sans son âge ». Le commit `dce8dca` (« ajoute le suivi temps réel du Bureau avec âge obligatoire ») traite spécifiquement ce point ; à vérifier que `PositionController`/`PositionResponse` continuent d'exposer cet âge à chaque évolution du modèle.

### 1.5 Déviations documentées plutôt que silencieuses

Chaque écart aux hypothèses du CDC/Plan d'Exécution est tracé dans `docs/adr/` avec justification (ports non standards, Java 17 au lieu de 21, PWA différée, carte schématique en attendant GEO réel, etc.). C'est conforme à l'esprit du CDC : les écarts sont acceptables s'ils sont explicites et réversibles, pas s'ils sont silencieux.

---

## 2. Écarts identifiés, et leur traitement (mise à jour 2026-08-08)

### 2.1 EF-BUR-02 — export des flux supervisés absent (M, Phase 1) → **corrigé**

Le CDC exige : « le système doit permettre le **filtrage, le détail et l'export** des flux supervisés » (M). [`missions-list.component.ts`](../web/src/app/features/bureau/missions/missions-list.component.ts) n'offrait qu'un affichage brut de la liste.

**Correction apportée** :
- Backend (`MissionAppparieeController`) : filtrage par `statut` et `axeId` (query params), endpoint de détail `GET /api/v1/bureau/missions-appariees/{missionId}` (404 sans fuite d'existence si la mission appartient à un autre tenant — testé), endpoint d'export `GET /api/v1/bureau/missions-appariees/export` (CSV, mêmes filtres, même en-tête `Content-Disposition` que l'export ADM existant pour rester cohérent).
- Web (`missions-list.component.ts/.html`) : contrôles de filtre (statut, axe), panneau de détail au clic, bouton d'export déclenchant un téléchargement CSV (même pattern que `JournalAuditComponent.exporter()`, déjà en place côté Admin).
- Tests : 6 nouveaux tests backend (`MissionAppparieeControllerFiltreExportTest`, y compris un test qui vérifie qu'on ne fuite pas une mission d'un autre tenant via le détail) + 3 nouveaux tests Angular. Suite complète gateway : verte.

### 2.2 EF-PAY-05 — notifications entrantes du prestataire : pas de webhook signé/idempotent → **corrigé**

Le CDC (M, Phase 1) exige une vérification cryptographique et un traitement idempotent des **notifications entrantes** du prestataire de paiement. `MockPrestatairePaiementAdapter` fonctionnait en pull seul (`obtenirReleve`), sans endpoint entrant.

**Correction apportée** :
- Domaine : `NotificationPrestataireService` (vérifie la signature → vérifie l'idempotence → enregistre l'encaissement via `GrandLivreService` existant), ports `SignatureVerifierPort` / `NotificationIdempotencePort`, exception `SignatureInvalideException`.
- Infra : `HmacSha256SignatureVerifierAdapter` (HMAC-SHA256, comparaison en temps constant via `MessageDigest.isEqual`, secret externalisé `FRETCORRIDOR_PAY_WEBHOOK_SECRET` — ENF-SEC-05), persistance JPA des clés d'idempotence déjà traitées (survit à un redémarrage), `WebhookPrestataireController` (`POST /api/v1/pay/webhooks/prestataire`, en-têtes `X-Prestataire-Signature`/`X-Prestataire-Idempotency-Key`, 401 si signature invalide, 200 idempotent si rejeu).
- Tests : 4 tests domaine (signature invalide rejetée sans écriture, notification valide traitée, rejeu avec même clé non dupliqué, rejeu falsifié rejeté), 4 tests unitaires HMAC, 3 tests d'intégration HTTP bout en bout avec Postgres réel (Testcontainers). Suite complète service-pay (31 tests, y compris ENF-FIN-01/02/03) : verte.
- Le verrou V2 (aucun prestataire agréé sélectionné) reste inchangé : cet endpoint est prêt à recevoir de vraies notifications, mais rien ne les émet encore en développement — c'était déjà le cas avant, ce correctif ferme l'écart texte de l'exigence, pas le verrou métier.

### 2.3 Configuration hors base pour le seuil d'agrégation → **non corrigé, par choix de proportionnalité**

`AgregationMissionsService` reçoit son seuil via `@Value("${fretcorridor.bur.seuil-agregation:3}")` plutôt qu'une configuration versionnée en base comme `service-adm`. Ce n'est pas l'anti-patron strict visé par le CDC §12.4 (barèmes/clés de répartition réglementaires) ni une exigence MoSCoW du CDC — c'est une incohérence de style entre deux services du même volet, déjà correctement externalisée (pas de valeur codée en dur, conforme à ENF-SEC-05). Répliquer le mécanisme `ConfigurationVersionnee` d'ADM (entité, audit, historique de versions) pour un seul entier ne semblait pas proportionné juste avant une fusion vers `dev`. À faire si l'équipe veut une cohérence stricte entre BUR et ADM, mais ce n'est pas bloquant.

---

## 3. Ce qui ressemble à un écart mais n'en est pas un — correctement hors périmètre Phase 1

Le Plan d'Exécution (§5.2, §5.3) rephase volontairement une partie des exigences MoSCoW du CDC vers les phases suivantes. Ces éléments sont **absents du code actuel, et c'est normal** :

| Exigence | Contenu | Phase prévue |
|---|---|---|
| EF-BUR-04/05/06/07 | Seuil d'agrégation minimal (déjà fait, en avance), couverture d'échantillon, **journal nominatif des consultations**, alertes configurables | Phase 3 |
| EF-ADM-03/04/05 | Grille de décision versionnée, **recours instruit par un opérateur différent du premier décideur**, **escalade automatique** sur délai plafond | Phase 3 |
| EF-PAY-06/07/08/09 | Monnaie électronique/virement/terme, mode espèces signalé, reversement auto à expiration du délai de contestation | Phase 2 |

Point d'attention repéré en marge : `EscaladeService.detecterEtEscalader` (Phase 3, donc pas encore exigé) existe déjà en Phase 1 sous forme d'un endpoint déclenché à la demande, avec un `TODO` explicite dans le code indiquant qu'un vrai ordonnanceur (scheduler) remplacera cet appel manuel en Phase 2. C'est cohérent et assumé — à ne pas confondre avec une fonctionnalité Phase 3 « terminée en avance » : elle est esquissée, pas conforme à EF-ADM-05 telle quelle (« automatiquement »).

`EF-BUR-04` (seuil d'agrégation), en revanche, est déjà pleinement implémenté et testé alors qu'il est prévu Phase 3 — livré en avance, sans inconvénient identifié.

---

## 4. Ce qui n'a pas été vérifié dans cette passe

- Les exigences ENF-PRF (latence < 2 s au p95), ENF-DIS (disponibilité, RPO/RTO) : non mesurables sur ce dépôt sans environnement de charge.
- L'accessibilité (ENF-A11Y-01, contraste AA) côté web n'a pas été auditée visuellement.
- L'intégralité du code des écrans Transporteur/Admin (au-delà des points ci-dessus) n'a pas été relue ligne à ligne.

---

## 5. Synthèse

Le socle transactionnel et sécuritaire (invariant financier, isolation multi-tenant, RBAC) est solide et **vérifié par des tests qui échoueraient réellement en cas de régression** — c'est le point le plus important du CDC et il est bien couvert. Le phasage Phase 1/2/3 du Plan d'Exécution est globalement respecté (rien de Phase 2/3 n'a été construit prématurément, sauf le seuil d'agrégation, sans conséquence).

Les deux écarts Phase 1 identifiés (EF-BUR-02, EF-PAY-05) ont été corrigés le 2026-08-08, avec tests à l'appui (backend et web) — voir §2.1 et §2.2. Le seul point restant est mineur et documenté comme un choix assumé (§2.3, seuil d'agrégation hors base). En l'état, `feature/web-socle` respecte le CDC v4 et le Plan d'Exécution V4.2 sur l'ensemble du périmètre vérifié dans ce document.

**Note indépendante de cette vérification** : un chantier d'internationalisation (i18n) est en cours, non commité, ailleurs dans l'arbre de travail au moment de cette passe. Il fait échouer 13 suites de tests Angular préexistantes (dont certaines touchées par les corrections ci-dessus, ex. `missions-list.component.spec.ts`) pour une raison sans rapport avec ce document : les libellés de statut renvoient désormais une clé de traduction (`enum.missionStatut.CONFIRMEE`) au lieu du texte français, et les specs concernées n'ont pas encore été mises à jour pour fournir `TranslateService` en test. Ce n'est pas un écart CDC/Plan d'Exécution — à ne pas confondre avec les points de ce rapport, et à traiter par la personne qui mène cette migration.
