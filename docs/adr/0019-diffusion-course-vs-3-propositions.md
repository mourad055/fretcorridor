# ADR 0019 — Modèle de matching : diffusion-course (premier chauffeur acceptant) vs "3 propositions classées au client"

## Statut
Accepte (decision-validee-en-equipe, plan de reorientation post-demo §1)

## Contexte

Le CDC v4.0 (EF-MKT-06/07) decrit un modele de matching : une demande client
recoit **au plus 3 propositions classees et motivees**, presentees au chargeur
qui choisit. Le plan de reorientation post-demo (section 1, "ecart majeur a
trancher AVANT de coder") mettait en evidence que les nouvelles instructions
produisent, elles, exactement le modele inverse :

- **CDC v4.0** : le *client/chargeur* choisit parmi des propositions classees par le moteur.
- **Nouvelle instruction** : une demande est *diffusee a tous les chauffeurs compatibles*, le **premier qui accepte l'obtient**, et les autres voient la notification disparaitre (mais l'historique est conserve).

Ce sont deux modeles de matching structurellement differents, pas une extension
l'un de l'autre. Coder les deux en parallele demultiplie le travail OPT/MAT pour
rien ; la decision doit donc etre tranchee et documentee avant tout dimensionnement.

## Decision

Le modele **diffusion-course ("premier arrive gagne")** est retenu pour la
presentation/production.

Implique, cote Moteur (Personne 3, OPT) :

1. **Diffusion a tous les chauffeurs compatibles** — `AffectationL1Service`
   publie une proposition **par chauffeur compatible** (et non 3 au client).
2. **Resolution atomique premier-arrive** — `AffectationConfirmationService`
   commit l'Affectation via `AffectationRepository.confirmerSiProposee`
   (compare-and-swap en base, aucune double-affectation possible meme si deux
   `DemandeAcceptee` arrivent hors ordre, l'ordre Kafka n'etant pas garanti).
   Les affectations concurrentes de la meme demande sont expirees.
3. **Nouveaux contrats Kafka Mobile -> OPT** (voir `shared-contracts/asyncapi/events/`) :
   - `demande-acceptee` : un chauffeur accepte (resolution de la course).
   - `demande-refusee-par-chauffeur` : refus explicite -> expiration de la
     proposition + ajout du transporteur a la liste d'exclusion de la demande
     (`opt.demande_en_attente.transporteurs_exclus`) + remise en file pour
     re-matching vers un AUTRE chauffeur.

Ce modele etait deja implemente cote OPT (diffusion + confirmation + ecouteurs
`DemandeAcceptee`/`DemandeRefuseeParChauffeur`) ; la presente ADR tranche et
formalise la decision que le code traduisait deja.

## Consequences

- **Pour OPT (Moteur)** : la charge L1 bascule de "classer <=3 et choisir" a
  "diffuser a tous + verrouillage premier-arrive". La course se resout par
  transaction atomique (`confirmerSiProposee`), jamais en supposant un ordre
  d'arrivee des evenements.
- **Pour le client (chargeur)** : il n'y a plus de "choix parmi 3" cote Moteur.
  Le Moteur n'emet plus de proposition *au client* mais une diffusion *aux
  chauffeurs* ; l'issue de la demande (acceptee ou non) est portee par les
  evenements d'acceptation/refus.
- **Refus -> rematching** : un chauffeur qui refuse est exclu du prochain cycle
  pour CETTE demande (persiste dans `transporteurs_exclus`), jamais re-diffuse
  pour la meme demande.
- **Historique** : les chauffeurs non retenus voient la notification disparaitre
  (les affectations concurrentes expirent) mais conservent la trace en historique.
- **Contrats a valider avec Personne 1 (Mobile)** : `demande-acceptee.yaml` et
  `demande-refusee-par-chauffeur.yaml` sont formalises dans shared-contracts et
  doivent etre confirmes par le porteur service-mkt avant implementation du flux
  cote app chauffeur.
- Le modele "3 propositions classees au chargeur" du CDC n'est plus retenu pour
  cette fonctionnalite ; l'ADR ecrase la lecture du CDC S8.5/EF-MKT-06-07 sur ce
  point specifique.
