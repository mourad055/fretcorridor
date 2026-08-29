# FretCorridor v4 — Transmission d'état (mise à jour 26 août 2026)

## Règle absolue (méthode de travail) — à respecter dans toute session

**Avant toute action non triviale (créer un script, changer une procédure,
proposer une méthode de lancement/déploiement), vérifier d'abord (1) que ça
correspond à la manière de travailler déjà en place dans le projet — chercher
si une solution existe déjà avant d'en inventer une nouvelle — et (2) que
c'est cohérent avec le CDC et le Plan d'Exécution.** Ne jamais partir sur une
solution ad hoc sans avoir d'abord regardé l'existant.

**Incident qui a motivé cette règle (24 août)** : besoin de démarrer les
microservices backend pour un test live avant une présentation. Un script
Maven (`infra/start-backend.sh`, `mvn spring-boot:run` par service) a été
créé et documenté dans le `README.md` **sans vérifier au préalable** si le
projet avait déjà sa propre méthode. Il en avait une : chaque service porte
un `docker-compose.service-X.yml` (sous `backend/<service>/`), prévu pour
être combiné avec `infra/docker-compose.yml` via plusieurs `-f` — c'est
d'ailleurs comme ça que `service-flt` tournait déjà en conteneur au moment de
l'incident (vérifiable via `docker inspect <conteneur> --format
'{{.Config.Labels}}'`, qui liste les fichiers compose d'origine). L'utilisatrice
a repéré l'écart et fait corriger : script supprimé, `README.md` mis à jour
avec la vraie commande `docker-compose -f infra/docker-compose.yml -f
backend/<service>/docker-compose.service-X.yml ... up -d` (un `-f` par
service), processus Maven arrêtés. **Réflexe à appliquer désormais** :
`grep`/`find` l'existant (fichiers de config, scripts, conventions déjà
présentes dans le dépôt) avant de proposer quoi que ce soit de nouveau, et
vérifier CDC/Plan d'Exécution avant toute décision qui touche à
l'architecture ou au périmètre d'une fonctionnalité.

---

> **Session diagnostic GPS + 2 bugs tenant + audit CDC complet (26 août)** —
> suite directe des sessions des 24-25 août (résumées ci-dessous faute
> d'avoir été consignées ici sur le moment ; détail complet dans
> `recap-session-2026-08-24.md`, racine du dépôt).
>
> **Résumé 25 août, jamais noté ici** : validation téléphone — un premier
> correctif (`disableLengthCheck: true` sur `IntlPhoneField`, les 5 écrans
> login/inscription des 2 apps + destinataire) avait désactivé toute
> validation de longueur pour corriger un rejet silencieux, alors que la
> librairie applique déjà la bonne longueur par pays nativement. Corrigé
> deux fois de suite sur retour explicite de l'utilisatrice avant de
> revenir aux réglages par défaut. Repli `getLastKnownPosition()` avant
> `getCurrentPosition()` ajouté côté `capacite_provider.dart` et
> `position_provider.dart` (un fix `getCurrentPosition` haute précision
> pouvait rester bloqué indéfiniment en intérieur).
>
> **26 août — diagnostic GPS "aucune position envoyée" : cause réelle
> trouvée par élimination, PAS un bug de code.** Traces `debugPrint`
> temporaires ajoutées à chaque étape de `_envoyerUnePosition()`
> (`position_provider.dart`) pour observer en direct sur téléphone. Dès que
> le backend (12 services) et le tunnel `adb reverse` sont réellement up,
> le suivi fonctionne immédiatement (`getLastKnownPosition()` ~20ms,
> `_envoyer -> true`, confirmé en base `service_flt.positions` toutes les
> 30s) — le symptôme du 25/08 venait d'un backend resté arrêté après un
> incident de sécurité (voir plus bas), pas d'un défaut Flutter/Riverpod.
> Traces retirées après confirmation, `gradle.properties` corrigé
> (`-Xmx8G` → `-Xmx1536m` : la machine de test a 7,5 Go de RAM, un heap
> Gradle de 8 Go déclenchait des kills OOM systématiques une fois le
> backend + Docker déjà en mémoire — un process `claude` lui-même a été
> tué une fois par l'OOM killer pendant cette session).
>
> **Mais un VRAI bug de suivi GPS existait quand même, trouvé ensuite en
> testant côté app Client** : l'écran "Suivi" affichait "Position GPS pas
> encore disponible" en boucle malgré une mission "Prise en charge" et des
> positions bien reçues en base. Cause : `PositionController.getDerniere`
> (service-flt) filtrait `GET /positions/mission/{id}/derniere` par le
> `tenantId` du JWT du **lecteur** (le chargeur, ex. `MARKETPLACE_CM`),
> alors que la position est enregistrée sous le tenant du **chauffeur**
> qui exécute la mission (ex. `tenant-bgft-douala`) — même classe de bug
> que celui déjà corrigé le 22 août sur `MissionController.getChronologiePourDemande`
> (voir plus bas dans l'historique), pas encore traité côté service-flt à
> l'époque. Corrigé en filtrant uniquement par `missionId` (UUID déjà
> suffisant, non devinable), comme la chronologie le fait déjà. Commit
> `fea3877`.
>
> **Rappel utile trouvé en creusant ce bug (concept tenant, souvent mal
> compris)** : un tenant = un **Bureau de Gestion du Fret Terrestre**
> institutionnel réel (BGFT = Douala), PAS un split par rôle
> chargeur/transporteur — confirmé par l'ADR 0010/0011. En Phase 1, il
> n'existe qu'un seul vrai Bureau (`tenant-bgft-douala`), auquel tout
> chauffeur/transporteur est assigné automatiquement à l'inscription
> (`AuthService.java`, service-ida, constante en dur, jamais un choix
> utilisateur) ; `MARKETPLACE_CM` n'est pas un Bureau, juste la valeur par
> défaut d'un chargeur auto-inscrit. Le S18 ("second bureau") vérifié ce
> jour est un vrai mécanisme d'**affiliation a posteriori** (le Bureau
> invite un compte existant vers son tenant, fonctionnel de bout en bout
> web→gateway→service-ida→DB) mais **pas** la création d'un second Bureau
> institutionnel complet — pas de choix de tenant à l'inscription nulle
> part, mobile ou web.
>
> **Second bug trouvé en creusant une question sur les prix affichés** :
> l'écran Propositions montrait un rang 1 à 50 000 XAF puis des rangs 2/3 à
> "2.22 XAF" / "2.23 XAF" — absurde. Cause : `AffectationL1Service.publierAlternatives`
> (service-opt) republiait `BigDecimal.valueOf(candidat.cout())`, le score
> composite Kuhn-Munkres normalisé [0,1] (service-mat, sert uniquement à
> **classer** les candidats) comme s'il s'agissait d'un prix XAF pour les
> rangs 2/3, alors que le rang 1 utilise un vrai calcul tarifaire
> (`TarificationL4Service`). Corrigé pour recalculer un vrai prix pour les
> alternatives aussi (sans itinéraire Valhalla, `distanceMetres=null`,
> même intention réseau déjà documentée) ; alternative omise plutôt
> qu'inventée si le régime de tarification repasse en mode dégradé
> (ENF-DIS-04). Commit `baf8a30`. **A cassé `AffectationL1ServiceTest`**
> (le mock attendait encore l'ancien score comme prix) — réparé dans la
> foulée, commit `1226fa4` : avec un même axe/type de véhicule pour tous
> les candidats du test, le vrai prix tarifé est désormais identique pour
> les 2 alternatives (attendu, le prix dépend du poids/distance/type de
> véhicule, pas de la capacité précise choisie).
>
> **Audit CDC complet en 3 volets parallèles** (background agents,
> re-vérification indépendante du code, aucune confiance aveugle dans les
> audits précédents) : backend/moteur, mobile (2 apps), web. Résultat
> détaillé : contenu repris et à jour dans `plan-fretcorridor-reorientation.md`
> (racine du dépôt) -- l'audit du 26/08 lui-même a été retiré, superseded.
> Verdict global : les 5 points ouverts par l'audit du 23/08 tous résolus,
> S17 (observatoire marché) passé d'absent à fait côté web depuis le
> 23/08, S18/S19/S16 mobile confirmés démockés. Manques inchangés : S13
> (connecteurs flotte), S14 (paiement MoMo/Orange réel — toujours
> `MockPrestatairePaiementAdapter`), S20 (export PDF/Excel — CSV
> seulement), suivi GPS mobile toujours pas un vrai service arrière-plan
> Android, photo de litige chauffeur jamais transmise (pas d'endpoint
> upload service-adm).
>
> **`dev` synchronisé pendant cette session** : 3 commits d'un coéquipier
> récupérés par fast-forward propre (`1226fa4..4835894`, aucun conflit) —
> migration `V9__corriger_tenant_id_axes_selon_data_dev.sql` (service-geo,
> corrige des axes de démo mal tenantés), KYC réel côté gateway/service-ida
> (`RealIdaKycAdapter` remplace le mock, `KycAdminController`), portail
> Bureau : carte OSRM pour les axes, pagination, légende, libellé tenant
> lisible (`libelle-tenant.ts`, affiche "BGFT Douala" au lieu de
> `MARKETPLACE_CM`/`tenant-bgft-douala` bruts).
>
> **Incident de sécurité pendant cette session, transparence totale** :
> des commandes `adb` destinées à l'écran de connexion de l'app ont atterri
> par erreur dans le volet de réponse rapide WhatsApp d'une notification,
> avec un numéro de téléphone pré-rempli à côté d'un bouton d'envoi.
> Aucun envoi n'a eu lieu — arrêt immédiat avant tout tap sur "envoyer",
> retour arrière (`KEYCODE_BACK` ×2), vérification par capture d'écran que
> rien n'était parti, signalé immédiatement à l'utilisatrice. Elle a
> demandé un arrêt complet puis une reprise explicite avant de continuer.
> **Leçon** : vérifier par capture d'écran l'état exact avant chaque tap
> `adb` quand plusieurs écrans/notifications peuvent se superposer, plutôt
> que d'enchaîner des taps à l'aveugle sur des coordonnées supposées.
>
> **`README.md` et ce fichier étaient périmés (dernière modification
> 24 août)** malgré ~60 commits (mobile + web + backend) depuis — mis à
> jour cette session sur demande explicite de l'utilisatrice. Un rappel :
> penser à les tenir à jour à la fin de chaque session, pas seulement
> quand on le remarque deux jours plus tard.

---

# Historique (mise à jour 24 août 2026)

> **Session audit croisé + fix capacité (23 août)** — suite directe de la
> session mockups/bugs live ci-dessous. Point de départ : l'utilisatrice a
> reçu un rapport d'audit d'un coéquipier (branche `backend-stevetelecom`,
> 494 tests, ~15 modules) et a demandé une vérification indépendante
> complète (web/mobile/moteur) + confirmation que `dev` local était bien
> synchronisé avec `origin/dev`.
>
> **`dev` local était en retard sur `origin/dev`** — les coéquipiers avaient
> poussé la veille (ALNS, intégration Valhalla, `CalculateurCoutSeptTermes`,
> `InstrumentationPerfService`, modifs `MatchingCycleService`) sans que la
> session ne les récupère. Fast-forward propre (`b4790c3..04ebc7a`), aucun
> conflit. **Leçon** : `git fetch` peut rester bloqué silencieusement sur une
> invite d'identifiants sans TTY — utiliser `GIT_TERMINAL_PROMPT=0 git fetch`
> pour échouer vite plutôt que de croire à tort que le dépôt est à jour sur
> la seule foi d'un `git status` (qui ne lit que le ref distant **en cache**,
> jamais rafraîchi tant que le fetch n'a pas réussi).
>
> **Audit indépendant en 4 volets parallèles** (background agents, jamais
> confiance aveugle dans le rapport du coéquipier ni dans l'audit du
> 19 août) : backend/moteur, mobile, web, re-vérification des 18 bloquants
> du 19/08. Résultat détaillé : `AUDIT_CDC_v4_complet_2026-08-23.md`
> (racine du dépôt). Verdict global : rapport du coéquipier globalement
> fiable ; **17/18 bloquants du 19 août confirmés résolus** dans le code
> actuel ; web intégralement confirmé (47/47 specs, 147 tests exécutés,
> tous verts) ; mobile : 3 fonctionnalités que le rapport présentait comme
> acquises sont en réalité des **mocks explicitement annotés dans le code**
> ("⚠️ MOCK, aucun appel réseau") : plan de chargement (S16), sélection
> tenant (S18), litige côté app_client (S19).
>
> **Bug de capacité résiduelle (le plus important, trouvé la nuit
> précédente pendant les tests live) : corrigé.** `CapaciteEnAttente`
> (service-opt) marquait `traitee=true` **définitivement** dès le premier
> match, sans jamais vérifier le reliquat (`capaciteResiduelleKg`) restant
> côté service-cap — pire, le rang 1 (affectation directe Kuhn-Munkres,
> le flux normal) ne décrémentait **jamais** la capacité réelle, seul le
> flux d'acceptation explicite d'une proposition rang 2/3 le faisait
> (`ServiceCapClient.reserver`, service-mkt). Un camion de 20T apparié à
> 500kg voyait ses 19,5T restantes perdues pour le système. **Fix** :
> nouveau `ServiceCapClient` côté service-opt (même pattern que
> `ServiceMatClient`), appelé juste après `AffectationConfirmee` pour
> réserver le poids matché ; `CapaciteService.decrementerCapacite`
> (service-cap) républie désormais `CapaciteDeclaree` quand un reliquat
> exploitable subsiste — `CapaciteDeclareeListener` (déjà existant côté
> opt, non modifié) crée naturellement une nouvelle `CapaciteEnAttente`
> non traitée. Best-effort (ENF-DIS-04) : un échec réseau ponctuel laisse
> juste la capacité intacte jusqu'au prochain décrément réussi, jamais de
> perte de donnée silencieuse.
>
> **3 autres corrections du même audit** : `GET /api/opt/affectations/{id}`
> n'avait aucune vérification de clé interne (seule protection :
> l'imprévisibilité de l'UUID) — aligné sur le pattern
> `X-Internal-Service-Key` déjà utilisé partout ailleurs, `ServiceOptClient`
> (service-trk) transporte désormais la clé. Création de tenant
> (service-adm) : un JWT valide suffisait sans vérification de rôle —
> réservé désormais à `ADMINISTRATION`. Poids taxable service-mkt :
> coefficient volumétrique codé en dur (200.0), incohérent avec le
> coefficient (333.0) déjà résolu par axe côté service-cap pour la même
> grandeur physique (RG-101) — résolu désormais depuis `Axe.parametres`,
> même clé des deux côtés.
>
> **3 items mobiles identifiés MAIS volontairement PAS corrigés cette
> session — à ne pas traiter comme de simples oublis** : (1) plan de
> chargement S16 — l'événement backend (`PlanChargementConfirmeEvent`)
> porte lui-même la mention "BROUILLON, contrat non encore validé avec
> Mobile" ; construire l'intégration sans coordination risquerait un
> contrat différent de celui qu'un coéquipier a peut-être déjà en tête.
> (2) sélection tenant S18 — nécessiterait un vrai support multi-tenant
> côté serveur, alors que tout le système repose sur le hack mono-tenant
> Phase 1 (ADR 0011, cf plus bas) : décision d'architecture/produit, pas
> un bug ponctuel. (3) litige côté app_client S19 — l'endpoint réel existe
> (`POST /api/v1/dossiers`, service-adm) mais exige un champ
> `delaiTraitement` (Instant) que rien ne définit côté chargeur ; le
> renseigner aurait exigé d'inventer une règle métier absente du CDC.
> **Si une session future s'attaque à l'un des trois, commencer par
> obtenir/valider le contrat ou la décision produit manquante, pas
> réimplémenter le mock directement.**
>
> **Suite du 23 août — S18 corrigé, langue FR/EN construite.** L'utilisatrice
> a vérifié elle-même le Plan d'exécution (Sprint 18) et corrigé mon
> affirmation initiale : "Sélection de tenant au login (si multi-bureau)"
> **est bien spécifié** côté Plan (backend service-ida+gateway "isolation
> renforcée, marque blanche" / mobile "sélection au login" / web "portail
> second bureau distinct") — seul le MÉCANISME d'affiliation (qui accorde
> l'accès à un second tenant) n'était pas détaillé. Question posée
> explicitement à l'utilisatrice → réponse : **c'est le second bureau qui
> invite/valide, jamais le transporteur** (l'invitation EST la validation,
> aucun flux d'acceptation côté transporteur).
>
> **S18 implémenté pour de vrai** : nouvelle table d'affiliation
> (`AffiliationTenant`, service-ida) séparée du tenant d'origine de l'acteur
> (jamais modifié — reste l'identité KYC canonique) ; `JwtService` sait
> émettre un JWT scopé à un tenant différent après vérification de
> l'affiliation. **Découverte architecturale importante en cours de route** :
> la gateway n'est PAS un simple proxy vers service-ida — elle émet son
> PROPRE JWT (secret distinct) qui embarque le token service-ida en
> "délégation" (claim `idaToken`, double autorité JWT, cf.
> `ServiceIdaAuthenticationAdapter`). Toute nouvelle fonctionnalité d'auth
> côté app Chauffeur/Transporteur (qui passe par la gateway, contrairement à
> app Client) doit donc être doublée : un endpoint service-ida qui fait le
> vrai travail + un port/adapter/controller gateway qui le relaie et
> réémet SON PROPRE JWT avec les nouvelles infos (mécanisme déjà utilisé par
> `IdaProfilPort`/`RealIdaProfilAdapter` pour la complétion KYC — suivi ici à
> l'identique, pas réinventé). Non traité : le portail web "second bureau
> distinct" (l'invitation est fonctionnelle via API,
> `POST /api/v1/bureau/affiliations`, mais sans interface web).
>
> **Langue FR/EN (EF-NOT-05) construite en parallèle**, sur simple "fais la
> langue" — l'écran `langue_screen.dart` (les deux apps) était un mock figé
> sur "Bientôt disponible" pour l'anglais, sans la moindre infrastructure
> i18n derrière (zéro `flutter_localizations`, zéro fichier `.arb`, ~300+
> chaînes en dur rien que côté écrans). Infrastructure `flutter_localizations`
> + `gen-l10n` complète et réelle dans les deux apps (persistance du choix
> via `flutter_secure_storage`, `MaterialApp` câblé). **Écrans convertis
> cette session : le socle commun aux deux apps** (accueil, connexion,
> inscription, menu, paramètres, langue, accueil principal, centre d'aide,
> CGU, politique de confidentialité) — testable dès maintenant en changeant
> la langue dans Paramètres. **Écrans métier de chaque app PAS encore
> convertis** (toujours en français en dur, aucune régression — simplement
> pas traduits) : côté app Client — publier une demande, suivi, propositions,
> paiement, notifications, mes demandes, litige, compléter profil ; côté
> app Chauffeur — missions, capacité, véhicules, KYC, plan de chargement,
> etc. Mécanique déjà rodée si une session future veut continuer : extraire
> chaque `Text('...')` vers une clé ARB (fr+en), remplacer par
> `AppLocalizations.of(context).cle`, `flutter gen-l10n`, `flutter analyze`.
>
> **Suite immédiate (même session, 23 août) : S19 et S16 corrigés pour de
> vrai, S18 volontairement laissé en l'état — raison ci-dessous.**
>
> **S19 (litige app_client)** : le contrat `POST /api/v1/dossiers`
> (service-adm) ne portait ni `motif` ni `description` (pensé pour un
> dossier ouvert côté ADM avec parties/preuves structurées, pas pour la
> plainte initiale d'un chargeur) — étendu avec ces deux champs texte
> libre. `delaiTraitement` devient optionnel : un chargeur n'a aucune idée
> d'un délai de traitement administratif, un délai par défaut (72h,
> hypothèse d'équipe documentée dans `DossierController`) s'applique
> maintenant à la place d'un rejet. `litige_provider.dart` appelle
> réellement l'endpoint (nouveau client Dio direct vers service-adm, même
> raisonnement que les autres — aucune route gateway pour le rôle
> Chargeur).
>
> **S16 (plan de chargement)** : `PlanChargementConfirmeEvent` était déjà
> publié pour de vrai par service-opt (`SequencementDeclencheur`) mais
> **aucun service ne le consommait** — canal mort, pas un contrat inventé
> ici. Complété plutôt que redéfini : nouveau listener + entité
> `PlanChargementEtape` côté service-exe (corrélée à l'`EtapeTournee`
> locale par `(tourneeId, rang)` — les deux entités `EtapeTournee`, opt et
> exe, ont des ids internes distincts, rang est la seule clé partagée),
> `GET /missions/tournees/{tourneeId}` (déjà utilisé par l'écran tournée
> multi-étapes, S11) expose désormais `chargesParEssieu` par étape,
> propagé à travers 3 couches de DTO gateway jusqu'ici silencieuses sur ce
> champ (records stricts, Jackson ignore un champ inconnu sans erreur).
> Mobile : positions/orientations de colis **retirées** de l'écran (le
> Moteur ne les calcule pas, contrat colis 3D absent) — seule la
> répartition de poids par essieu (donnée réelle, garantie faisable par
> construction puisque l'oracle ne publie jamais un état rejeté) est
> restituée.
>
> **S18 (sélection tenant) : NON traité, à dessein.** Contrairement à
> S16/S19, aucune primitive backend n'existe à compléter — `Acteur.telephone`
> porte une contrainte **UNIQUE globale** en base (`service-ida`), donc un
> numéro de téléphone ne peut structurellement appartenir qu'à UN SEUL
> tenant aujourd'hui. Supporter "un acteur choisit son tenant" exigerait de
> concevoir de zéro un modèle d'affiliation multi-tenant (nouvelle table
> de rattachement, mécanisme d'octroi de l'affiliation, ré-émission d'un
> JWT après sélection) — rien de tout cela n'est spécifié dans le CDC ni
> déjà esquissé dans le code. C'est une décision de produit/architecture,
> pas une correction — à ne pas trancher seul, surtout que ça touche
> directement le hack mono-tenant Phase 1 (ADR 0011) sur lequel tout le
> reste du backend s'appuie. **Si une session future s'y attaque : d'abord
> obtenir la décision produit sur comment l'affiliation est accordée,
> avant tout schéma de base de données.**
>
> **Suite du 23 août (soir/nuit) : traduction FR/EN des écrans métier
> terminée dans les deux apps + audit de vérification pour la présentation
> du 24 août.** L'utilisatrice a demandé de continuer la traduction
> "écrans métier" au-delà du socle commun (voir plus haut), puis un audit
> ciblé sur les bugs de capacité/matching trouvés en test live, en vue
> d'une présentation le lendemain.
>
> **Traduction FR/EN : tous les écrans métier des deux apps sont
> maintenant convertis** (plus aucun `Text('...')` en dur côté écrans
> listés comme "pas encore convertis" plus haut). Côté app Client :
> publier une demande, suivi, propositions, paiement, notifications, mes
> demandes, litige, compléter profil. Côté app Chauffeur : missions
> (liste + détail), capacité (déclarer + "Mes capacités"), véhicules, KYC,
> plan de chargement, tournée multi-étapes, enrôlement agent,
> notifications, carrousel promo. `flutter analyze` propre (0 issue) sur
> l'ensemble des deux apps après chaque écran. Règle de traduction
> appliquée partout : le texte dynamique venant du backend (messages
> d'erreur des providers de paiement, `motifClassement` du Moteur, motifs
> de litige envoyés en texte libre à service-adm, `EtapeMission.libelle`)
> reste volontairement en français, non traduit — le traduire côté UI sans
> toucher ce qui est réellement transmis/persisté côté serveur créerait un
> décalage entre ce que l'app affiche et ce que le backend reçoit. Chaque
> occurrence est commentée dans le code au moment où elle a été
> délibérément laissée de côté.
>
> **Incident de coordination pendant cette phase** : une deuxième session
> Claude Code tournait en parallèle sur le même répertoire de travail
> (donc directement sur les mêmes fichiers, pas deux clones séparés) pour
> traiter 4 des écrans Chauffeur restants. Coordination établie par
> message inter-session, répartition convenue, puis cette deuxième
> session a été arrêtée par erreur par l'utilisatrice avant d'avoir
> committé son travail. **Aucune perte** : elle n'avait ajouté que des
> clés ARB (FR uniquement, pas encore les clés EN ni le câblage Dart), le
> reste du travail a été repris et terminé dans cette session. Point de
> vigilance pour une future session parallèle sur ce dépôt : committer
> plus souvent réduit la fenêtre de perte en cas d'arrêt accidentel.
>
> **Audit de vérification du bug de capacité résiduelle (le plus
> important trouvé en test live, corrigé plus haut le 23 août matin) :
> confirmé tenant toujours en code et couvert par les tests.** Relecture
> du code actuel (`AffectationL1Service.calculerAffectationOptimale`,
> service-opt : l'appel `serviceCapClient.reserver(...)` après
> `AffectationConfirmee` est bien présent ; `CapaciteService.decrementerCapacite`,
> service-cap : republie bien `CapaciteDeclaree` quand un reliquat
> subsiste) + exécution réelle des suites de tests (pas seulement lecture
> de code) :
> - `service-opt` (moteur de matching, inclut `AffectationL1ServiceTest`
>   qui cible directement la réservation de capacité au rang 1) :
>   **49/49 tests verts**.
> - `service-cap` (décrément + republication de capacité, inclut
>   `CapaciteServiceConcurrenceTest`) : **9/9 tests verts**.
> - `service-mkt` (coefficient poids taxable, autre correctif du même
>   audit) : **6/6 tests verts**.
> - `service-trk` (clé interne `X-Internal-Service-Key`, autre correctif
>   du même audit) : **14/14 tests verts**.
> - `service-adm` (rôle `ADMINISTRATION` requis pour créer un tenant,
>   dernier correctif du même audit) : **34/39 tests verts** — les 5 en
>   échec sont uniquement des tests d'intégration (`TenantControllerIntegrationTest`
>   et 4 autres) qui échouent à **télécharger l'image Docker `postgres:16`**
>   (`Can't get Docker image`, pas d'accès réseau sortant dans ce sandbox)
>   — pas un défaut de code. Le test unitaire de la règle elle-même
>   (`TenantServiceTest`) passe (2/2). **À vérifier en conditions réelles
>   avant la présentation** si possible : lancer la stack complète
>   (`docker compose up`, backend a accès réseau normal hors de ce
>   sandbox) et rejouer `mvn test` sur `service-adm`, ou simplement tester
>   manuellement la création de tenant avec un rôle non-ADMINISTRATION
>   (doit être rejetée en 403).
>
> **Rappel Java pour relancer les tests backend sur cette machine** :
> `JAVA_HOME` pointe par défaut vers Java 17, mais tous les modules
> backend ciblent Java 21 (`<java.version>21</java.version>`) — préfixer
> les commandes `mvn` avec `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`
> sous peine d'un échec immédiat ("class file version 65.0... only
> recognizes up to 61.0").
>
> **Conclusion pour la présentation du 24 août** : les bugs de
> capacité/matching remontés pendant les tests live du 22-23 août sont
> corrigés dans le code actuel de `dev` et vérifiés par 78 tests verts sur
> les 5 modules concernés (`service-opt` + `service-cap` + `service-mkt` +
> `service-trk`, complet ; `service-adm`, complet côté unitaire). Aucun
> nouveau problème de capacité/matching détecté pendant cette vérification.
> Traduction FR/EN mobile complète côté écrans métier (socle + tous les
> écrans listés ci-dessus). Hors périmètre mobile, toujours vrai : portail
> web "second bureau" (S18) non construit (côté collègue), S13/S14
> connecteurs flotte/paiements réels, S17 observatoire marché UI, S20
> exports conformité, Oracle 3D complet (positions/orientations colis) et
> Phase 4 — ces manques sont documentés plus haut, pas des oublis de
> dernière minute.

---

# Historique (mise à jour 22 août 2026, après-midi)

> **Session bugs live + mockups (22 août, journée)** — suite directe de la
> session CRUD ci-dessous, tests en continu avec l'utilisatrice.
>
> **Bug le plus important trouvé cette session : le suivi/paiement
> chargeur ne fonctionnait pour AUCUNE demande, depuis toujours.**
> `MissionController.getChronologiePourDemande` filtrait par
> `tenantId` du JWT du chargeur (ex. `MARKETPLACE_CM`), mais
> `Mission.tenantId` porte systématiquement le tenant PHASE 1 côté
> exécution/transporteur (`tenant-bgft-douala`, constante posée par
> `AffectationConfirmeeListener`) — les deux ne correspondent **jamais**.
> Résultat : l'écran Suivi répondait "pas encore disponible" en boucle
> même avec une Mission réelle déjà en "Prise en charge", masquant la
> section paiement qui en dépend. Corrigé en filtrant par `demandeId`
> seul (UUID non devinable, connu du seul chargeur propriétaire — même
> principe que `notificationAppartenantA` côté service-not), le JWT
> restant vérifié pour l'authentification. **Symptôme à surveiller si ça
> revient** : tout endpoint qui croise un tenant "marketplace" (chargeur)
> avec un tenant "exécution phase 1" (transporteur/chauffeur) est
> suspect — les deux univers de tenant ne se recoupent structurellement
> pas dans ce MVP mono-tenant hack (ADR 0011).
>
> **Canal de notifications entièrement mort, depuis le début** :
> `TypeNotification.PROPOSITION_RECUE`/`STATUT_MISSION` existaient déjà
> dans l'entité côté service-not, mais **aucun service n'appelait jamais**
> `NotificationService.creer()` pour une demande publiée, une proposition
> émise ou une capacité déclarée — juste 2 listeners Kafka
> (`AlerteEcartListener`, `PropositionRetourAVideListener`) sur des
> canaux annexes. Ajouté : endpoint interne
> `POST /api/notifications/interne` (X-Internal-Service-Key, même
> pattern que `POST /api/cap/capacites/*/decrement`) + appels
> best-effort depuis `DemandeService.publier()`/`PropositionEmiseListener`
> (service-mkt) et `CapaciteService.declarer()` (service-cap). Web only
> — pas de push FCM, donc ajout aussi d'un **sondage périodique (20s) +
> son/vibration** (`NotificationNotifier`, les deux apps) : avant ça, une
> notification ne devenait visible qu'en rouvrant manuellement l'écran
> Notifications, jamais en tâche de fond.
>
> **Auto-régression trouvée et corrigée en cours de session** : une
> reconstruction de l'app Client (après reprise de contexte) utilisait de
> mauvais ports `--dart-define` pour NOT (8090 au lieu de 8094), EXE
> (8091 au lieu de 8093), GEO (`/api` en trop, la base ne doit PAS
> l'inclure) et PAY (8098 au lieu de 8088) — masquait paiement,
> notifications et résolution d'axe sans lien avec le vrai bug ci-dessus.
> **Toujours vérifier les valeurs par défaut documentées dans
> `dio_provider.dart` avant de composer une commande `flutter run`**,
> surtout après une reprise de session — ne jamais les retaper de
> mémoire.
>
> **`service-pay` n'avait jamais été démarré avant cette session** (11ᵉ
> service backend, oublié du script de relance depuis le début) — démarré
> pour la première fois, port 8088, `SPRING_DATASOURCE_URL` sur le port
> 5434 comme les autres (défaut du service pointe sur 5432).
>
> **Annuler une demande ne prévenait jamais le Moteur (service-opt)** —
> reproduit en direct : une capacité fraîchement déclarée a été
> aussitôt consommée par une ancienne demande déjà annulée, laissant la
> nouvelle demande sans capacité disponible. `DemandeService.annuler()`
> publie désormais un événement Kafka `DemandeAnnulee`, consommé par un
> nouveau listener côté service-opt qui retire la demande de
> `opt.demande_en_attente` avant qu'un cycle ne la traite.
>
> **Infos demande propagées jusqu'à Mission (3ᵉ itération du même
> pattern)** : `destinataireNom/Telephone`, `modeCollecte`,
> `typeDisponibilite`, `poidsTotalKg`, `grandeValeur` — même chaîne
> Kafka déjà utilisée pour marchandise (`DemandePubliee` → `opt.demande_en_attente`
> → `AffectationConfirmee` → `Mission`/`MissionDto`). Piège rencontré :
> `AffectationConfirmeeEvent` avait déjà un champ `modeCollecte`
> (placeholders `"DEPOT"/"RETRAIT"` internes au calcul d'itinéraire,
> sans lien avec `Demande.modeCollecte`) — collision de nom sur un
> `record` Java = erreur de compilation immédiate, pas un bug silencieux;
> renommé en `demandeModeCollecte` pour lever l'ambiguïté.
>
> **3 écrans reconstruits à partir de mockups fournis par
> l'utilisatrice** (règle stricte : "suivre mes mockups à la lettre") —
> `missions_screen.dart` (app Chauffeur), `suivi_screen.dart` et
> `propositions_screen.dart` (app Client). Quand deux mockups étaient
> fournis pour un même écran avec des retours contradictoires ("la mise
> en page du 1er est bien mais les icônes du 2e sont meilleures"),
> **combiner les deux plutôt que choisir** — c'est ce qu'elle voulait à
> chaque fois.
>
> **Process** : deux documents produits explicitement à sa demande (un plan
> avant les travaux, `recap-session-2026-08-22.md` après, tableau demandé/fait
> -- le plan du 22/08 a depuis été retiré, superseded) — elle a dit ne plus
> vouloir se répéter,
> donc systématiser ce couple plan→récap dès qu'une liste de demandes
> s'accumule plutôt que d'attendre qu'elle le redemande.
>
> **DB nettoyée deux fois cette session** (missions/demandes/capacités
> de test datant d'avant les correctifs successifs) — toujours confirmer
> avant un `DELETE` multi-tables large (le classifier de l'environnement
> bloque ces commandes par défaut, ce qui est un signal sain à respecter,
> pas à contourner).

---

# Historique (mise à jour 22 août 2026, matin)

> **Session CRUD + icône adaptative (21→22 août, nuit)** — suite directe
> de la session pipeline matching ci-dessous, même soirée de test
> continue. **Nouvelle règle stricte posée par l'utilisatrice** : mettre à
> jour ce fichier après chaque merge sur `dev` (ou quand elle le demande
> explicitement) ; un audit complet du projet vs CDC/Plan d'Exécution est
> prévu comme tâche séparée dédiée (trop volumineux pour être glissé en
> parallèle de correctifs live), à lancer sur demande explicite.
>
> **CRUD demande/capacité** (elle avait explicitement demandé "modifier,
> supprimer surtout") :
> - Suppression : `DELETE /api/demandes/{id}` (service-mkt) — annule
>   (`StatutDemande.ANNULEE`, existait dans l'enum sans jamais être
>   atteint par aucun code) plutôt qu'une suppression physique ; refusée
>   si une proposition est déjà acceptée. Capacité : `supprimer()` déjà
>   présent côté `capacite_provider.dart`, juste jamais exposé dans l'UI
>   avant cette session (écran "Mes capacités").
> - **Modification** : pas de vrai endpoint PATCH dans aucun des deux cas
>   (recalculer poids résiduel déjà entamé pour une capacité, ou
>   rerésoudre l'axe/les coordonnées pour une demande, est plus risqué
>   qu'utile pour du test). "Modifier" réutilise le formulaire de
>   déclaration existant pré-rempli (`PublierDemandeScreen.demandeAModifier`,
>   `CapaciteScreen.capaciteAModifier`) : republie/redéclare avec les
>   nouvelles valeurs puis annule/supprime l'ancienne entrée sur succès.
>   `CapaciteDeclaree` (app_chauffeur) exposait pas `vehiculeId` avant
>   cette session — ajouté, nécessaire pour pré-remplir le véhicule.
>
> **Icône d'application — saga en 5 correctifs, cause réelle trouvée
> seulement au 4ᵉ essai** : les deux premiers essais (marge 6% puis
> 1,5% sur le PNG brut) n'ont RIEN changé visuellement — cause : le
> projet n'avait **aucune icône adaptative**
> (`mipmap-anydpi-v26/ic_launcher.xml`), seulement des PNG "legacy".
> Sur Android 8+, Pixel Launcher applique un rétrécissement/masquage
> **automatique supplémentaire** aux icônes legacy pour les harmoniser
> avec les vraies icônes adaptatives des autres apps — peu importe le
> remplissage du PNG source, ce rétrécissement s'ajoutait par-dessus.
> Ajout d'une vraie icône adaptative (foreground + `ic_launcher_background`
> couleur) a réglé la sous-taille, mais le 1ᵉʳ essai adaptatif (marge 1%,
> quasi plein cadre) a révélé un **second piège** : le masque circulaire
> du lanceur recadre les **coins** du carré adaptatif — un contenu logo
> en format paysage (le camion) qui s'étend jusqu'aux bords se fait
> couper net par le cercle inscrit. Toujours **simuler le masque
> circulaire (PIL, `ImageDraw.ellipse`) sur le foreground avant de
> reconstruire** plutôt que de juger sur le PNG carré brut — ça aurait
> évité l'aller-retour. Marge finale : 20% (contenu à 60% du canevas),
> ajustée par petites touches sur retour utilisateur direct (13% encore
> jugé "trop à l'avant"). **Piège de process à retenir** : une icône
> éditée ne se reflète pas toujours avec un simple `flutter run` (réinstall
> `-r`) — le lanceur peut garder l'ancien bitmap en cache ; désinstaller
> complètement (`adb uninstall`) puis réinstaller à neuf est le seul
> moyen fiable de vérifier un changement d'icône.
>
> **Autres correctifs** : débordement d'affichage (`Wrap` au lieu d'un
> `Row` à plat) sur la carte de demande une fois Modifier/Annuler ajoutés
> à côté de Suivi ; nettoyage des demandes de test `AXE_NON_DESSERVI`
> (données de session précédentes, sans rapport avec un bug).
>
> **Web "Bureau de fret" (`localhost:8099`) + `service-bur`/`service-pay`
> ne font PAS partie du setup testé cette session** (uniquement les 2 apps
> mobiles + backend host `mvn spring-boot:run`) — mais l'utilisatrice a
> confirmé que **le web fait partie du projet** et devra être lancé aussi
> avant toute démo publique (les 3 apps). Pas encore fait, à prévoir.
>
> **IP du laptop encore instable cette nuit** — changée au moins 3 fois
> de plus après la précédente note (`172.22.171.133` → `192.168.1.142` →
> `192.168.1.53`, stable ensuite sur `.53` pour le reste de la session).
> Chaque changement casse silencieusement les deux apps ("erreur réseau",
> "je ne vois pas les demandes/capacités") puisque l'IP est figée au
> build (`--dart-define`) — réflexe : `ip -4 addr show` dès qu'un symptôme
> réseau généralisé apparaît après une pause.

---

# Historique (mise à jour 21 août 2026, nuit)

> **Session pipeline matching bout-en-bout (21 août, soir/nuit)** — suite
> directe de la session UI/UX du même jour (ci-dessous). Cette fois : le
> vrai chemin critique marketplace (publier → matcher → proposer →
> accepter → mission) testé de bout en bout en conditions réelles sur le
> téléphone, plusieurs bugs bloquants réels trouvés et corrigés. Tout
> commité directement sur `dev` (même précédent que le 20 août).
>
> **Bloquants réels corrigés (vérifiés par test, pas juste relus)** :
> - **`service-mkt` ne renseignait jamais les coordonnées d'une demande à
>   la publication** — `Demande.origineLatitude/...` existaient déjà sur
>   l'entité et étaient déjà lus par `publierEvenement()`, mais rien ne
>   les remplissait jamais. Le moteur de matching n'avait donc **aucune**
>   position exploitable pour **aucune** demande, jamais. `AxeResponse`
>   (service-geo) expose désormais les coordonnées réelles des hubs,
>   reprises par service-mkt à la résolution de l'axe (granularité hub,
>   donnée réelle).
> - **Une demande sans coordonnées faisait planter tout le cycle
>   Kuhn-Munkres** (`AffectationL1Service`, NullPointerException) — une
>   seule demande de test incomplète bloquait aussi les demandes valides
>   du même lot. Puis, une fois ce crash corrigé au mauvais endroit
>   (après le solveur), découverte du vrai bug : le solveur pouvait
>   quand même affecter une capacité réelle à cette demande morte (coûts
>   MAT identiques entre candidats, aucun signal pour la disqualifier) —
>   gaspillant la capacité pour tout le cycle. Filtrée en amont
>   (`MatchingCycleService.lotNonVide`), avant le solveur.
> - **`origineNom`/`destinationNom` partaient en dur à `null`** dans
>   `AffectationConfirmeeEvent` → `Mission.origineNom/destinationNom`
>   (service-exe) toujours vides, alors que l'app Chauffeur affichait déjà
>   ces deux champs sans jamais rien avoir à montrer. `AxeActifDto`
>   récupère désormais les noms de villes des hubs, propagés via une
>   surcharge de `calculerAffectationOptimale` (signature existante
>   conservée pour l'endpoint de vérif manuelle + les tests).
> - **Infos marchandise (type/quantité/poids) jamais propagées** au-delà
>   de service-mkt — ni l'app Chauffeur (missions) ni l'app Client
>   (suivi) ne savaient ce qui était réellement transporté. Même
>   principe que le fix précédent, propagé de bout en bout
>   (`DemandePublieeEvent` → `DemandeEnAttente`/`DemandeAvecCandidats`
>   → `AffectationConfirmeeEvent` → `Mission` → DTOs → UI des deux apps).
>   Migration `V18__add_marchandise_demande_en_attente.sql` (service-opt).
> - **`SecurityConfig` bloquait les appels internes AVANT le controller**
>   (`service-cap` `POST /decrement`, `service-flt` `GET /vehicules/{id}`)
>   — les deux controllers savent déjà accepter la clé interne partagée
>   en plus du JWT, mais restaient sous `.anyRequest().authenticated()`
>   côté Spring Security → 403 systématique pour tout appelant sans JWT,
>   *avant même* d'atteindre le code qui gère la clé interne. Piège à
>   revérifier systématiquement pour tout nouvel endpoint interne : un
>   controller qui "accepte la clé interne" ne suffit pas, il faut aussi
>   un `permitAll()` explicite au niveau du filtre Spring Security (même
>   pattern que le `GET` déjà en place à côté).
> - **EF-MKT-08 (réservation réelle à l'acceptation d'une proposition)**
>   construit de zéro — `accepterProposition()` marquait ACCEPTEE en base
>   locale sans jamais réserver la capacité côté transporteur. Nouveau
>   `ServiceCapClient` (service-mkt), pont cross-tenant via clé interne.
> - **`copyWith(champ: null, ...)` ne vide jamais un champ en Dart** —
>   `null ?? ancienneValeur` retombe sur l'ancienne valeur. Repéré sur
>   `SuiviState` (app_client) : ouvrir le suivi d'une demande sans mission
>   affichait le suivi de la DERNIÈRE demande consultée qui, elle, en
>   avait un. Piège générique à surveiller partout où `copyWith` sert à
>   *effacer* un champ plutôt qu'à le laisser inchangé — repartir d'un
>   objet neuf dans ce cas, pas de `copyWith`.
>
> **Pièges opérationnels pour la suite** :
> - **L'IP du laptop a changé en cours de session** (réseau WiFi, pas
>   hotspot fixe cette fois) — les deux apps mobiles ont l'IP baked-in au
>   build (`--dart-define`), donc un changement d'IP casse tout
>   silencieusement ("catalogue indisponible", "erreur réseau") sans
>   rapport avec les données. Vérifier `ip -4 addr show` en cas de
>   symptôme réseau généralisé après un moment d'inactivité/reconnexion.
> - **`service-mat`, `service-opt`, `service-exe` ne sont PAS dans la
>   liste de démarrage "standard" héritée du 20 août** (qui ne couvrait
>   que ida/geo/cap/mkt/gateway) — sans eux, aucun matching ne se
>   déclenche jamais, silencieusement (une demande publiée reste
>   PUBLIEE pour toujours). Les 10 services à démarrer pour un test de
>   bout en bout : ida, geo, cap, mkt, flt, not, exe, opt, mat, gateway.
> - **Une capacité déclarée est "consommée" par le matching dès qu'elle
>   sert à une affectation**, même si son poids résiduel reste énorme —
>   ce n'est pas un pool réutilisable pour plusieurs demandes dans le
>   même cycle. Une nouvelle demande sans nouvelle capacité disponible
>   restera sans proposition indéfiniment (comportement normal, pas un
>   bug) ; la queue traite en priorité la plus ANCIENNE demande en
>   attente dès qu'une capacité se libère, pas la plus récente.
> - **Aucun barème de tarification n'existait en base** (`opt.bareme_tarification`
>   vide) — `TarificationL4Service` tombe en mode dégradé sans ça, aucune
>   affectation ne se termine. Deux barèmes de test insérés manuellement
>   (axe Douala↔Yaoundé, régime `FORFAITAIRE_VEHICULE`, 50000 XAF socle,
>   10% commission) — **données de démo, pas des valeurs métier
>   validées**, à ne pas confondre avec une vraie config Moteur.
> - **10 JVM simultanées (~500-700 Mo chacune malgré `-Xmx256m`, overhead
>   hors-tas) saturent facilement les 7,5 Go de la machine** — l'OOM-killer
>   a tué le démon Gradle à plusieurs reprises pendant les builds mobiles.
>   Toujours couper tout le backend avant un `flutter run`/build, le
>   relancer juste après.
>
> **Non fait, connu et documenté au fil de la session** (pas retesté
> depuis, ne pas supposer résolu) : wizard de publication graduée
> (photo, déclaration N1→N5), fidélité pixel-perfect au mockup pour les
> 2 premiers écrans chauffeur (profil/accueil), vraie carte pour le
> suivi position (actuellement juste "véhicule en mouvement" + horodatage,
> mieux que des coordonnées brutes mais pas une carte), i18n anglais
> (aucune infrastructure de traduction dans le code).

---

# Historique (mise à jour 21 août 2026, matin/après-midi)

> **Session UI/UX mobile (21 août)** — suite directe de la session de
> test du 20 août (Pixel 6a physique, hotspot téléphone). Corrections
> menées sur les deux apps (Client, Chauffeur/Transporteur), commitées
> directement sur `dev` (pas de PR — précédent déjà posé le 20 août pour
> ce type de changement mobile-only, solo, sans autre développeur
> concerné) :
>
> - **Menu latéral (hamburger) à moitié mort** : 5 des 7 liens (Langue,
>   Centre d'aide, Politique & confidentialité, Conditions
>   d'utilisation, Paramètres) avaient `onTap: null` — grisés, ne
>   menaient nulle part, dans les deux apps. 6 nouveaux écrans créés
>   (`aide_screen.dart`, `conditions_utilisation_screen.dart`,
>   `langue_screen.dart`, `parametres_screen.dart`,
>   `politique_confidentialite_screen.dart`, `simple_page_screen.dart`
>   comme gabarit commun) et branchés dans `menu_drawer.dart` des deux
>   apps. Contenu FAQ adapté au rôle (chargeur côté Client,
>   capacités/missions côté Chauffeur) ; Politique/CGU en placeholder
>   raisonnable, pas un vrai texte juridique validé.
> - **Bouton "Enregistrer" mal placé sur l'écran de complétion de
>   profil** (`completer_profil_screen.dart` Client,
>   `kyc_screen.dart` Chauffeur) : il appartenait à la carte "1.
>   Identité", donc affiché **au-dessus** de la carte "2. Pièce
>   d'identité" — déroutant visuellement. Extrait en bouton commun,
>   replacé après les deux étapes (bas d'écran), dans les deux apps.
> - **Fausse alerte "perte de données"** : après un rebuild+reinstall
>   pendant que le backend était encore coupé (pause RAM habituelle
>   avant tout build), l'app Client affichait un état vide (catalogue
>   d'emballages + "Mes demandes"), lu par l'utilisatrice comme une
>   suppression en base. Vérifié directement en Postgres
>   (`service_mkt.demandes`, `service_mkt.catalogue_emballages`) :
>   aucune perte, tout intact. Cause réelle : `DemandeNotifier` charge
>   catalogue + demandes une seule fois à la création du provider, sans
>   retry automatique si l'appel échoue (backend pas encore levé).
>   Ajout d'un état "Catalogue indisponible + bouton Réessayer" dans
>   `publier_demande_screen.dart` pour ne plus reproduire la confusion.
> - **Petits correctifs UX supplémentaires côté Client**
>   (`publier_demande_screen.dart`) : sections du formulaire renommées
>   (Où→Lieu, Quoi→Marchandise, Quand/Comment→Modalités) ; bandeau
>   "prix estimatif" figé remplacé par une notification transitoire ;
>   libellé de quantité désormais dynamique selon le type de
>   marchandise choisi + suffixe d'unité ; téléphone destinataire
>   basculé sur le même sélecteur pays/indicatif (`IntlPhoneField`) que
>   partout ailleurs ; récapitulatif poids/volume enrichi d'un
>   "véhicule adapté" suggéré par palier de poids (camionnette → semi-
>   remorque) pour rendre un chiffre brut en kg compréhensible.
>   `propositions_screen.dart` entièrement redessiné — affichait
>   littéralement `Map.toString()` brut par proposition, remplacé par
>   de vraies cartes (rang, prix, motif de classement, badge de
>   statut).
>
> **⚠️ Piège de commande à connaître** : les deux apps mobiles n'ont
> **pas** le même schéma de variables `--dart-define`. App Client
> attend 5 variables séparées par service
> (`API_BASE_IDA`/`API_BASE_MKT`/`API_BASE_NOT`/`API_BASE_EXE`/
> `API_BASE_FLT`, voir `dio_provider.dart`), App Chauffeur attend une
> **seule** `API_BASE` pointant la gateway (`http://<IP>:8082/api/v1`,
> voir `dio_provider.dart` de cette app). Utiliser le mauvais schéma ne
> fait pas planter le build — l'app compile et se lance avec l'URL par
> défaut (`localhost`), silencieusement inutilisable sur téléphone
> physique. Toujours relire `mobile/*/scripts/run.sh` (source de
> vérité) avant de composer la commande à la main plutôt que de la
> deviner par analogie entre les deux apps.

---

# Historique (mise à jour 20 août 2026, nuit)

> **Session de test mobile de bout en bout (20 août, soir/nuit)** — les
> deux apps mobiles (Client, Chauffeur/Transporteur) ont été installées
> et testées sur un téléphone physique (Pixel 6a). Résultats :
>
> **Fonctionnalité ajoutée** : l'app Client n'avait **aucun écran
> d'upload de pièce justificative KYC** — RG-011 exige identité déclarée
> ET pièce déposée pour atteindre NIVEAU_1 (débloquant "publier une
> demande"), mais `completer_profil_screen.dart` ne gérait que la
> première condition et fermait l'écran en laissant croire le profil
> complet. Ajout d'un écran en 2 étapes (identité, pièce photo via
> `image_picker`, même pattern que l'app Chauffeur) — `kyc_provider.dart`
> et `completer_profil_screen.dart` réécrits en conséquence.
>
> **4 bugs réels trouvés en testant le parcours complet publication
> demande → déclaration capacité → matching** (aucun lié aux correctifs
> d'audit précédents, tous pré-existants) :
> 1. Gateway → service-geo : le gateway (conteneur Docker) tentait de
>    résoudre `service-geo` comme un nom de conteneur Docker — cassé dès
>    que service-geo tourne hors conteneur (process hôte). Symptôme :
>    `DnsErrorCauseException: NXDOMAIN`.
> 2. Secret JWT désynchronisé entre le gateway et service-ida : chacun
>    utilisait sa propre valeur par défaut différente en l'absence de
>    `FRETCORRIDOR_JWT_SECRET` — tout jeton service-ida rejeté par le
>    gateway. (Sans effet sur le flux réel de l'app Chauffeur, qui passe
>    par le login **propre** du gateway `/api/v1/auth/login`, pas
>    `/api/auth/login` de service-ida — mais un vrai risque de
>    régression si les deux chemins se recoupent un jour.)
> 3. `com.fretcorridor.gateway.domain.Role.valueOf(...)` plante
>    (`NullPointerException: Name is null`) si un JWT ne porte pas une
>    claim `role` **singulier** — seul le gateway émet ce format ; un
>    jeton service-ida (claim `roles`, pluriel, liste) fait planter
>    `JwtReactiveAuthenticationManager`. Les deux systèmes d'auth (JWT
>    gateway vs JWT service-ida) ne sont **pas interopérables** — à
>    garder en tête si un flux futur mélange les deux.
> 4. service-opt → service-geo : même bug DNS que le gateway (service-opt
>    aussi en conteneur Docker, résout `service-geo:8084` en interne).
> 5. **Perte silencieuse de capacité déclarée** : `capaciteResiduelleKg`
>    arrivait `null` dans l'événement Kafka `CapaciteDeclaree` reçu par
>    service-opt (violation NOT NULL, capacité jamais matchée) — le code
>    source actuel a pourtant déjà un correctif documenté pour exactement
>    ce bug (18 août). Cause : le **conteneur Docker service-cap tournait
>    une image obsolète**, construite avant ce correctif. Résolu en le
>    relançant en process hôte avec le code `dev` actuel (comme fait pour
>    la plupart des services ce soir, cf. note environnement ci-dessous).
>
> **Note environnement (contexte, pas un bug applicatif)** : la stack
> Docker locale (`docker ps`, containers "Up 2 days") était bâtie sur du
> code d'avant la quasi-totalité des correctifs de la nuit. Plutôt que de
> tout reconstruire (risque disque — les deux partitions de cette machine
> sont proches de la saturation), la majorité des 14 microservices ont
> tourné en process `mvn spring-boot:run` sur l'hôte pendant cette
> session, avec quelques variables d'environnement ajustées à la main
> (`SPRING_DATASOURCE_URL` vers le port hôte 5434 de Postgres, `SERVER_PORT`
> pour éviter un conflit avec le gateway sur 8082 côté service-mkt). Le
> gateway et service-opt (restés en conteneurs) ont été recréés avec des
> variables d'environnement pointant vers `172.18.0.1` (passerelle Docker
> bridge) plutôt que les noms de conteneurs Docker absents. **Ceci est un
> contournement de session de test, pas un changement de configuration
> durable** — à refaire proprement (rebuild Docker complet ou tout en
> process hôte) pour la prochaine session.

---

> Document de suivi/handoff, versionné dans le dépôt à la racine. Remplace
> la version du 20 août (fin d'après-midi) : **les 18 bloquants initiaux
> de `AUDIT_CDC_v4_complet_2026-08-19.md` sont désormais tous traités**
> — 17 résolus sans réserve, le 18e (RG-039) traité avec une limitation
> explicitement documentée (voir ci-dessous et §6). **29 PR** au total
> mergées depuis le début de cette passe (#91 à #124, sauf #111).
>
> **Audit de suivi périmètre Mobile reçu des coéquipiers (dev@727410b)
> — vérifié et traité (PR #124, #125)** : 3 constats confirmés réels et
> corrigés — secret JWT non paramétrable (en réalité 5 services
> concernés, pas seulement service-ida comme rapporté) ; refresh token
> contournant le verrouillage de compte (`AuthService.rafraichir()`) ;
> `GET /api/cap/capacites/{id}` public sans aucune vérification (clé
> interne partagée service-not↔service-cap, PR #125 — décision explicite
> de l'utilisateur de le corriger plutôt que de le laisser comme
> service-geo/mat/opt/trk, vérifié conforme CDC/Plan d'Exécution §4.3
> avant de coder). **3 points de ce rapport sont faux/obsolètes** (RG-101,
> RG-070, endpoint véhicule service-flt donnés "toujours ouverts") —
> leur audit a été fait sur un commit ~12 merges avant mes correctifs
> du soir, pas une erreur de leur part. Détail complet §6.
>
> **RG-070 (preuve de livraison) est maintenant fermé de bout en bout** :
> backend (photo + signature tactile, PR #118) **et UI mobile app
> Chauffeur** (écran de capture, PR #122) — le parcours Phase 1→Phase 2
> est de nouveau testable normalement sur téléphone, prise en charge et
> livraison demandent désormais une photo + signature avant de valider.
> Le code SMS (autre mode de validation tiers prévu par le CDC) reste
> hors périmètre — le backend ne le supporte pas (numéro du destinataire
> non propagé jusqu'à service-exe).
>
> **Seule limitation restante, à connaître** : **RG-039** (jusqu'à 3
> propositions ordonnées, PR #120) — rang 2/3 ajoutés (informationnels,
> prix estimé) sans toucher au rang 1 existant. L'endpoint "accepter"
> (service-mkt) marque la proposition choisie, mais **ne déclenche pas
> la réservation réelle de capacité** (`decrementer()` exige le même
> tenant que le transporteur propriétaire — un chargeur d'un autre
> tenant ne peut pas l'appeler sans un pont de confiance cross-tenant
> qui n'existe pas encore).
>
> Cette limitation a été **décidée explicitement avec l'utilisateur**
> après découverte de son ampleur réelle en cours de route (pas un
> oubli) — voir §6 pour le détail complet et les échanges avec le
> collègue Moteur.
>
> **⚠️ Incident corrigé ce soir** : un merge de branche Moteur
> (`backend-stevetelecom`, commit `c564100` "fusion termine") a laissé
> des **marqueurs de conflit Git non résolus commités directement sur
> `dev`** dans ce fichier — corrigé immédiatement en gardant la version
> la plus à jour (celle-ci). Si un autre fichier affiche un
> comportement bizarre après un merge Moteur récent, vérifier d'abord
> l'absence de `<<<<<<<`/`=======`/`>>>>>>>` avant de chercher plus
> loin.

---

## Prérequis avant toute intégration réelle (S11 à S19)

**Tout sprint mobile de S11 à S19 (Phase 2 + Phase 3) reste en mode
mocké** tant que le backend réel visé n'est pas confirmé prêt côté
Moteur/Web pour le sprint concerné.

Le test bout-en-bout Docker du fix S7 (condition qui figurait ici au
17 août) **a été fait, en dehors d'une session Claude Code** — voir §5.1
pour le résultat détaillé. Ne plus le traiter comme "en attente".

**Exceptions actées — toute la Phase 2 en fait partie désormais** :
- **S15** (sélecteur d'axe, Chauffeur + Client) — `service-geo` confirmé
  prêt par le Moteur (2 axes réels en base, filtrage réel), câblé avec
  accord explicite de l'utilisateur.
- **S11** (tournée multi-étapes, Chauffeur) — le Moteur a construit et
  testé `TourneeConstituee` (20 août), câblé de bout en bout côté Mobile
  le jour même. Voir §5.4 pour le détail (solution différente de ce qui
  était envisagé le 18 août).
- **S12** (retour à vide, Chauffeur) — déjà réel depuis le 18 août, mais
  un gap silencieux (`etape-executee` sans producteur) empêchait le
  déclenchement effectif ; corrigé le 20 août (PR #85). Voir §5.2.
- **S14** (moyen de paiement, Chauffeur + Client) — backend Item B livré
  par Web le 18 août, gateway + mobile câblés le 20 (PR #87). Espèces
  reste une exception assumée (confirmation locale, jamais envoyée au
  backend) — voir §5.5, pas un oubli.

Ce n'est pas un modèle à reproduire automatiquement pour les **prochains**
sprints (S16 à S19, Phase 3) — ne sortir un sprint du mock que sur
demande explicite, après confirmation du backend concerné.

---

## Règle absolue (Git) — à respecter dans toute session

**INTERDICTION FORMELLE de `git merge`, `git push origin dev`, ou toute
action qui modifie directement `dev`.** Toujours : créer une branche →
committer dessus → pousser cette branche → s'arrêter et attendre que
l'utilisateur crée et fasse fusionner la Pull Request lui-même sur GitHub.
Si une tâche semble nécessiter de fusionner dans `dev`, demander
confirmation explicite et attendre une réponse contenant le mot **PULL
REQUEST** avant toute action de merge (ouvrir la PR via `gh pr create` est
en revanche acceptable une fois ce mot reçu).

**⚠️ Pattern récurrent à surveiller (pas un incident isolé)** : le Moteur
(`stevetelecom`) a commité directement sur `dev` sans PR **trois fois** :
`384f168` et `ef71786` (14 août), `a640efe` (17 août — fix sérialisation
Kafka + `proposition-retour-a-vide.yaml`). Signalé fermement par
l'utilisateur après la 3e fois. Ne pas traiter comme un oubli ponctuel :
si un futur changement côté Moteur apparaît dans `dev` sans commit de
merge de PR associé, c'est probablement lui — le signaler explicitement
à l'utilisateur plutôt que de le documenter comme un merge normal.

**Protocole confirmé en usage (20 août)** : branche `backend-stevetelecom`
poussée en PR (#81) plutôt que commitée directement cette fois — bon
signe. Le mot **PULL REQUEST** a été demandé et reçu avant chaque merge
(#81 à #104, plus de 20 PR consécutives au total sur la journée). Sur
#87, `gh pr checks` a d'abord montré des checks `pending` (gateway) :
attendre qu'ils passent avant de merger plutôt que de merger sur un
statut incomplet — quelques cycles de sondage (~10s) ont suffi.

**`gh pr merge` bloqué par le classificateur auto-mode — résolu (20
août, nuit)** : la commande était refusée par le classificateur malgré
le mot PULL REQUEST reçu. Cause : aucune règle de permission Bash
explicite pour `gh pr merge`/`gh pr create`/`gh pr checks` dans
`.claude/settings.local.json`. **Corrigé en ajoutant ces trois patterns
à `permissions.allow`** (avec l'accord explicite de l'utilisateur, qui a
choisi cette option plutôt que la fusion manuelle) — `gh pr merge`
fonctionne normalement depuis. Si le blocage réapparaît malgré la règle
présente, c'est un signal différent (mode d'exécution de la session, pas
un problème de settings) — rendre la main à l'utilisateur dans ce cas
précis, jamais chercher à contourner autrement.

**Fusions de PR dans la même branche : conflits attendus, pas une
anomalie.** Sur cette session, plusieurs correctifs indépendants
touchaient les mêmes fichiers (ex. `AdmPort`/`PayReadPort` côté gateway,
modifiés par PR #98, #100, #101, #102 quasi simultanément) — chaque
`gh pr merge` suivant a produit `GraphQL: Pull Request has merge
conflicts`. Résolution systématique : `git fetch origin <branche
source>`, checkout de cette branche, `git merge origin/dev`, résoudre
(généralement additif — combiner les deux signatures de méthode élargies
plutôt que choisir un côté), recompiler + retester le module concerné,
push, puis `gh pr merge` repasse.

**Contention de ressources entre suites Testcontainers lancées en
parallèle** : deux `mvn test` simultanés (ex. gateway + service-bur, deux
suites Testcontainers/PostgreSQL) ont produit des échecs qui
ressemblaient à de vraies régressions (timeouts 5s, crash de fork JVM
"VM terminated without properly saying goodbye"). Confirmé non-lié en
réexécutant chaque suite **séquentiellement** — toujours revalider
isolément avant de conclure à une régression quand des tests tournent en
parallèle dans le même sandbox.

---

## 1. Objectif du projet

FretCorridor v4 est une marketplace numérique de fret et de colis pour le
Cameroun et la zone CEMAC (Flysoft Engineering SAS). Trois volets :

- **App mobile Client** — publier une demande de transport, suivre sa
  livraison, payer
- **App mobile Chauffeur/Transporteur** — s'authentifier, déclarer sa
  capacité disponible, exécuter des missions, être payé
- **Portail Web** — Bureau de fret (supervision), Transporteur (gestion
  flotte), Administration (KYC, configuration, audit)

Architecture microservices Spring Boot (Java 17), une gateway API en
entrée pour les rôles Web (BUREAU/TRANSPORTEUR/ADMIN — **pas** l'app
Client, cf §2), communication inter-services par Kafka, PostgreSQL/PostGIS,
Redis, MinIO. Phase 1 = Sprints 1 à 10. Phase 2 = Sprints 11, 12, 14, 15
(S13 backend/Web uniquement, rien côté mobile).

---

## 2. État actuel du projet (branche `dev`)

### Back-end — tous les microservices existent et sont avancés

| Service | Rôle | Porteur | État |
|---|---|---|---|
| `service-ida` | Identité, KYC, RBAC | Mobile | ✅ |
| `service-mkt` | Marketplace (demandes client) | Mobile | ✅ Pipeline Kafka complet — `DemandePubliee` publié, `proposition-emise` consommé (voir §3, ancienne "tâche concrète" du 7 août, **résolue**) |
| `service-flt` | Positions GPS | Mobile | ✅ Fix "exempte lookup véhicule interne" mergé (PR #58, débloque S7) |
| `service-exe` | Exécution de mission | Mobile | ✅ Consomme `AffectationConfirmee` + `TourneeConstituee` (20 août), expose `GET /missions/mes`, `POST /missions/{id}/etapes`, `GET /missions/tournees/{id}`, publie `MissionLivree` + `EtapeExecutee` (20 août) |
| `service-not` | Notifications in-app | Mobile | ✅ |
| `service-cap` | Capacité (déclaration véhicule) | Mobile | ✅ Dockerfile + docker-compose ajoutés (n'existaient pas au 7 août) |
| `service-geo` | Axes, zonage H3 | Moteur | ✅ `GET /api/geo/axes?tenantId=` filtre réellement en base (ENF-MUL-01, correctif du 2026-08-09) — **2 axes réels actifs en base** (Douala-Yaoundé, Douala-Bafoussam) |
| `service-mat` | Coût composite matching | Moteur | ✅ Avancé |
| `service-opt` | Moteur de matching + séquencement tournées | Moteur | ✅ Avancé — Kuhn-Munkres, + séquencement ALNS (Sprint 11/12), consomme `EtapeExecutee` (producteur réel côté `service-exe` depuis le 20 août), publie `PropositionRetourAVideEvent`, `TourneeConstituee` (S11, 20 août), oracle chargement S16 |
| `service-trk` | Suivi/ETA temps réel | Moteur | ✅ |
| `gateway` | Point d'entrée Web (Bureau/Transporteur/Admin) | Web | ✅ Architecture hexagonale, 2 fixes de ports mergés (service-adm, service-pay — PR #59) |
| `service-pay` | Paiement, grand livre miroir | Web | ✅ `ModePaiementChoisi` (S14 Item B, 18 août) : `POST`/`GET .../moyen-paiement`, proxifié par la gateway (Chauffeur) et appelé directement par l'app Client (20 août, voir §5.5) |
| `service-bur` | Agrégation Bureau (missions) | Web | ✅ |
| `service-adm` | Back-office (KYC, tenants, config, audit) | Web | ✅ |

### Mobile — les deux apps sont maintenant sur `dev`

- **App Client** (`mobile/app_client/`) — Phase 1 (S1-S9) + Phase 2 (S11
  Volet B, S12 — rien pour Client ce sprint, S14 Volet B, S15 Volet B)
  intégrés dans `dev`. **Pas de gateway unifiée pour ce rôle** : chaque
  écran appelle directement le microservice concerné (auth/KYC →
  service-ida 8081, demandes → service-mkt 8089, notifications →
  service-not 8094, chronologie/position → service-exe 8093 / service-flt
  8092). `lib/providers/dio_provider.dart` expose désormais 5 clients Dio
  nommés (un par service, réapplication du correctif `9c52f02` — PR #64,
  mergée le 17 août) — le `dioProvider` unique/mauvais port n'existe plus.
- **App Chauffeur/Transporteur** (`mobile/app_chauffeur_transporteur/`) —
  Phase 1 (S1-S10) + Phase 2 (S11 Volet A, S12, S14 Volet A, S15 Volet A)
  intégrés dans `dev`. Passe par la gateway (port 8082, chemins
  `/api/v1/...`).

### Web (Angular)

Portails Bureau, Transporteur, Admin — largement construits, tests
unitaires et e2e (Playwright). Pas revérifié depuis le 7 août, pas de
raison de penser que ça a régressé.

---

## 3. Phase 2 (Sprints 11, 12, 14, 15) — état détaillé

Toute la Phase 2 est mergée dans `dev` :

| Sprint | Chauffeur/Transporteur | Client |
|---|---|---|
| S11 — Consolidation LTL | ✅ **branché sur le vrai backend** (PR #81/#82/#84 — 20 août) | ✅ **branché sur le vrai backend** (PR #89 — 20 août) |
| S12 — Retour à vide | ✅ **branché sur le vrai backend, chaîne complète** (PR #76/#77 — 18 août, gap `etape-executee` comblé PR #85 — 20 août) | — (rien pour Client) |
| S13 | — (backend/Web uniquement) | — |
| S14 — Paiement Mobile Money | ✅ **branché sur le vrai backend** (Item B 18 août, gateway+mobile PR #87 — 20 août) | ✅ **branché sur le vrai backend pour MoMo/Orange Money** (PR #87) — Espèces reste local, exception assumée (§5.5) |
| S15 — Second axe | ✅ **branché sur le vrai backend** (PR #56 puis re-câblage réel PR #60) | ✅ **branché sur le vrai backend** (PR #57 puis re-câblage réel PR #61) |

**Toute la Phase 2 (S11, S12, S14, S15) est réelle de bout en bout,
Chauffeur et Client, depuis le 20 août au soir.** Le dernier volet mocké
(S11 Volet B, indicateur "envoi consolidé" côté Client) a fermé sans
aucun changement backend : `tourneeId` était déjà exposé par
`service-exe` sur l'endpoint chronologie du Client depuis la PR #82,
juste jamais lu côté app (PR #89). Seule exception assumée restante :
Espèces (S14 Client) confirmé localement, jamais envoyé au backend — un
choix de conception documenté en §5.5, pas un gap.

---

## 4. Contrats Kafka S11/S12 — mergés dans `shared-contracts/`, deux encore en BROUILLON

- **`shared-contracts/asyncapi/events/etape-executee.yaml`** — toujours
  marqué **BROUILLON** dans le fichier lui-même, mais **le producteur
  existe et fonctionne désormais** (`service-exe`, PR #85, 20 août) :
  publié à chaque `PRISE_EN_CHARGE`/`LIVRAISON` confirmée par le
  chauffeur. `missionId` = `Affectation.id`, confirmé. Le statut BROUILLON
  du fichier n'a donc plus valeur de blocage — implémentation faite en
  écrivant volontairement une copie locale du contrat côté `service-exe`
  (tolérante à une évolution ultérieure), pas en important le fichier.
- **`shared-contracts/asyncapi/events/tournee-constituee.yaml`** (nouveau,
  20 août, PR #81) — même statut BROUILLON assumé, même approche : copie
  locale côté `service-exe`/gateway, pas d'import direct. Publié par
  `service-opt` uniquement pour une Tournée LTL consolidée.
- **`shared-contracts/asyncapi/events/proposition-retour-a-vide.yaml`** —
  résolu et mergé le 18 août : `tourneeId`/`affectationId` nullable et
  mutuellement exclusifs, couvre FTL simple et LTL consolidé. Version
  1.0.0, plus un brouillon.
- **Bug de sérialisation transversal (dates OPT en epoch flottant)** —
  trouvé et corrigé le 17 août (`JavaTimeModule` + `WRITE_DATES_AS_TIMESTAMPS`
  désactivé, `service-opt`). Sans impact réel côté Mobile.

**Aucun contrat ne bloque plus S11/S12 aujourd'hui** — les deux
BROUILLON (`etape-executee`, `tournee-constituee`) ont un producteur et
un consommateur réels et fonctionnels malgré leur statut de fichier ; à
faire valider formellement au prochain point de synchro hebdo Moteur/Mobile,
sans urgence bloquante.

---

## 5. Ce qui reste ouvert

### 5.1 Test S7 en Docker — FAIT (18 août), résultat détaillé

**Ne plus traiter ce point comme "en attente"** — le test bout-en-bout
réel (déclaration de capacité via la gateway, deux essais successifs) a
été effectué en dehors d'une session Claude Code. Résultat :

- **Fix de port confirmé fonctionnel** (8092→8083,
  `service-cap/application-docker.yml`) : `transporteur_id` se peuple
  bien en base côté `service-cap` une fois le pool de connexions
  `ServiceFltClient` chaud. Le tout premier appel à froid après un
  `docker-compose up` échoue en timeout (dégradation gracieuse comme
  prévu, ENF-DIS-04) — **corrigé** en allongeant les timeouts
  (`connectTimeoutMs` 200→500, `readTimeoutMs` 300→1000,
  `ServiceFltClientProperties`, PR #71, mergée).
- **Nouveau bug trouvé en poussant le test jusqu'à `service-opt`** :
  `CapaciteDeclareeEvent` a divergé entre les deux copies du contrat.
  La copie côté `service-opt` a deux champs de plus
  (`capaciteResiduelleKg`, `volumeResiduelM3`, ajoutés pour le
  séquencement L2 Phase 2 / EF-CAP-07) que `service-cap` ne publie
  jamais. Conséquence : ces champs arrivent `null` côté `service-opt`,
  violent la contrainte `NOT NULL` en base
  (`capacite_en_attente.capacite_residuelle_kg`, migration V10) et font
  échouer l'insertion à **chaque** déclaration de capacité — y compris le
  flux Phase 1 basique, pas seulement Phase 2. Vérifié dans le code :
  `CapaciteDeclareeListener.ingerer()` (service-opt) attrape
  `DataIntegrityViolationException` de façon trop large et logue
  "CapaciteDeclaree deja ingeree, doublon ignore" — **la capacité est
  donc perdue silencieusement**, sans marquage d'erreur distinct pour la
  retrouver (le message de log est trompeur : ce n'est pas un doublon).
  **Décision du Moteur (18 août)** : fix côté `service-cap` (le Moteur a
  un empêchement réseau) plutôt que nullable côté `service-opt`. **Fait et
  mergé** — `capaciteResiduelleKg`/`volumeResiduelM3` ajoutés à
  `CapaciteDeclareeEvent` (service-cap), valeurs prises sur
  `Capacite.getCapaciteResiduelleKg()`/`getVolumeM3()` (PR #73, mergée).
  Revalidation Docker bout-en-bout abandonnée (réseau trop instable
  pendant la session) — repose sur compilation + tests unitaires
  uniquement pour l'instant.

### 5.2 S12 réel (Chauffeur) — FAIT (18 août), chaîne complétée (20 août)

`proposition-retour-a-vide.yaml` corrigé et mergé, backend construit de
zéro (`service-not` : première consommation Kafka de ce service,
écouteur + résolution transporteur via nouveau `GET
/api/cap/capacites/{id}` côté `service-cap` — PR #76), câblage Flutter +
route gateway (PR #77). **La réponse accepter/refuser reste locale à
`service-not`** : aucun contrat n'existe pour la relayer au Moteur à ce
jour — à revoir si besoin plus tard.

**Gap trouvé par l'audit CDC du 19 août, corrigé le 20 (PR #85)** : toute
cette chaîne (`service-opt` → `service-not` → Mobile) était réelle et
mergée, mais **jamais déclenchée en pratique**. `EtapeExecuteeListener`
(`service-opt`) attend l'événement `etape-executee` pour figer l'exécuté
(EF-MAT-09) et appeler `proposerRetourAVide` — **aucun producteur
n'existait nulle part dans le dépôt**. `service-exe.MissionService.ajouterEtape()`
publie désormais cet événement à chaque `PRISE_EN_CHARGE`
(→`ENLEVEMENT`)/`LIVRAISON` confirmée. La leçon : un "branché sur le vrai
backend" mergé et testé unitairement peut quand même rester mort en
pratique si un maillon Kafka intermédiaire n'a pas de producteur —
vérifier la chaîne complète, pas seulement chaque bout séparément.

### 5.3 Test Docker bout-en-bout — bloqué (réseau, pas le code)

Tentatives répétées de reconstruire `service-cap` en Docker pour
revalider le fix §5.1 : builds bloqués à répétition (dépendances Maven
téléchargées from scratch dans le conteneur, pas de cache `~/.m2` de
l'hôte — contrairement à `mvn compile`/`mvn test` en local qui
fonctionnent instantanément). Abandonné après plusieurs tentatives sur
plusieurs dizaines de minutes. **Pas un problème de code** — juste pas
revalidé en conditions Docker réelles depuis.

### 5.4 S11 réel (Chauffeur) — FAIT (20 août)

Le blocage du 18 août (aucune donnée de tournée exposée par
`service-opt`) est levé, mais **pas par la voie envisagée à l'époque**
(`tourneeId`/liste d'étapes portée par `AffectationConfirmeeEvent`) — le
Moteur a tranché pour un **événement séparé**, `TourneeConstituee`,
publié uniquement quand une Tournée LTL consolidée est confirmée (jamais
pour une affectation FTL simple, qui reste entièrement décrite par son
propre `AffectationConfirmeeEvent`). `missionId` par étape = même UUID
qu'`AffectationConfirmeeEvent.missionId` — clé de corrélation avec les
Missions déjà créées côté `service-exe`.

Écart de modélisation assumé par le Moteur (à noter, pas à corriger) :
le CDC §13 prévoit une Mission unique portant plusieurs Étapes liées à
plusieurs Demandes ; côté implémentation réelle, chaque Affectation
génère sa propre Mission — `TourneeConstituee` les regroupe *a
posteriori* sous un `tourneeId` commun plutôt que de fusionner les
entités Mission. Le Moteur suggère qu'un ADR documente ce choix côté
`service-exe` si jugé structurant — pas fait à ce jour.

**Chaîne complète, mergée le 20 août** :
- `service-opt` publie `TourneeConstituee` (PR #81).
- `service-exe` la consomme (`TourneeConstitueeListener`), persiste
  l'ordre planifié (`EtapeTournee`), rattache les Missions à leur
  `tourneeId`, expose `GET /missions/tournees/{tourneeId}` (PR #82).
- La gateway proxifie vers `GET /api/v1/missions/tournees/{tourneeId}`
  (PR #82).
- L'app Chauffeur consomme cet endpoint réel, navigation depuis "Mes
  missions" (bandeau sur toute mission dont `tourneeId != null`) — plus
  d'écran "démo" (PR #84).

Contrat `tournee-constituee.yaml` toujours marqué BROUILLON dans le
fichier — voir §4, pas bloquant en pratique.

### 5.5 S14 réel (Chauffeur + Client) — FAIT (20 août)

Le blocage du 18 août ("le concept moyen de paiement n'existe nulle
part dans le domaine service-pay") avait été levé côté Web dès le 18 :
Item B (`docs/DEPENDANCES_MOBILE_PHASE4.md`) livré par Personne 2 —
`ModePaiementChoisi` (un choix par mission, "moyen choisi/prévu" distinct
du "moyen effectivement encaissé", volontairement non recoupés),
`POST`/`GET /api/v1/pay/missions/{id}/moyen-paiement`. Gateway et mobile
sont restés mockés jusqu'au 20 août (PR #87) — ce qui suit décrit ce lot.

**Gateway (Chauffeur, lecture seule)** : `PayReadPort.modePaiementChoisi()`
+ `ServicePayWebClientAdapter`, `GET /api/v1/paiement/missions/{id}/moyen-paiement`
(`PaiementReadController`) — ouvert à tout acteur authentifié (même
raisonnement que `/api/v1/paiement` déjà existant, sinon `CHAUFFEUR`
serait exclu). 404 (rien choisi) mappé sur `MissionIntrouvableException`,
déjà géré globalement.

**App Client (écrit)** : **aucune route gateway** — l'app Client appelle
`service-pay` directement (port 8088), même principe architectural que
tous ses autres providers (`dio_provider.dart` documente l'absence de
gateway unifiée pour le rôle Chargeur depuis le S6/S7). Nouveau
`payDioProvider`.

**Décision de conception notable, à connaître avant de toucher à cet
écran** : MoMo et Orange Money envoient tous deux `MONNAIE_ELECTRONIQUE`
(seule granularité connue de `ModePaiement`, 4 valeurs). **Espèces
n'appelle jamais le backend** — confirmation purement locale. Ce n'est
pas un oubli : le commentaire du commit service-pay Item B est explicite,
"espèces (EF-PAY-07) explicitement hors périmètre — mode dégradé décidé
à l'enlèvement, jamais choisi en amont dans l'app". Le mélange
réel/local sur un même écran est donc voulu, pas un bug à corriger.

**Écrans mobiles rattachés à une vraie mission** (fini le mode "démo"
sans contexte, même principe que S11/§5.4) : `PaiementScreen` (Client)
prend désormais un `missionId`, accessible depuis "Suivi"
(`suivi_screen.dart`, à côté de "Signaler un litige") — l'entrée
générique de l'accueil a été retirée. Côté Chauffeur, l'écran solde/gains
affiche les 4 valeurs réelles de `ModePaiement` (pas de distinction
MoMo/Orange Money, contrairement à ce que supposait le mock qu'il
remplace) ; `lib/mock/moyen_reglement_mock.dart` supprimé.

### 5.6 S11 Volet B (Client) — FAIT (20 août), zéro changement backend

Dernier volet mocké de toute la Phase 2. `tourneeId` (nullable) était en
fait déjà exposé sur `GET /missions/demande/{demandeId}/chronologie` —
le même champ ajouté à `ChronologieResponse` côté `service-exe` pour le
Volet A Chauffeur (PR #82), qui se trouve être le même endpoint que
consomme déjà l'écran de suivi Client pour sa propre chronologie. Personne
ne l'avait remarqué avant de vérifier : `ChronologieModel` (Client) ne
lisait simplement pas ce champ. Ajouté (`chronologie_model.dart`), le
bandeau "envoi groupé" (`suivi_screen.dart`) se base maintenant dessus au
lieu du mock déterministe sur `missionId.hashCode`. `lib/mock/consolidation_mock.dart`
supprimé (PR #89).

---

## 6. Suivi de l'audit CDC du 19 août — ce qui a été corrigé, ce qui reste ouvert

`AUDIT_CDC_v4_complet_2026-08-19.md` (racine du dépôt) reste la référence
complète : 18 bloquants, 35 majeurs, 29 mineurs, ~50 % de conformité CDC
globale au moment de sa rédaction. **Une passe de correction systématique
a eu lieu dans la nuit puis le matin du 20 août**, à la demande explicite
de l'utilisateur ("corrige les bugs bloquants restants de l'audit"),
service par service, dans l'ordre le plus sûr (mécaniques d'abord, puis
authentification, puis confiance mal placée dans le corps de requête).
**19 PR** (#91 à #110, plus #112 — #111 n'est pas de cette session) ont
chacune suivi la discipline habituelle : branche dédiée, compilation +
tests avant commit, mot **PULL REQUEST** explicite avant merge.

### Corrigés et mergés dans `dev` (bloquants/majeurs mécaniques + authentification, PR #91→#105)

| # | Service(s) | Correctif |
|---|---|---|
| #91 | `service-not` | IDOR sur `/notifications/{id}/lue` et `/repondre` — vérification tenant ajoutée |
| #92 | `service-ida` | Activation d'enrôlement agent rendue accessible (`permitAll` explicite sur l'endpoint, sans compte encore créé) |
| #93 | `service-cap` | Poids taxable — 3e terme LDM ajouté (RG-100) |
| #94 | `service-cap` | Perte d'écriture sous concurrence (EF-CAP-07) — **bug trouvé en cours de route, hors liste initiale de l'audit** ; savepoint JDBC natif remplace `REQUIRES_NEW` (qui commitait l'idempotency-log même quand le decrement échouait) |
| #95 | `service-cap`, `gateway` | Authentification JWT + IDOR sur `POST /decrement` ; `tenantId` ajouté à `Capacite` (migration V3) ; gateway transmet désormais le `delegationToken` |
| #96 | `service-exe` | Précédence des étapes imposée (RG-062/070) — `LIVRAISON` sans `PRISE_EN_CHARGE` préalable désormais rejetée (`ETAPE_HORS_SEQUENCE`) ; un test existant qui codifiait le bug a été corrigé, pas juste complété |
| #97 | `service-flt` | Contrainte d'unicité sur l'immatriculation (RG-088) |
| #98 | `service-mkt`, `service-geo` | Pipeline marketplace → matching mort (§1.2) — `DemandeService.publier()` ne renseignait jamais `axeId`/`valeursCriteres` ; nouveau `ServiceGeoClient` résout l'axe par nom de ville. **Côté `service-geo` (Java 21, Moteur) non compilé localement** (sandbox limité à Java 17, voir note ci-dessous) — revu manuellement champ par champ |
| #99 | `service-exe`, `service-flt`, `service-not` | Canaux Kafka morts `position-brute`/`alerte-ecart` (§7.1) fermés — `Mission.vehiculeId` ajouté (peuplé depuis `AffectationConfirmeeEvent`, jamais persisté avant), `service-flt` publie désormais `position-brute`, `service-not` consomme `alerte-ecart` |
| #100 | `service-pay`, `gateway` | Authentification JWT — analyse des appelants réels faite **avant** implémentation (discutée avec l'utilisateur, service financier sensible) : seul `/moyen-paiement` avait un vrai appelant (app Client, envoie déjà un token) ; `/webhooks/**` reste `permitAll` (signature HMAC, pas de JWT possible côté prestataire externe) |
| #101 | `service-adm`, `gateway` | Authentification JWT — dernier des « 8 services sans authentification » côté Mobile/Moteur/Web restants après #95/#100 |
| #102 | `service-bur`, `gateway` | Authentification JWT — dernier des 4 services **Web** (gateway/pay/adm/bur) sans authentification. `OptPort`/`TrkPort` (gateway) transmettent désormais le token vers `service-bur`, qu'ils appellent en réalité malgré leur nom (`ServiceBurMissionAppparieeAdapter`/`ServiceBurPositionAdapter`) |
| #103 | `service-adm` | IDOR sur `GET /api/v1/dossiers/{id}` (§7.2) — `FileTravailService.consulter()` vérifie désormais le tenant, même exception "introuvable" pour les deux cas |
| #104 | `app_chauffeur_transporteur` | File locale pour le suivi GPS hors ligne — même patron que l'enrôlement agent (`flutter_secure_storage` + retry à la reconnexion) |
| #105 | `web` (Angular) | `environment.development.ts` pointait vers `service-pay` (8088) au lieu de la gateway (8082) (majeur, §4 de l'audit) — poussée après une coupure réseau locale (`Destination Net Unreachable`, pas un problème GitHub) |

**Bonus incident, PR #94** : découverte en cours de route, pas dans la
liste initiale de l'audit — l'utilisateur, consulté explicitement, a
choisi de la traiter immédiatement plutôt que de la différer.

### `tenantId`/`acteurId` lus du corps de requête plutôt que du JWT — traité (matin du 20 août, PR #107→#110)

Point laissé explicitement ouvert la nuit précédente (voir version
antérieure de ce document) : l'authentification (#100-#102) vérifiait
qu'un JWT valide était présent, mais pas que le `tenantId`/`acteurId`
porté par le corps de la requête correspondait bien à celui du JWT — un
acteur authentifié pouvait agir pour n'importe quel tenant en le
choisissant simplement dans le payload. Traité à la demande explicite de
l'utilisateur ("attaque le tenantId lu du corps de requête").

| # | Service(s) | Correctif |
|---|---|---|
| #107 | `service-pay` | `PaiementController` (cloture, confirmerLivraison, souscrireGarantie, choisirModePaiement, declarerPaiementEspeces, reversement) : tenantId/acteurId extraits du JWT. `rapportTenant`/`paiementsEspecesTenant`/`ecrituresTransporteur` : IDOR corrigé (403 `AccesRefuseException` cross-tenant, override `ADMINISTRATION` conservé, même principe que `rapportFinancierAdmin` déjà existant) |
| #108 | `service-adm` | `DossierController` (ouvrir/prise-en-charge/decision) + `TenantController.creer` : même traitement. `fileDeTravail` (GET) : IDOR corrigé (403 cross-tenant, override `ADMINISTRATION`) — comble aussi le point "IDOR sur les endpoints de mutation service-adm" resté ouvert la nuit précédente |
| #109 | `service-adm` | `JournalAuditController` : comble le point "export du journal d'audit cross-tenant si tenantId omis" resté ouvert la nuit précédente — sans `ADMINISTRATION`, tenantId omis retombe désormais sur celui du JWT (plus "tous les tenants") ; tenantId différent du JWT → 403 |
| #110 | `service-bur` | **Constat étendu, hors des 18 bloquants de l'audit initial** : en vérifiant systématiquement les autres services pour la même classe de bug, les 4 contrôleurs REST internes (`AlerteSeuilController`, `MissionAppparieeController`, `PositionController`, `BureauAgregatController`) se sont révélés avoir le même problème, sous une docstring trompeuse ("pas de RBAC ici, le gateway filtre déjà par tenant") — le `SecurityConfig` exigeait bien un JWT valide, mais ne vérifiait jamais que son tenantId correspondait à celui du corps/query. Même traitement (tenantId/acteurId du JWT). Le gateway n'a pas eu besoin d'être modifié (il calculait déjà les bonnes valeurs, juste envoyées à un endroit qui ne les vérifiait pas) |

Pattern de fix commun aux 4 PR : extraction via
`@RequestHeader("Authorization") String authHeader` +
`jwtService.extraireTenantId(...)`/`extraireActeurId(...)`, champ
correspondant retiré du DTO de requête (Jackson/Spring ignorent
silencieusement un champ ou paramètre inconnu — vérifié empiriquement,
aucun appelant réel cassé). Tests d'intégration : pattern
`token(tenantId)` / `token(tenantId, acteurId)` (JWT dont le claim
correspond à ce que le test doit vérifier), répliqué identiquement sur
les 4 PR.

### Derniers correctifs de l'après-midi (PR #114, #115)

| # | Service(s) | Correctif |
|---|---|---|
| #114 | `gateway` | ENF-SEC-02 : `DossierController.consolide()` ne journalisait aucune consultation de dossier. `admPort.enregistrerAudit(tenantId, acteurId, "CONSULTATION_DOSSIER_DETAIL", "dossier:"+dossierId, ...)` appelé avant la lecture, même pattern exact que `MissionAppparieeController.detail()`/`PaiementReadController` |
| #115 | `service-flt`, `service-cap` | Bloquant audit §3 "endpoint véhicule public, sans filtre tenant" : `GET /api/flt/vehicules/{id}` était `permitAll()` sans aucune vérification (n'importe qui pouvait lire n'importe quel véhicule de n'importe quel tenant). `service-cap` (`ServiceFltClient`, seul appelant légitime, jamais via le gateway) transmet désormais son propre JWT ; `VehiculeController.consulter` filtre sur le tenantId du JWT (404 si mismatch, même principe que `DossierController` service-adm) |

Ces deux PR n'ont aucune CI (service-flt/service-cap et gateway sur ce
chemin ne sont pas dans le scope `backend-web-scope.yml` pour flt/cap —
vérifié localement par `mvn test` sur les modules concernés avant merge).

### RG-101 — coefficient volumétrique par tenant/axe (PR #117)

**3e bloquant manqué dans le décompte initial** (18 au total, seulement
16-17 nommément détaillés dans les versions précédentes de ce document) :
"Coefficient volumétrique global, non scopé tenant/axe" (§5.2 de
l'audit) — `CalculateurPoidsTaxable` (RHO/LAMBDA) lisait une seule
valeur dans `application.yml` pour tout le système. Distinct du bug
RG-100 déjà corrigé en PR #93 (3e terme LDM manquant, même fichier).

Fix : nouveau `ServiceGeoClient` côté service-cap résout
`Axe.parametres` (clés `coefficientVolumetriqueKgParM3`/
`coefficientPlancherKgParLdm`, même mécanisme que
`detourMaxDistanceKm`/EF-MAT-10) — un axe appartenant à exactement un
tenant, scoper par axe scope aussi par tenant. Repli sur la référence
globale (`application.yml`) si absent/injoignable (ENF-DIS-04).
Nouveau `CalculateurPoidsTaxableTest` (aucun test dédié n'existait
avant). Vérifié : `mvn -o test` service-cap, 6 tests, 0 échec.

### RG-070 — preuve d'enlèvement/livraison (PR #118 backend + #122 mobile — fermé de bout en bout)

Décision explicite de l'utilisateur ("signature tactile seule pour
l'instant") après découverte que le code SMS (autre mode de validation
tiers prévu par le CDC, UC-EXE-03) nécessiterait de faire traverser
`destinataireTelephone` à travers 3 services (mkt→opt→exe, changement
de contrat Kafka partagé avec le Moteur — signalé à sa session, pas
fait). **Ce point (code SMS) reste seul hors périmètre.**

**Backend (PR #118)** : `service-exe` refuse désormais toute
`PRISE_EN_CHARGE`/`LIVRAISON` sans au moins une photo ET une signature
tactile (`PREUVE_MANQUANTE`, nouveau endpoint multipart sur
`POST /api/missions/{id}/etapes`, différencié de l'ancien JSON par
`consumes`). Stockage MinIO + empreinte SHA-256 par photo
(`PreuveEtape`, RG-072/EF-EXE-05, immuable par construction). Le
gateway (WebFlux) reconstruit un multipart réactif vers service-exe
(`MissionExecutionPort.ajouterEtapeAvecPreuve`). EN_TRANSIT/INCIDENT
ne sont pas concernés (JSON existant inchangé pour ces deux types).

**Mobile, app Chauffeur/Transporteur (PR #122)** : nouveau
`SignaturePad` (pad tactile dessiné à la main, `CustomPainter` +
`RenderRepaintBoundary`, sans nouvelle dépendance pub.dev) +
`_FormulairePreuve` (`mission_detail_screen.dart`, même patron que
`_FormulaireIncident` déjà existant) — jusqu'à 3 photos
(`image_picker`, déjà une dépendance) + signature obligatoires avant
de valider PRISE_EN_CHARGE/LIVRAISON. `MissionNotifier
.ajouterEtapeAvecPreuve` (FormData multipart Dio) en plus de
l'`ajouterEtape` JSON existant (conservé pour EN_TRANSIT/INCIDENT).

**Le parcours de test Phase 1→Phase 2 sur téléphone fonctionne de
nouveau normalement** — prise en charge et livraison demandent
désormais une photo + signature à l'écran, tout le reste est inchangé.

Vérifié : `mvn -o test` service-exe (12 tests) + gateway (184 tests) —
backend. `flutter analyze` (0 issue) + `flutter build apk --debug`
(compilation réelle réussie) — mobile, aucun test Flutter n'existait
avant pour cet écran.

### RG-039 — jusqu'à 3 propositions ordonnées (PR #120)

**18e et dernier bloquant, traité en soirée.** Avant de coder, investigation
qui a révélé une portée bien plus large que "générer un top-3" :
`AffectationL1Service` (Kuhn-Munkres) **committe directement** une
`Affectation`/Mission réelle pour le seul match optimal, et **aucun appel**
à `POST /api/cap/capacites/{id}/decrement` n'existe nulle part dans le
pipeline de matching -- EF-MKT-08 ("réservation atomique de la capacité"
à l'acceptation du chargeur) n'était donc pas câblé du tout, pas
seulement le classement top-3. Signalé au collègue Moteur (propriétaire
d'`AffectationL1Service`) avant toute modification.

Décision utilisateur ("Backend complet : top-3 + endpoint accepter") :

- **service-opt** (`AffectationL1Service`) : rang 1 **inchangé**
  (toujours le pick Kuhn-Munkres, `Affectation`+`AffectationConfirmee`
  publiés exactement comme avant -- zéro impact sur service-exe/tournées
  déjà en prod). Rang 2/3 **ajoutés** (`publierAlternatives`) : jusqu'à 2
  alternatives par coût croissant sur la même ligne de la matrice de
  cette demande, purement informationnelles (aucune `Affectation`
  créée, prix estimé non ferme au sens RG-041). "Au plus trois", jamais
  forcé si moins de candidats disponibles.
- **service-mkt** : `Proposition.statut` (EN_ATTENTE/ACCEPTEE/EXPIREE),
  nouveau `POST /api/demandes/{id}/propositions/{propositionId}/accepter`
  qui marque la proposition choisie et expire les autres de la même
  demande.

**Limitation assumée et documentée** : l'endpoint "accepter" ne
déclenche **pas** la réservation réelle de capacité.
`CapaciteService.decrementer()` exige que l'appelant soit du **même
tenant** que le propriétaire de la capacité (IDOR corrigé en PR #95) --
un chargeur (tenant différent du transporteur) qui accepte ne peut donc
pas l'appeler directement sans un pont de confiance cross-tenant qui
n'existe pas aujourd'hui. Construire ce pont était hors périmètre de
cette session -- **à traiter séparément si EF-MKT-08 doit être fermé
complètement**.

Tests : `AffectationL1ServiceTest` (nouveau, aucun test n'existait avant
pour cette classe) -- classement rang 1/2/3 par coût, un seul candidat
ne produit qu'une seule proposition. `DemandeServiceTest` -- accepter
marque bien les autres EXPIREE, refuse une proposition déjà traitée.
Vérifié : `mvn -o test` service-opt (9 tests, Java 21) + service-mkt (4
tests), 0 échec.

### Audit de suivi périmètre Mobile (coéquipiers, dev@727410b) — PR #124, #125

Rapport reçu en soirée, vérifié point par point contre le code réel de
`dev` (jamais de confiance aveugle, ni dans mes propres PR ni dans un
rapport externe) avant d'agir.

**3 constats confirmés réels, tous absents des 18 bloquants originaux :**

- **Secret JWT non paramétrable via variable d'environnement** (PR
  #124) — le rapport ne citait que `service-ida`, vérification élargie
  à tout le dépôt : en réalité **5 services** concernés
  (`service-ida`, `service-exe`, `service-flt`, `service-mkt`,
  `service-not`), tous alignés sur `${FRETCORRIDOR_JWT_SECRET:...}`.
- **`AuthService.rafraichir()` (service-ida) ne vérifiait jamais
  `acteur.getActif()`** (PR #124), contrairement à `login()` — un
  compte verrouillé après 3 échecs de PIN pouvait continuer à
  rafraîchir son token indéfiniment. Même garde ajoutée. Nouveau
  `AuthServiceTest` (aucun test n'existait avant pour cette classe).
- **`GET /api/cap/capacites/{id}` totalement public** (PR #125) —
  d'abord laissé de côté (même architecture que service-geo/mat/opt/trk,
  appelant Kafka sans JWT disponible), puis **corrigé sur demande
  explicite de l'utilisateur** ("corrige une fois c'est pas mieux ?").
  Vérifié conforme CDC/Plan d'Exécution avant de coder : §4.3 du Plan
  d'Exécution autorise explicitement l'appel synchrone entre
  service-not et service-cap (même porteur Mobile) ; ENF-SEC-05
  ("secrets centralisés... rotation périodique") couvre la nouvelle clé.
  Fix : clé interne partagée (`X-Internal-Service-Key`,
  `fretcorridor.internal.service-key`, même mécanisme de rotation par
  variable d'environnement que le secret JWT) -- `ServiceCapClient`
  (service-not) l'envoie, `CapaciteController.obtenir` (service-cap) la
  vérifie, 401 si absente/incorrecte. `SecurityConfig` reste
  `permitAll()` au niveau Spring Security (rien ne change à cette
  couche, la clé est vérifiée au niveau du contrôleur) -- documenté
  explicitement pour ne pas reproduire l'ancien raisonnement "permitAll
  = pas de contrôle". Nouveau `CapaciteControllerTest` (aucun test HTTP
  n'existait avant pour ce contrôleur, comme relevé par le rapport).

**1 point du rapport confirmé réel, resté hors périmètre** : absence de
tests `@SpringBootTest`/`MockMvc` sur `service-ida`/`service-cap` au-delà
de ce qui a été ajouté ci-dessus -- construire une vraie suite
HTTP/Spring Security complète pour ces deux modules serait un chantier
à part, pas traité cette nuit.

**3 points du rapport vérifiés et FAUX/obsolètes** : RG-101, RG-070, et
l'endpoint véhicule `service-flt` donnés "toujours ouverts" sont en
réalité déjà corrigés sur `dev` (PR #117/#118/#115). Le rapport a été
produit sur le commit `727410b` (PR #111), ~12 merges avant ces
correctifs -- pas une erreur de méthode des coéquipiers, juste un
instantané pris trop tôt dans la soirée.

Vérification : `mvn -o test` sur les 5 services touchés par PR #124
(ida 15 tests, exe 12, flt 5, mkt 4, not 3) + service-cap/service-not
pour PR #125 (cap 9 tests, not 3 tests) -- 0 échec partout.

### Autres points hors périmètre — à ne pas croire résolus

- **Multi-pays / conventions bilatérales** (`service-geo`, EF-GEO-05) —
  fonctionnalité absente du domaine, hors périmètre d'un correctif
  ponctuel.
- **Secret webhook par défaut prévisible** (`service-pay`) — déjà piloté
  par `FRETCORRIDOR_PAY_WEBHOOK_SECRET` (fallback dev uniquement dans le
  code) ; l'action réelle relève du déploiement (positionner la variable
  en prod), pas d'un changement de code.

### Vérifié et jugé non concerné — service-geo/mat/opt/trk (Moteur)

En profitant de Java 21 (voir ci-dessous) pour vérifier si les 4
services Moteur avaient le même problème que `service-bur` (#110) :
**non, architecture différente et délibérée**, déjà revue par l'audit
du 19 août lui-même (les `SecurityConfig` de ces 4 services citent
explicitement cet audit dans leur javadoc, contrairement à `service-bur`
dont la docstring était juste trompeuse) :
- `service-geo` : lectures `permitAll` (appels synchrones internes L0
  ~50ms sans JWT + cartes Web en lecture), écritures sensibles
  (`POST`/`PATCH` sur `/api/geo/axes`) restreintes à
  `ROLE_ADMINISTRATION`.
- `service-mat`/`service-opt`/`service-trk` : `permitAll` total —
  endpoints purement internes qui ne transportent jamais de JWT
  (`CoutController` appelé par `ServiceMatClient` sans header
  Authorization), ou endpoints de test manuel (flux nominal réel via
  Kafka), ou (pour `service-trk`) aucun endpoint HTTP exposé du tout.

Ne pas retravailler ces 4 `SecurityConfig` en pensant reproduire le fix
`service-bur` — ce serait casser le budget de latence L0/L1 documenté
sans bénéfice de sécurité réel (l'appelant interne ne transporte de
toute façon aucune identité à vérifier).

### Déjà corrigé par le Moteur, indépendamment de cette passe

- **Fausses alertes `AnomalieDetector`** (`service-trk`, EF-TRK-03) —
  **déjà corrigé** par `stevetelecom` (commit `33818d3`, hors de cette
  session) : fenêtre glissante de 15 min au lieu de comparer à la toute
  première position de l'historique complet. Vérifié en lisant le code
  et son commentaire "BUG CORRIGE (audit du 2026-08-19)" — ne pas
  retravailler ce fichier en pensant le bug encore présent.
- **EF-MAT-10, détour jamais borné** (`service-opt`,
  `SequencementDeclencheur`/`ReplanificationService`/`DetourValidator`)
  — **même commit `33818d3`**, découvert cet après-midi en vérifiant ce
  bloquant précis (pas documenté avant ce jour, alors que
  l'`AnomalieDetector` l'était déjà). `alnsSolver.resoudre(...)` reçoit
  désormais `resoudreParametresAxe(axeId)` (résout `detourMaxDistanceKm`
  réel auprès de `service-geo`) au lieu d'un `Map.of()` codé en dur —
  vérifié réel, pas un stub (`ServiceGeoClient.axeParId`, dégradation
  gracieuse `Map.of()` seulement si axe absent/injoignable). Ne pas
  retravailler ces fichiers en pensant le détour encore illimité.

### Contrainte d'environnement Java 21 — résolue (matin du 20 août)

Ce sandbox n'avait que **Java 17** installé ; `service-geo`/`service-mat`/
`service-opt`/`service-trk` et `common-libs` exigent Java 21. Une
première tentative d'installer Java 21 via SDKMAN avait échoué
(téléchargement corrompu, 136 Mo au lieu des ~190 Mo attendus) —
abandonnée à l'époque plutôt que de s'acharner (**conséquence pratique
documentée alors** : tout changement Moteur, comme `AxeResponse.java`
dans PR #98, ne pouvait être vérifié que par relecture manuelle ligne à
ligne, jamais par compilation réelle).

**Résolu** : `sudo apt-get install -y openjdk-21-jdk` (paquet Ubuntu
standard, `21.0.11+10`) — installé par l'utilisateur lui-même (`sudo`
demande un mot de passe interactif, hors de portée de l'agent). `java`
par défaut bascule automatiquement sur 21 ; Maven nécessite en revanche
`export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` explicite à chaque
commande (l'état du shell ne persiste pas entre deux appels Bash dans
cet environnement — JAVA_HOME doit être réexporté systématiquement pour
tout `mvn` touchant Moteur/`common-libs`).

**Première exécution réelle de `mvn test`** sur les 4 modules Moteur
dans cette session : 2 échecs, tous deux des **tests eux-mêmes**, jamais
revérifiés depuis leur écriture faute de Java 21 — corrigés en PR #112
(`CoutCompositeServiceTest` côté `service-mat` : stub Mockito manquant
sur `saveAll` ; `KuhnMunkresSolverTest.matriceVide` côté `service-opt` :
attendait un comportement contraire à la précondition documentée du
solveur, jamais atteint en pratique). Aucun changement de code de
production. Les 4 modules sont maintenant entièrement verts.

Aucune CI ne couvre ces services Moteur (`backend-web-scope.yml` se
limite à gateway/pay/bur/adm) — toujours vrai, Java 21 disponible en
local ne change rien à ça côté GitHub Actions.

**Tout le reste de l'audit (majeurs/mineurs non cités ci-dessus) n'a pas
été vérifié ni corrigé** dans cette session — ne pas supposer qu'un point
de l'audit est réglé sans le revérifier dans le code, ce document ne
liste que ce qui a été touché explicitement.
