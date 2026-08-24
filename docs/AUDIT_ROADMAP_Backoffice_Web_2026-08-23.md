# Audit & Roadmap — Backoffice Web opérationnel (Admin / Bureau / Transporteur)

**Date** : 2026-08-23 **Branche** : `dev` **Périmètre** : `web/` (Angular, 3 rôles) — diagnostic uniquement, **aucun code modifié**.
**Méthode** : 4 audits en lecture seule (un par rôle + un transverse layout/design system), croisés avec `docs/AUDIT_CDC_v4_complet_2026-08-19.md`, `docs/PRD_FretCorridor_Web.md`, `PRODUCT.md`, `DESIGN.md`. Chaque constat ci-dessous est sourcé fichier:ligne dans les rapports d'origine (disponibles sur demande, non reproduits ici pour la lisibilité).

**Règle d'or de toute cette roadmap** : rien ici ne modifie un contrat consommé par `mobile/` ou le Moteur sans le dire explicitement. Chaque item note son risque de casse transverse. Par défaut : nouveaux endpoints en nouveaux fichiers de contrat, jamais de modification d'un contrat existant déjà consommé par un autre client.

---

## 1. Résumé exécutif

Le constat de départ ("la gestion clients manque, les interfaces sont mal structurées") est confirmé et va plus loin que prévu :

1. **Le plus critique, jamais mentionné dans les audits précédents** : côté Transporteur, les écrans Capacités et Missions affichent des **données 100 % mockées côté gateway** (`MockCapAdapter`, `MockExeAdapter`) — un vrai transporteur voit des données figées, pas les siennes. Tant que ce n'est pas branché sur `service-cap`/`service-exe`, tout le reste de l'UI Transporteur (graphiques, agrégats...) afficherait de fausses données.
2. **Deux briques métier marquées "conformes" dans l'audit CDC du 19/08 n'ont en réalité aucune interface web** : l'Observatoire de marché et les Alertes de seuil côté Bureau. Le backend existe, personne ne peut le consulter.
3. **Aucune gestion de comptes utilisateurs n'existe nulle part dans le web** (Admin gère des tenants en create-only et des dossiers KYC, jamais un compte individuel : créer, désactiver, réinitialiser, changer de rôle). C'est le trou "gestion clients" que vous aviez repéré — il est réel et structurel, pas un bug.
4. Les 3 constats structurels que vous aviez identifiés à l'œil sont **tous les trois vérifiés dans le code**, et sont des propriétés du design system partagé par les 3 rôles (donc à corriger une fois, pour tout le monde) :
   - Navigation en onglets horizontaux sous le header (décision documentée du Sprint 14, `DESIGN.md`) — pas une nav latérale.
   - Contenu plafonné à `72rem` (1152px) et centré sur **tout** écran, y compris les très grands (`--fc-content-width`, aucune justification design documentée — contrairement à la nav, rien ne protège ce choix).
   - **Zéro** librairie de dataviz dans `package.json`, zéro graphique nulle part dans les 3 rôles — uniquement des tableaux/listes.
5. Aucun des correctifs proposés n'exige de modifier un contrat consommé par le mobile ou le moteur — détail par item en section 5.

---

## 2. Constats transverses (structure UI, partagée par les 3 rôles)

### 2.1 Navigation
`ShellNavComponent` rend des onglets pilule horizontaux sous le header (`shell.component.html`), identiques pour les 3 rôles (`ONGLETS_ADMIN`, `ONGLETS_BUREAU`, `ONGLETS_TRANSPORTEUR`). Sous 720px : défilement horizontal, jamais d'empilement vertical. C'est documenté comme un choix délibéré du Sprint 14 (`DESIGN.md` §5, `docs/adr`), pas un oubli. Mais Admin et Bureau ont déjà 6 onglets chacun — à la limite haute de ce que ce pattern supporte, et ça va s'aggraver (Phase 2 paiement ajoute des écrans).

Aucun breadcrumb nulle part dans l'application. Routing plat (`app.routes.ts`), pas de groupement structurel par sous-domaine métier.

### 2.2 Largeur / densité
`--fc-content-width: 72rem` (`web/src/styles.css`) plafonne header, nav ET contenu (`.fc-page`, utilisée par **12 des 15 écrans**) sur tout écran, y compris 1920px/2560px. Aucun palier de breakpoint au-delà de 860px. Résultat : bandes de fond vides des deux côtés sur poste de travail large — exactement le contexte d'usage documenté pour Bureau et Admin (`PRODUCT.md` : "poste de travail, sessions longues"). Contrairement à la navigation, ce choix n'est justifié nulle part dans `DESIGN.md` : c'est le constat le plus solide et le plus simple à corriger des trois.

### 2.3 Absence de dataviz
`web/package.json` : aucune dépendance de charting (ni morte, ni vivante). Aucun composant chart/sparkline/stat-tile dans `web/src/app/shared/components/`. Les écrans nommés "dashboard" (ex. `kyc-dashboard`) sont en réalité des tableaux purs. Leaflet est présent mais uniquement pour la carte des axes Bureau — jamais pour de la dataviz analytique.

### 2.4 Design system
Tokens centralisés et cohérents (`styles.css`), bonne réutilisation des classes utilitaires (`.fc-page`, `.fc-table`, `.fc-panel`...). Dette repérée : chaque écran réécrit sa structure de page en HTML dupliqué (12 fichiers) au lieu d'un composant `app-page-shell` réutilisable — pas bloquant aujourd'hui, mais coûteux dès qu'on touche à la structure de page (sidebar, breadcrumb) puisqu'il faudrait modifier 12 fichiers un par un.

### 2.5 Accessibilité (WCAG AA)
Bases posées (focus visible global, cibles tactiles ≥48pt, `prefers-reduced-motion`, quelques `aria-label`/`role="alert"`) mais couverture très inégale (19 fichiers sur ~35+ templates ont au moins un attribut aria). Aucun outillage automatisé (`axe-core`/`pa11y` absent des devDependencies) — l'audit WCAG AA reste déclaratif, jamais vérifié en CI.

---

## 3. Constats par rôle

### 3.1 Admin — le cœur du reproche "gestion clients"

**Ce qui existe** : KYC (file d'attente + valider/rejeter, rien d'autre), Dossiers/modération (le module le plus riche : file de travail, dossier consolidé, décision, escalade), Journal d'audit (lecture + export CSV), Configurations (catalogue + historique versionné, le module le plus complet), Rapport financier (tableaux bruts), Tenants (liste + création **uniquement**).

**Ce qui manque, confirmé absent dans tout `web/src/app`** :
- **Aucun module de gestion de comptes utilisateurs.** Ni création, ni désactivation, ni réinitialisation de moyen d'authentification, ni changement de rôle, ni liste des comptes actifs par tenant. KYC valide une identité, Tenants gère une entité institutionnelle — aucun des deux ne gère un compte individuel. C'est un trou de couverture jamais spécifié au PRD, pas une régression.
- Tenants en create-only : pas d'édition, pas de statut actif/inactif, pas de vue détail (acteurs rattachés, volumétrie), pas de recherche.
- Journal d'audit : le paramètre `tenantId` existe côté service mais **n'est jamais exposé dans l'UI** — aucun filtre tenant/date/acteur/action. Aggrave en pratique le risque déjà documenté par l'audit CDC (export cross-tenant possible).
- KYC : pas d'historique des décisions passées (elles disparaissent de l'écran sans laisser de trace consultable), pas de détail de dossier, pas de filtre, pas de KPI.
- Rapport financier Admin : aucun total/agrégat, aucun export (incohérent avec le journal d'audit qui, lui, en a un).
- Dossiers : le PRD mentionne des preuves jointes dans le dossier consolidé — absentes (dépendance amont non livrée côté Mobile/EXE, pas un bug Admin isolé). Consultation de dossier non journalisée côté backend (écart CDC déjà connu, ENF-SEC-02).

### 3.2 Bureau — l'observatoire n'existe pas

**Ce qui existe et fonctionne bien** : Axes (vraie carte Leaflet + tableau synchronisé), Missions (filtre + détail + export CSV réel, complet), Chronologie (délègue proprement au composant partagé).

**Écart le plus grave, découvert par cet audit** : l'audit CDC du 19/08 marque **EF-BUR-03 (observatoire de marché)** et **EF-BUR-07 (alertes de seuil)** comme ✅ conformes en ne vérifiant que le backend (`ObservatoireService.java`, `AlerteSeuilController.java` existent bien) — mais **aucun composant, aucune route, aucun endpoint gateway** n'expose ces briques au web Bureau. Le Bureau ne peut littéralement pas consulter l'observatoire ni voir une alerte de seuil, alors que le PRD le prévoit explicitement (ligne 51) et que c'est précisément l'écran où des graphiques seraient naturels.

**Autres manques** :
- Positions : table brute de coordonnées, **pas de carte** — alors que le PRD (Sprint 6) exige explicitement une "carte de suivi temps réel avec horodatage et âge visible". Le composant Leaflet existe déjà (réutilisé pour les axes) mais n'a jamais été adapté aux positions.
- Rapport financier Bureau : deux tableaux bruts, aucun total, aucune tendance, aucun export.
- Notifications : pas de compteur non-lu dans la nav, pas de filtre, pas d'action (marquer lu/archiver).

### 3.3 Transporteur — données mockées, c'est prioritaire sur tout le reste

**Découverte critique** : les endpoints `/transporteur/capacites` et `/transporteur/missions` sont branchés côté gateway sur `MockCapAdapter`/`MockExeAdapter` — **des listes Java codées en dur**, documenté explicitement dans les contrats OpenAPI eux-mêmes ("TODO(mobile)", en attendant `service-cap`/`service-exe`). Un vrai transporteur connecté aujourd'hui voit des données qui ne sont pas les siennes. Tant que ça n'est pas corrigé, aucune amélioration UI de ces deux écrans n'a de valeur réelle.

**Paiement** est le seul sous-module réellement branché sur du réel (`service-pay`), mais reste un solde brut + une liste plate d'écritures : aucune agrégation par période, aucun total, aucun export, aucun graphique.

**Pas de vraie gestion de flotte** : le champ "véhicule" est une chaîne de texte libre, pas une entité structurée (pas de chauffeur, pas de disponibilité, pas de document). FE-TRP-01 du PRD promet une "vue flotte" — ce qui existe est une vue "capacités déclarées ponctuelles", pas une flotte.

Écart de sécurité déjà documenté côté CDC à corriger avant d'investir dans la présentation : `transporteurId` lu du corps de requête sur `cloture`/`confirmerLivraison`/`reversement` (`service-pay`) — un tiers du même tenant pourrait forger un bénéficiaire.

FE-TRP-04 (connecteur flotte tiers) est absent mais classé "souhaitable"/Phase 2 dans le CDC — pas urgent.

---

## 4. Roadmap priorisée

Organisée en 4 phases séquentielles. Chaque phase suppose la précédente terminée (les fondations conditionnent la valeur des phases suivantes — inutile de construire des graphiques sur des données mockées, par exemple).

### Phase 0 — Fondations et prérequis bloquants (faible risque, haute valeur)
Rien ici n'est visible du grand public mais tout le reste en dépend.

**Correction post-investigation (2026-08-23, après le "go")** : deux items initialement listés ici se sont révélés plus lourds/hors périmètre que prévu à l'implémentation, et un troisième s'est révélé déjà fait :

- ~~0.1 (brancher capacités/missions Transporteur sur le vrai backend)~~ **retiré de Phase 0.** `service-cap`/`service-exe` appartiennent au porteur **Mobile** (S4/S7, `docs/ROADMAP_OFFICIELLE_par_porteur.md`), pas Web. L'intégration `service-exe` est en plus documentée comme fonctionnellement bloquée par un bug upstream côté `service-opt`, hors périmètre Mobile lui-même (`backend/gateway/.../ExePort.java`, commentaire `application.yml`). Toucher ceci reviendrait à marcher sur le travail d'un autre porteur. **À signaler/coordonner avec le porteur Mobile, pas à corriger unilatéralement côté Web.** `service-cap` a en revanche un vrai endpoint `GET /api/cap/capacites/mes` fonctionnel (non bloqué) — brancher les Capacités seules (sans les Missions) reste une option future si le porteur Mobile confirme le contrat.
- ~~0.2 (transporteurId forgeable sur `cloture`/`confirmerLivraison`/`reversement`)~~ **retiré de Phase 0, requalifié en chantier séparé.** Ces endpoints sont activement utilisés par le flux Bureau nominal (tests d'intégration existants, rôle BUREAU) — les restreindre à ADMINISTRATION casserait ce flux. La correction propre (vérifier l'affiliation transporteur↔mission avant de payer) demanderait un appel `service-pay → service-ida`, **explicitement interdit entre porteurs de façon synchrone** par le Plan d'Exécution §4.2/4.3 (le pattern déjà en place pour `confirmation-livraison`/Kafka le confirme). Nécessite une conception dédiée (mécanisme asynchrone), pas un quick-fix. Risque déjà documenté depuis l'audit CDC du 19/08 — ce n'est pas une régression introduite ici, juste un report assumé.
- **0.7 est déjà fait.** `AlerteSeuilController` (`/api/v1/bureau/alertes`) et `observatoirePourAxe` (dans `MissionAppparieeController`) existent déjà dans le gateway, authentifiés, réellement branchés sur `service-bur` (pas des mocks) — l'audit Bureau initial ne les avait pas vus car il ne cherchait que côté frontend. Il ne reste **que les écrans Angular** à construire (repris tels quels en 1.3/1.4 ci-dessous, désormais purement frontend).

| # | Action | Pourquoi en premier | Statut |
|---|---|---|---|
| 0.3 | Rendre le filtre `tenantId` du journal d'audit **obligatoire côté UI** (Admin) | Ferme un vrai risque de sécurité déjà documenté, coût minime | ✅ Fait (2026-08-23) — sélecteur de tenant explicite, "tous les tenants" reste possible mais n'est plus un défaut silencieux |
| 0.4 | Introduire un composant `app-page-shell` (remplace la structure `.fc-page` dupliquée dans 12 templates) | Prépare sidebar/breadcrumb sans réécrire 12 fichiers plus tard | ✅ Fait (2026-08-23) — les 15 écrans migrés (`extraClass` pour les 3 qui scopaient un style local), 153/153 tests verts, build propre |
| 0.5 | Élargir `--fc-content-width` (ou introduire un second token `--fc-content-width-wide` pour les écrans tabulaires denses) | Le correctif structurel le plus simple et le plus visible immédiatement | ✅ Fait (2026-08-23) — 72rem → 100rem |
| 0.6 | Ajouter `axe-core`/`pa11y` au pipeline de test | Objectiver l'audit WCAG AA au lieu de rester déclaratif | ✅ Fait (2026-08-23) — `jest-axe` global (`setup-jest.ts`), premiers tests sur `ShellNavComponent`/`JournalAuditComponent`/`PageShellComponent` ; à étendre aux autres écrans (Phase 3.6) |

### Phase 1 — Fonctionnalités P0 : rendre le backoffice réellement opérationnel

| # | Action | Rôle |
|---|---|---|
| 1.1 | **Module de gestion des comptes utilisateurs** (créer / désactiver / réactiver / réinitialiser le moyen d'authentification / changer de rôle, par tenant) | Admin | ✅ Statut/rôles faits (2026-08-24) — lister/désactiver/réactiver/changer les rôles, via 3 nouveaux endpoints `service-ida` (`/api/ida/comptes`) relayés par la gateway. **Création de compte et réinitialisation de PIN volontairement hors périmètre** — voir §3 |
| 1.2 | Tenants : édition, statut actif/inactif, vue détail (acteurs rattachés), recherche | Admin | ✅ Édition/statut/recherche faits (2026-08-23) — vue détail (acteurs rattachés) reste à faire, dépend du futur module comptes (1.1) |
| 1.3 | Écran Observatoire de marché (courbes médiane/IQR par axe, indicateur de déséquilibre) | Bureau | ✅ Fait (2026-08-23) — chiffres bruts pour l'instant (pas de courbe, dataviz repoussée à 2.3), estimation de marché incluse |
| 1.4 | Écran Alertes de seuil (liste/bannière des alertes actives par axe) | Bureau | ✅ Fait (2026-08-23) — création/liste/suppression, état évaluable/déclenchée |
| 1.5 | Carte de suivi temps réel pour les positions (réutiliser/dériver le composant Leaflet des axes), tableau en vue de repli | Bureau | ✅ Fait (2026-08-23) — `PositionsMapComponent`, marqueurs ponctuels |
| 1.6 | KPIs en tête de chaque écran "dashboard" des 3 rôles (dossiers en attente/retard, KYC en attente, écart de réconciliation, tenants actifs, solde ventilé transporteur...) — chiffres simples avant tout graphique | Admin, Bureau, Transporteur | ✅ Fait (2026-08-23) pour KYC/dossiers/tenants (Admin) et les 3 rapports financiers (1.7) ; écart de réconciliation restant hors périmètre (pas d'écran dédié) |
| 1.7 | Agrégation + export sur les 3 rapports financiers (Admin, Bureau, Transporteur) : totaux, ventilation par période/mode, export CSV a minima | Les 3 | ✅ Fait (2026-08-23) — `TotauxEcrituresComponent` + export CSV client, ventilation par période restant à faire (P2) |
| 1.8 | Traçabilité renforcée sur la consultation de dossier (qui a déjà consulté, cohérent avec le gap ENF-SEC-02 déjà connu côté backend) | Admin | ✅ Fait (2026-08-23) — `DOSSIER_CONSULTE` journalisé côté `service-adm` |

### Phase 2 — Navigation, dataviz et fonctionnalités P1

| # | Action | Rôle |
|---|---|---|
| 2.1 | `ShellSidebarComponent` (sidebar gauche rétractable, groupes logiques par rôle) réutilisant les données déjà exposées par `ShellNavComponent` — garder la nav horizontale actuelle en fallback mobile/drawer | Transverse |
| 2.2 | Breadcrumb minimal (rôle > écran) intégré à `app-page-shell` | Transverse |
| 2.3 | Premier lot de widgets dashboard réutilisables : `fc-stat-tile`, `fc-sparkline` (librairie recommandée : `ngx-charts`, SVG natif Angular, thémable, accessible — voir §6) | Transverse, déployé d'abord sur KYC dashboard + rapports financiers |
| 2.4 | Historique des décisions KYC passées (validées/rejetées), pas seulement la file en attente | Admin |
| 2.5 | Recherche globale transverse (par ID mission/acteur/dossier/tenant) | Admin |
| 2.6 | Centre de notifications internes Admin (escalades, écarts de réconciliation, KYC en retard) | Admin |
| 2.7 | Drill-down mission → dossier → paiement (aujourd'hui les écrans sont cloisonnés) | Bureau, Admin |
| 2.8 | Compteur de notifications non lues + filtres + actions (marquer lu/archiver) | Bureau |
| 2.9 | Vue de gestion de flotte structurée (entité Véhicule distincte du trajet ponctuel, disponibilité récurrente) | Transporteur |
| 2.10 | Alertes documents KYC/assurance expirant | Transporteur |
| 2.11 | Widgets `fc-courbe-temporelle`, `fc-repartition`, `fc-jauge-seuil` diffusés sur les rapports financiers des 3 rôles | Transverse |

### Phase 3 — Confort, densité avancée, diffusion complète (P2)

| # | Action |
|---|---|
| 3.1 | Grille en colonnes adaptative sur grand écran (nouveaux breakpoints `--fc-bp-lg`/`--fc-bp-xl`) pour les écrans tabulaires les plus denses |
| 3.2 | Exports avancés (Excel/PDF) sur tous les modules de reporting, pas seulement CSV |
| 3.3 | Pagination généralisée sur tous les tableaux (aucun n'en a aujourd'hui) |
| 3.4 | Confirmation avant actions irréversibles (nouvelle version de config, décision de dossier, création de tenant/compte) |
| 3.5 | Descriptions/aide contextuelle sur les clés de configuration |
| 3.6 | Extension complète de la couverture aria/role à tous les écrans (pas seulement les 19 déjà couverts) |
| 3.7 | FE-TRP-04 — écran de configuration de connecteur flotte tiers (déjà classé "souhaitable"/Phase 2 CDC) |
| 3.8 | Vue garantie/caution transporteur (backend `GarantieService` déjà présent, jamais exposé en UI) |
| 3.9 | Libellés lisibles des UUID bruts (transporteur/véhicule) dans les vues Bureau consommées transversalement — piste déjà notée dans `docs/CONTEXTE_SESSION_UI.md` |

---

## 5. Risques de casse transverse — synthèse consolidée

Aucune action de cette roadmap ne modifie un contrat déjà consommé par le mobile ou le moteur, sous réserve de respecter ces garde-fous :

- **Nouveaux endpoints uniquement en nouveaux fichiers de contrat** (`shared-contracts/openapi/`) pour l'observatoire et les alertes de seuil (1.3/1.4) — ne jamais modifier un fichier de contrat existant déjà consommé.
- **Gestion de comptes (1.1)** : si elle touche au référentiel identité, ne jamais dupliquer le modèle `Acteur` côté `service-adm` — consommer/déclencher via `service-ida` (propriétaire du KYC/auth côté Mobile) à travers la gateway, jamais un accès direct ou une table parallèle. C'est déjà le pattern en place pour KYC, à répliquer.
- **Correction du mock Transporteur (0.1)** : vérifier que le futur adaptateur réel produit exactement les champs attendus par `capacite.models.ts`/`MissionResponse` côté Angular — pas de validation de schéma stricte actuellement, un changement de forme de réponse casserait le front silencieusement. Les endpoints web (`/transporteur/capacites`, `/transporteur/missions`) sont distincts des endpoints mobiles (`/capacites`, `/capacites/mes`) donc pas de risque direct sur le mobile, mais toute évolution du DTO `Ecriture`/`Capacite` partagé avec `service-pay`/`service-cap` doit rester additive.
- **Correction `transporteurId` (0.2)** : `PaiementController` (`cloture`, `confirmerLivraison`, `reversement`) est potentiellement appelé aussi par le mobile chauffeur — tout correctif doit être testé contre les deux clients (web ET mobile) avant déploiement.
- **Sidebar/largeur/page-shell (0.4, 0.5, 2.1, 2.2)** : `ShellComponent`/`ShellNavComponent`/tokens `styles.css` sont partagés par les 3 rôles — toute modification les affecte tous les trois par nature. C'est voulu (fondation commune), mais si un rôle doit diverger visuellement des deux autres, prévoir un scoping explicite (classe CSS par route, variante de composant) plutôt qu'une modification globale non intentionnelle.
- **Rien ne touche `service-pay`** au-delà du point 0.2 déjà documenté par l'audit CDC — aucun risque sur les invariants financiers testés par ArchUnit (ENF-FIN-01/02/03).

---

## 6. Note technique — choix de librairie de dataviz

Recommandation : **`ngx-charts`** (composants Angular natifs, rendu SVG donc thémable via les tokens CSS existants et accessible aux lecteurs d'écran, contrairement à un canvas Chart.js). Alternative plus légère si le besoin reste limité à 2-3 types de graphiques : composants SVG maison (sparklines/jauges), cohérent avec la philosophie de sobriété du design system et sans alourdir le bundle. Dans tous les cas, respecter la règle déjà actée dans `DESIGN.md` : jamais un statut porté par la seule couleur d'un graphique, toujours un libellé/légende texte à côté.

---

## 7. Ce qui n'est PAS dans cette roadmap (hors périmètre volontaire)

- Les écarts de sécurité backend déjà documentés dans `docs/AUDIT_CDC_v4_complet_2026-08-19.md` et `docs/AUDIT_dev_Web_2026-08-20.md` (ex. `RoleProtectedSampleController` toujours ouvert, secret JWT `service-ida` non paramétrable) ne sont pas répétés ici — ils restent pilotés par ces documents. Le point 0.2 (transporteurId) est repris ici uniquement parce qu'il conditionne directement la fiabilité de l'UI paiement.
- Aucune proposition ne concerne les applications mobiles (`mobile/app_client`, `mobile/app_chauffeur_transporteur`) ni le Moteur — hors périmètre de cet audit, qui porte exclusivement sur `web/`.
- **Réinitialisation du PIN d'un compte (1.1)** : nécessiterait un flux OTP/SMS dédié pour rester sûr (un admin ne doit jamais pouvoir lire/définir directement le PIN d'un tiers) — non construit, à concevoir séparément si le besoin se confirme.
- **Création de compte depuis l'écran Admin (1.1)** : l'inscription/enrôlement passe déjà par des flux dédiés (inscription légère mobile, enrôlement agent terrain) ; dupliquer un chemin de création côté Admin suppose de décider quel flux reproduire (OTP SMS ?), non tranché ici.

## 8. Découverte notable en cours de Phase 1 (2026-08-24, hors scope initial de l'audit du 23/08)

En construisant 1.1 (gestion des comptes), investigation du gateway a révélé que **l'écran KYC Admin (`/admin/kyc`) tourne intégralement sur un mock** : `KycPort` est toujours lié à `MockIdaKycAdapter` (3 dossiers codés en dur, jamais persistés), alors que `AUDIT_CDC_v4_complet_2026-08-19.md` ne le signalait pas comme tel. Un admin qui valide/rejette un KYC aujourd'hui agit sur des données de démonstration, pas sur de vrais dossiers `service-ida`.

Contrairement aux items 0.1/0.2 (Phase 0), **rien n'interdit architecturalement de corriger ceci** : le pattern gateway → service-ida en appel synchrone est déjà utilisé en production ailleurs (`RealIdaProfilAdapter`, `RealAffiliationAdapter`, et le nouveau `RealIdaCompteAdminAdapter` de ce commit) — c'est uniquement que personne n'a encore écrit l'équivalent `RealIdaKycAdapter` ni les endpoints service-ida associés (liste des KYC en attente par tenant + décision, distincts des endpoints de complétion de profil déjà là dans `KycController`).

**Non corrigé dans cette session** (scope déjà large pour une seule session) — proposé comme prochain chantier Phase 1/2 : ajouter à `service-ida` un pendant admin de `KycController` (liste par tenant + décision, avec journalisation), puis `RealIdaKycAdapter` côté gateway pour remplacer le mock. Même ampleur que 1.1, mêmes garde-fous (rôle ADMINISTRATION vérifié dans le JWT, tenant scoping).
