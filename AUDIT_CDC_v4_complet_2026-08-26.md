# Audit complet indépendant — FretCorridor V4 (26 août 2026)

**Branche** : `dev` **Commit vérifié** : `1226fa4` (référence précédente : `04ebc7a`, audit du 23/08)
**Source CDC** : `docs/CDC_FretCorridor_v4_FSE2026004_lecture.pdf` + `docs/FretCorridor_Plan_Execution_V4_2.docx`
**Méthode** : 3 audits parallèles en lecture seule (backend/moteur, mobile, web), aucune confiance aveugle dans les audits précédents (19/08, 23/08) — chaque affirmation re-vérifiée dans le code actuel, citée fichier:ligne. Aucune modification de code pendant l'audit (les 2 correctifs mentionnés en §0 ont été faits et vérifiés séparément, avant le lancement des 3 audits).

---

## 0. Correctifs trouvés et corrigés le 26/08, avant cet audit

| # | Bug | Correctif | Commit |
|---|---|---|---|
| 1 | Suivi GPS : l'app Client affichait "Position GPS pas encore disponible" en permanence dès que le chargeur et le transporteur appartiennent à des tenants différents (cas normal du marketplace) | `PositionController`/`PositionService`/`PositionRepository` (service-flt) : filtrait par le tenant du **lecteur** (chargeur) au lieu du `missionId` seul (UUID déjà suffisant, non devinable) | `fea3877` |
| 2 | Propositions rang 2/3 ("2e/3e meilleur prix") affichaient un score de coût composite (ex. 2.22 XAF) au lieu d'un vrai prix XAF | `AffectationL1Service.publierAlternatives` (service-opt) recalcule désormais un vrai prix via `TarificationL4Service`, omet l'alternative si mode dégradé plutôt que d'inventer un prix | `baf8a30` |
| 3 | Régression du test `AffectationL1ServiceTest` suite au correctif #2 | Mock aligné sur le nouveau comportement (même prix tarifé pour les alternatives d'un même axe/véhicule) | `1226fa4` |

---

## 1. Backend / Moteur

**Correctifs du jour — vérifiés en code (test 2/2 vert sur le #1, suite ciblée verte sur le #2/#3)**, voir tableau §0.

### 5 points ouverts par l'audit du 23/08 — tous résolus depuis

| Point | Statut | Preuve |
|---|---|---|
| Capacité jamais réutilisée après un match | ✅ Résolu | `CapaciteService.java:237-260` |
| `GET /api/opt/affectations/{id}` sans clé interne | ✅ Résolu | `AffectationController.java`, `SecurityConfig.java` |
| Création de tenant sans vérification de rôle | ✅ Résolu | `TenantController.java` — exige `ADMINISTRATION` |
| Coefficient volumétrique en dur (200.0) | ✅ Résolu | `DemandeService.java` lit `Axe.parametres` |
| Suppression capacité décrémentée (FK) | ✅ Résolu | commit `0a6441a` |

### Grands chantiers CDC / Plan d'Exécution

| Sprint | Attendu | Statut | Preuve |
|---|---|---|---|
| S13 | Connecteurs flotte tiers | ❌ Absent | aucune classe/endpoint trouvé, non démarré |
| S14 | Paiement MoMo/Orange réel | ❌ Toujours mock | `MockPrestatairePaiementAdapter.java:11-13` |
| S16 | Plan de chargement, flux réel | ✅ Fait | `PlanChargementConfirmeListener` (service-exe) |
| S17 | Indicateurs marché par axe | ✅ Fait (backend) | `ObservatoireService.java` (service-bur) |
| S18 | Sélection de tenant réelle | ✅ Fait | `AffiliationTenant`, `/api/ida/affiliations` |
| S19 | Litige app_client, endpoint réel | ✅ Fait | `DossierController` (service-adm) |
| S20 | Exports PDF/Excel | ⚠️ Partiel | CSV seul — `MissionAppparieeCsvExporter.java` |
| — | Oracle 3D bin-packing réel | ⚠️ Approximation assumée | `OracleChargementService.java:20-33` |
| ADR 0011 | Isolation multi-tenant serveur (ENF-MUL-01) | ⚠️ Dette tracée | `RealGeoAdapter` — colle le tenant sans vérifier serveur (côté GEO, `AxeController` lui, filtre réellement depuis le 09/08) |

### Autres constats

- **Tests** : 548 `@Test` au total (vs 494 au 23/08, +54 cohérent avec S16/S18/S19). Exécution complète non refaite (contrainte de temps) ; exécution ciblée a révélé la régression du §0-#3, désormais corrigée.
- **JWT `CHANGE_ME`** : valeur de repli identique dans les 12 `application.yml`, toujours dépendante de la config Render en prod — inchangé.
- **Migrations Flyway** : aucun conflit détecté (service-geo 9 dont la nouvelle V9 tenant, service-opt 20, service-mat 5, service-cap 3, service-trk 2).

### Reste à faire — Backend

1. Lancer la suite complète des 548 tests avec `JAVA_HOME=21` pour écarter d'autres régressions silencieuses.
2. Ajouter un test HTTP pour `CoutController` (corrigé deux fois en 3 jours, zéro couverture).
3. S13/S14 restent hors périmètre Phase 1/2 — cohérent avec le phasage, à rappeler explicitement en présentation, pas un oubli.

---

## 2. Mobile — app_client & app_chauffeur_transporteur

21 commits depuis le 23/08. Les 3 mocks explicites signalés sont démockés.

### Mocks du 23/08 — état actuel

| Fonctionnalité | Attendu | Statut | Preuve |
|---|---|---|---|
| Plan de chargement (S16) | Données réelles du Moteur | ✅ Démocké | `plan_chargement_provider.dart:65` |
| Sélection tenant (S18) | Résolution réelle des affiliations | ✅ Démocké | `tenant_selection_provider.dart:47` |
| Litige app_client (S19) | Dossier réel créé côté serveur | ✅ Démocké | `litige_provider.dart:50` — `POST /dossiers` |
| Suivi GPS arrière-plan | Continue si l'app est tuée par l'OS | ⚠️ Toujours trompeur | pas de `ACCESS_BACKGROUND_LOCATION` ; `Timer.periodic` Dart pur (`position_provider.dart:139`) |
| Litige chauffeur — photo | Photo jointe transmise | ⚠️ Toujours partiel | `mission_detail_screen.dart:359-361` — jamais envoyée, pas d'endpoint upload côté service-adm |

### Scénario prioritaire — publier → déclarer → matcher → accepter → prise en charge → suivi → paiement

| Étape | App | Statut | Preuve |
|---|---|---|---|
| Publier une demande | Client | ✅ Fait | `demande_provider.dart:92` |
| Déclarer une capacité | Chauffeur | ✅ Fait | `capacite_provider.dart:93` |
| Propositions (matching) | Client | ✅ Fait | `demande_provider.dart:125` |
| Accepter une proposition | Client | ✅ Fait | `demande_provider.dart:139` (gère 409/503) |
| Prise en charge (photo + signature) | Chauffeur | ✅ Fait | `mission_provider.dart:195-204` (multipart réel) |
| Suivi GPS + position | Client / Chauffeur | ⚠️ Partiel | `suivi_provider.dart:47,66` réel mais fragile en arrière-plan |
| Choix du moyen de paiement | Client | ✅ Fait | `choix_paiement_provider.dart:72` |
| Solde / historique paiement | Chauffeur | ✅ Fait | `paiement_provider.dart:78` |

**Réserve hors mobile** : le règlement effectif MoMo/Orange dépend de `MockPrestatairePaiementAdapter` côté serveur (S14). Le mobile transmet et affiche correctement le vrai statut ; seul l'encaissement reste simulé.

### Autres briques confirmées inchangées

KYC niveau 1, enrôlement Agent, notifications, token en `flutter_secure_storage` avec refresh automatique : tous ✅ réels, aucune régression. Traduction FR/EN : ✅ complète sur les écrans métier des deux apps (234 clés app Client, 299 app Chauffeur).

### Reste à faire — Mobile

1. Suivi GPS réellement en arrière-plan (`ACCESS_BACKGROUND_LOCATION` + foreground service Android).
2. Endpoint d'upload côté service-adm pour la photo de litige chauffeur.
3. Étendre le suivi automatique du GPS au redémarrage d'app / changement d'état réseau.

---

## 3. Portail Web — Bureau, Transporteur, Admin

66 suites / 254 tests exécutés, tous verts (vs 47 suites / 147 tests au 23/08).

| Sprint | Attendu | Statut | Preuve |
|---|---|---|---|
| Bureau / Transporteur / Admin | Écrans métier réels, sans donnée mockée | ✅ Fait | 3 portails, tous branchés HTTP |
| Sécurité (`roleGuard`, session, intercepteur) | 19 routes protégées, session non persistante | ✅ Fait | `role.guard.ts`, `auth.service.ts:39` |
| S17 — Observatoire de marché | Écran indicateurs marché par axe | ✅ Fait (nouveau depuis le 23/08) | `observatoire.component.ts` — chiffres bruts, pas de graphique (Phase 2 dataviz prévue) |
| S18 — Second bureau | Création d'un second tenant institutionnel | ⚠️ Partiel | `affiliation.service.ts:8-10` — affiliation a posteriori (invitation = validation immédiate), pas de vrai second tenant institutionnel créé en base au-delà de BGFT |
| S20 — Exports conformité | Export PDF/Excel | ❌ Absent | CSV sur 5 écrans (missions, journal audit, rapports financiers Bureau/Admin, paiement Transporteur), aucun PDF/Excel |
| Tests | Suite exécutée, verte | ✅ Fait | 66 suites / 254 tests |

**Note** : dépendance `jest-axe` présente en `package.json` mais absente de `node_modules` avant réinstallation locale — vérifier que `npm ci` tourne bien en CI pour éviter cette régression.

### Reste à faire — Web

1. Export PDF/Excel (S20) — missions, journal audit, rapports financiers.
2. Clarifier avec le métier si l'affiliation a posteriori (S18) suffit au CDC ou si un vrai flux de création de tenant institutionnel est requis en Phase 3.
3. Observatoire (S17) : dataviz graphique non faite, à planifier si attendu.

---

## Synthèse — reste à faire, toutes plateformes confondues, priorisé

1. **Suivi GPS réellement en arrière-plan** (mobile chauffeur) — aujourd'hui le suivi s'arrête si l'app est mise en veille/tuée par l'OS ; c'est la brique la plus visible du scénario prioritaire de présentation.
2. **Exécuter la suite complète des 548 tests backend** avec le bon `JAVA_HOME`, pour confirmer l'absence d'autres régressions silencieuses.
3. **Test HTTP `CoutController`** — corrigé deux fois en 3 jours, toujours zéro couverture.
4. **Photo de litige chauffeur non transmise** — endpoint d'upload manquant côté service-adm.
5. **Exports PDF/Excel (S20)** — seul CSV existe, backend comme web.
6. **Clarifier l'ambition réelle du S18** — l'affiliation a posteriori fonctionne, mais n'est pas la création d'un second tenant institutionnel complet.
7. **S13 / S14** — connecteurs flotte tiers et paiement MoMo/Orange réel : non démarrés, cohérent avec le phasage du CDC, pas un oubli de dernière minute.

---

*Compilé à partir de 3 audits indépendants en lecture seule (backend, mobile, web), commits `fea3877` → `1226fa4` sur `dev`. Méthode : re-vérification code source systématique, citation fichier:ligne, aucune confiance aveugle dans les rapports antérieurs.*
