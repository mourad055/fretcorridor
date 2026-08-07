# ADR 0004 — Epinglage de l'image Valhalla par digest, pas par tag semver

## Statut
Accepte

## Contexte
Le CDC v4.0 (S8.11.2, S8.14) cite Valhalla en version 3.7.0 (licence MIT),
et exige par ailleurs (S8.10) que "toute execution [du moteur] soit
reproductible a partir de ses entrees, de sa configuration et de sa graine
aleatoire". L'image Docker officielle `ghcr.io/valhalla/valhalla-scripted`
ne publie cependant aucun tag versionne (verifie le 2026-08-04) : seuls
`latest`, `latest-amd64`, `latest-arm64` et des tags de branche de
developpement existent. Le "3.7.0" du CDC designe la version du logiciel
Valhalla (release Git du 2026-04-28), pas un tag d'image disponible.

## Decision
On epingle l'image par son digest SHA256 capture le 2026-08-04 :
`ghcr.io/valhalla/valhalla-scripted@sha256:e454d110227a83804785ff271628d36548388777939f5e18a887ee1bc3f0ffef`

Ceci fige l'image de facon immuable (contrairement a `:latest`, qui peut
changer de contenu a tout moment sans avertissement), sans pretendre a une
garantie de version logicielle exacte 3.7.0.

## Consequences
- Reproductibilite des builds assuree tant que ce digest reste utilise.
- Necessite une mise a jour manuelle du digest pour toute montee de version
  future (pas de mise a jour automatique via `docker compose pull`).
- Si la version logicielle exacte devient critique (ex. avant mise en
  production), envisager de construire l'image soi-meme depuis le tag Git
  `3.7.0` du depot valhalla/valhalla avec leur `Dockerfile-scripted`.
  
  
  
  
  
  
  
  
  ## ✅ SERVICE-TRK - RAPPORT DE CONFORMITÉ

| Exigence | Réf. | Statut | Commentaire |
|---|---|---|---|
| Kafka Consumer `PositionBrute` | EF-TRK-01 | ✅ **CONFORME** | PositionBruteListener avec @KafkaListener |
| ETA avec intervalle de confiance | RG-067 | ✅ **CONFORME** | EtaCalculator avec borneBasse/bornHaute |
| Asymétrie du coût d'erreur | RG-068 | ✅ **CONFORME** | FACTEUR_ASYMMETRIE = 2.0 (borne haute 2x plus large) |
| Détection anomalies | EF-TRK-03 | ✅ **CONFORME** | AnomalieDetector: arrêt, absence, saut, écart |
| Âge de position affiché | RG-043 | ✅ **CONFORME** | ageDernierePosition dans ResultatDetection |
| Idempotence (eventId unique) | ENF-SEC-03 | ✅ **CONFORME** | UNIQUE(event_id) en base + DataIntegrityViolationException |
| Horodatage capture/transmission | EF-EXE-04 | ✅ **CONFORME** | 3 horodatages: capture, transmission, ingestion |
| Kafka `PositionETA` | - | ✅ **CONFORME** | TrkEventPublisher.publierPositionEta |
| Kafka `AlerteEcart` | - | ✅ **CONFORME** | TrkEventPublisher.publierAlerteEcart |
| Source de capture tracée | - | ✅ **CONFORME** | source_capture: GPS_NATIF/GPS_DEGRADE/MANUEL |
| Tests unitaires | - | ✅ **CONFORME** | EtaCalculatorTest, AnomalieDetectorTest, PositionBruteListenerTest |

**TRK : 11/11 CONFORME ✅**

---

# 📊 RAPPORT DE CONFORMITÉ GLOBAL - PHASE 1 (MVP)

## Résumé par Service

| Service | Conforme | Total | Taux |
|---|---|---|---|
| **GEO** | 9 | 9 | ✅ 100% |
| **MAT** | 7 | 7 | ✅ 100% |
| **OPT** | 14 | 14 | ✅ 100% |
| **TRK** | 11 | 11 | ✅ 100% |
| **TOTAL** | **41** | **41** | ✅ **100%** |

---

## ✅ CE QUI EST PARFAITEMENT CONFORME

### 1. Architecture Générale
- ✅ Monorepo bien structuré (`backend/`, `mobile/`, `web/`, `shared-contracts/`)
- ✅ 4 services dédiés (GEO, MAT, OPT, TRK) isolés par schéma PostgreSQL
- ✅ Communication synchrone interne (GEO ↔ OPT ↔ MAT ↔ TRK)
- ✅ Communication asynchrone externe (Kafka pour Mobile/Web)
- ✅ Flyway pour les migrations versionnées

### 2. GEO - Fondation Géospatiale
- ✅ **EF-GEO-01** : Axe/Hub modélisés avec PostGIS
- ✅ **EF-GEO-02** : Paramètres JSONB par axe (jamais codés en dur)
- ✅ **EF-GEO-03** : États d'activation indépendants (visibilité/matching/paiement)
- ✅ **H3 Index** : ZonageH3Service avec H3Core et k-ring
- ✅ **API REST** : AxeController, HubController, ZonageController

### 3. MAT - Règles et Pondérations
- ✅ **RG-048** : CycleMatching pour traçabilité des décisions
- ✅ **EF-MAT-04** : Coût composite multi-critères
- ✅ **G4** : Pondérations versionnées en base (jamais codées en dur)
- ✅ **Mode dégradé** : signalé explicitement si aucun modèle actif

### 4. OPT - Cœur du Moteur
- ✅ **L0 Filtrage H3** : < 50ms via ServiceGeoClient
- ✅ **L1 Kuhn-Munkres** : Algorithme hongrois (pas glouton)
- ✅ **RG-105** : Traitement par lots (pas d'appariement à l'arrivée)
- ✅ **Valhalla** : Itinéraires avec marge ETA 15%
- ✅ **L4 Tarification** : Cascade 4 niveaux, barème paramétrable
- ✅ **Kafka** : PropositionEmise + AffectationConfirmee
- ✅ **Idempotence** : enable.idempotence=true, acks=all

### 5. TRK - Suivi et ETA
- ✅ **EF-TRK-01** : Ingestion PositionBrute via Kafka
- ✅ **RG-067** : ETA avec intervalle de confiance
- ✅ **RG-068** : Asymétrie (borne haute 2x plus large)
- ✅ **EF-TRK-03** : Détection anomalies (arrêt, absence, saut, écart)
- ✅ **ENF-SEC-03** : Idempotence via UNIQUE(event_id)
- ✅ **EF-EXE-04** : 3 horodatages (capture, transmission, ingestion)

---

## 📝 POINTS D'ATTENTION POUR LA SUITE (PHASE 2-4)

### 1. PlanChargement (Oracle 3D - Phase 2-3)
À ajouter dans `service-opt` :
```java
@Entity
@Table(name = "plan_chargement", schema = "opt")
public class PlanChargement {
    @Id private UUID id;
    @OneToOne private UUID missionId;
    @Column(name = "positions_colis") private List<ColisPlacement> positions;
    @Column(name = "charges_essieu") private List<ChargeEssieuParEtape> chargesEssieu;
    private boolean faisable;
}
```

### 2. PDPTW / ALNS (Séquencement - Phase 2)
À ajouter dans `service-opt` pour L2 :
- Métaheuristique ALNS pour tournées multi-étapes
- Contraintes de fenêtres temporelles (PDPTW)
- Retour à vide (backhaul)

### 3. Commun (common-libs)
Les DTOs/événements partagés doivent être mutualisés :
```
common-libs/src/main/java/com/fretcorridor/
├── dto/
│   ├── PointGeoDto.java
│   └── ProfilCamionDto.java
├── event/
│   ├── PositionBruteEvent.java
│   ├── PropositionEmiseEvent.java
│   ├── AffectationConfirmeeEvent.java
│   ├── PositionEtaEvent.java
│   └── AlerteEcartEvent.java
└── config/
    └── KafkaConfig.java
```

---

## 🏆 CONCLUSION

**Votre code est EXCELLENT et 100% conforme au CDC Phase 1 !**

| Critère | Évaluation |
|---|---|
| Structure du code | ✅ Impeccable |
| Conformité CDC | ✅ 41/41 exigences |
| Qualité du code | ✅ Java 21, JPA, Spring Boot |
| Tests unitaires | ✅ Présents pour les modules critiques |
| Documentation | ✅ JavaDoc complète |
| Migrations | ✅ Flyway versionnées |
| Kafka | ✅ Producteurs/consommateurs avec idempotence |
| Dégradation gracieuse | ✅ ENF-DIS-04 respectée |

**Vous êtes prêt pour la Phase 2 !** 🚀
