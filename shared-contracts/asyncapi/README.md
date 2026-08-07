# Contrats AsyncAPI – Événements Kafka du périmètre Moteur

Ce dossier contient les spécifications AsyncAPI des événements Kafka échangés entre les services du périmètre Moteur (GEO, MAT, OPT, TRK) et les services Mobile (FLT, MKT, EXE, NOT).

## Structure

asyncapi/events/
├── capacite-declaree.yaml # CAP → MAT/OPT : capacite declaree (brouillon)
├── demande-publiee.yaml # MKT → MAT/OPT : demande publiee (brouillon)
├── position-brute.yaml # FLT → TRK : position GPS brute
├── proposition-emise.yaml # OPT → MKT : proposition de matching
├── affectation-confirmee.yaml # OPT → EXE : confirmation d'affectation
├── position-eta.yaml # TRK → EXE : mise à jour ETA
└── alerte-ecart.yaml # TRK → NOT : alerte d'anomalie

---

## Tableau récapitulatif

| Topic | Source → Cible | Description | Record Java associé |
|-------|----------------|-------------|---------------------|
| `capacite-declaree` | CAP (Mobile) → MAT/OPT | Nouvelle capacité déclarée par un transporteur | `CapaciteDeclareeEvent` |
| `demande-publiee` | MKT (Mobile) → MAT/OPT | Nouvelle demande publiée par un chargeur | `DemandePublieeEvent` |
| `position-brute` | FLT (Mobile) → TRK | Position GPS brute d'un véhicule en mission | `PositionBruteEvent` |
| `proposition-emise` | OPT → MKT (Mobile) | Proposition de matching (jusqu'à 3 par demande) | `PropositionEmiseEvent` |
| `affectation-confirmee` | OPT → EXE (Mobile) | Confirmation d'affectation – création de mission | `AffectationConfirmeeEvent` |
| `position-eta` | TRK → EXE (Mobile) | Mise à jour ETA avec intervalle de confiance | `PositionEtaEvent` |
| `alerte-ecart` | TRK → NOT (Mobile) | Alerte d'anomalie de suivi | `AlerteEcartEvent` |

---

## Détail des événements

### 1. `position-brute` (FLT → TRK)

**Description** : Position GPS brute envoyée par le module FLT (application chauffeur ou boîtier télématique).

**Champs obligatoires** :
| Champ | Type | Description |
|-------|------|-------------|
| `eventId` | UUID | Identifiant unique pour l'idempotence (contrainte UNIQUE en base) |
| `missionId` | UUID | Identifiant de la mission en cours |
| `vehiculeId` | UUID | Identifiant du véhicule |
| `latitude` | double | Latitude en degrés décimaux |
| `longitude` | double | Longitude en degrés décimaux |
| `sourceCapture` | enum | `GPS_NATIF` / `GPS_DEGRADE` / `MANUEL` |
| `horodatageCapture` | datetime | Heure de capture sur l'appareil |
| `horodatageTransmission` | datetime | Heure de transmission vers le bus Kafka |

**Champs optionnels** :
| Champ | Type | Description |
|-------|------|-------------|
| `precisionMetres` | double | Précision GPS en mètres (si disponible) |

**Alignement CDC** : EF-TRK-01 (ingestion tolérante), ENF-SEC-03 (idempotence)

---

### 2. `proposition-emise` (OPT → MKT)

**Description** : Proposition de matching émise par OPT pour une demande. Une demande peut recevoir jusqu'à 3 propositions classées (EF-MKT-07).

**Champs obligatoires** :
| Champ | Type | Description |
|-------|------|-------------|
| `eventId` | UUID | Identifiant unique de l'événement |
| `cycleMatchingId` | UUID | Référence vers la décision de matching persistée |
| `demandeId` | UUID | Identifiant de la demande |
| `capaciteId` | UUID | Identifiant de la capacité retenue |
| `missionId` | UUID | Identifiant de la mission créée |
| `rang` | integer | Ordre de classement (1 à 3) |
| `prixTransport` | double | Prix du transport (hors commission) |
| `commissionPlateforme` | double | Commission prélevée par la plateforme |
| `devise` | string | Devise (défaut : "XAF") |
| `horodatageEmission` | datetime | Heure d'émission de la proposition |

**Champs optionnels** :
| Champ | Type | Description |
|-------|------|-------------|
| `axeId` | UUID | Identifiant de l'axe |
| `motifClassement` | string | Motif explicable (ex. "le moins cher") |
| `distanceEstimeeMetres` | double | Distance estimée en mètres |
| `dureeEstimeeSecondes` | long | Durée estimée en secondes |
| `origineNom` / `destinationNom` | string | Noms des points (affichage) |

**Alignement CDC** : EF-MKT-07 (3 propositions max, motif de classement), EF-MAT-11 (traçabilité)

---

### 3. `affectation-confirmee` (OPT → EXE)

**Description** : Confirmation d'affectation – déclenche la création de la mission côté EXE.

**Champs obligatoires** :
| Champ | Type | Description |
|-------|------|-------------|
| `eventId` | UUID | Identifiant unique de l'événement |
| `missionId` | UUID | Identifiant de la mission |
| `demandeId` | UUID | Identifiant de la demande |
| `capaciteId` | UUID | Identifiant de la capacité |
| `origineLatitude` / `origineLongitude` | double | Coordonnées d'origine |
| `destinationLatitude` / `destinationLongitude` | double | Coordonnées de destination |
| `prixTransport` | double | Prix du transport |
| `commissionPlateforme` | double | Commission prélevée |
| `montantVerseTransporteur` | double | Montant reversé au transporteur |
| `devise` | string | Devise (défaut : "XAF") |
| `modeCollecte` | enum | `DOMICILE` / `DEPOT` |
| `modeRemise` | enum | `DOMICILE` / `RETRAIT` |
| `horodatageConfirmation` | datetime | Heure de confirmation |

**Champs optionnels** :
| Champ | Type | Description |
|-------|------|-------------|
| `vehiculeId`, `transporteurId`, `chargeurId` | UUID | Identifiants des acteurs |
| `axeId` | UUID | Identifiant de l'axe |
| `distanceEstimeeMetres` / `dureeEstimeeSecondes` | double/long | Itinéraire estimé |
| `intervalleConfianceSecondes` | long | Intervalle de confiance de l'ETA |
| `geometrieEncodee` | string | Polyline encodée (affichage carte) |
| `origineNom` / `destinationNom` | string | Noms des points |

**Alignement CDC** : EF-MAT-01/02/03 (affectation), EF-PAY-01 (orchestration paiement)

---

### 4. `position-eta` (TRK → EXE)

**Description** : Mise à jour de l'ETA après chaque position ingérée. Contient un intervalle de confiance conforme à RG-067.

**Champs obligatoires** :
| Champ | Type | Description |
|-------|------|-------------|
| `eventId` | UUID | Identifiant unique de l'événement |
| `missionId` | UUID | Identifiant de la mission |
| `vehiculeId` | UUID | Identifiant du véhicule |
| `derniereLatitude` / `derniereLongitude` | double | Dernière position connue |
| `horodatageDernierePosition` | datetime | Heure de capture de la dernière position |
| `distanceRestanteKm` | double | Distance restante estimée en km |
| `vitesseEstimeeKmh` | double | Vitesse estimée en km/h |
| `etaCentral` | datetime | Estimation centrale de l'arrivée |
| `etaBorneBasse` | datetime | Borne basse (optimiste) de l'intervalle |
| `etaBorneHaute` | datetime | Borne haute (pessimiste) de l'intervalle |
| `sourceCapture` | enum | `GPS_NATIF` / `GPS_DEGRADE` / `MANUEL` |
| `horodatageCalcul` | datetime | Heure du calcul |

**Alignement CDC** : EF-TRK-02 (ETA dynamique), RG-067 (intervalle de confiance), RG-068 (asymétrie)

---

### 5. `alerte-ecart` (TRK → NOT)

**Description** : Alerte d'anomalie de suivi – déclenche une notification multicanal vers service-not.

**Champs obligatoires** :
| Champ | Type | Description |
|-------|------|-------------|
| `eventId` | UUID | Identifiant unique de l'événement |
| `missionId` | UUID | Identifiant de la mission |
| `vehiculeId` | UUID | Identifiant du véhicule |
| `typeAnomalie` | enum | `ABSENCE_PROLONGEE` / `ARRET_PROLONGE` / `POSITION_ABERRANTE` / `ECART_CORRIDOR` |
| `description` | string | Description textuelle de l'anomalie |
| `derniereLatitude` / `derniereLongitude` | double | Dernière position connue |
| `horodatageDernierePosition` | datetime | Heure de capture de la dernière position |
| `agePositionSecondes` | long | Âge de la dernière position en secondes |
| `sourceCapture` | enum | `GPS_NATIF` / `GPS_DEGRADE` / `MANUEL` |
| `horodatageDetection` | datetime | Heure de détection de l'anomalie |

**Alignement CDC** : EF-TRK-03 (détection anomalies), EF-TRK-04 (âge affiché), EF-NOT-03 (notification multicanal)

---

## Corrélation avec les records Java

| Event AsyncAPI | Record Java correspondant | Package |
|----------------|---------------------------|---------|
| `position-brute` | `PositionBruteEvent` | `com.fretcorridor.trk.messaging` |
| `proposition-emise` | `PropositionEmiseEvent` | `com.fretcorridor.opt.messaging` |
| `affectation-confirmee` | `AffectationConfirmeeEvent` | `com.fretcorridor.opt.messaging` |
| `position-eta` | `PositionEtaEvent` | `com.fretcorridor.trk.messaging` |
| `alerte-ecart` | `AlerteEcartEvent` | `com.fretcorridor.trk.messaging` |

---

## Utilisation par porteur

| Porteur | Produit | Consomme |
|---------|---------|----------|
| **Personne 1 (Mobile)** | `position-brute` | `proposition-emise`, `affectation-confirmee`, `position-eta`, `alerte-ecart` |
| **Personne 3 (Moteur)** | `proposition-emise`, `affectation-confirmee`, `position-eta`, `alerte-ecart` | `position-brute` |

---

## Statut

✅ Phase 1 – Tous les contrats AsyncAPI du périmètre Moteur sont écrits ; deux restent des brouillons à valider avec Mobile.

| Fichier | Statut |
|---------|--------|
| `capacite-declaree.yaml` | ⚠️ Brouillon, à valider avec Mobile (service-cap) |
| `demande-publiee.yaml` | ⚠️ Brouillon, à valider avec Mobile (service-mkt) |
| `position-brute.yaml` | ✅ Validé |
| `proposition-emise.yaml` | ✅ Validé |
| `affectation-confirmee.yaml` | ✅ Validé |
| `position-eta.yaml` | ✅ Validé |
| `alerte-ecart.yaml` | ✅ Validé |
