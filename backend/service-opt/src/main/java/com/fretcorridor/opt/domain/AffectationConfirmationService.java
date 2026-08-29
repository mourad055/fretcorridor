package com.fretcorridor.opt.domain;

import com.fretcorridor.opt.client.ServiceCapClient;
import com.fretcorridor.opt.client.ServiceGeoClient;
import com.fretcorridor.opt.messaging.AffectationConfirmeeEvent;
import com.fretcorridor.opt.messaging.OptEventPublisher;
import com.fretcorridor.opt.messaging.RepartitionConventionnelleAppliqueeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Diffusion-course (plan de reorientation post-demo) : point d'entree unique
 * pour committer reellement une Affectation, uniquement au moment ou un
 * chauffeur accepte - jamais a la creation (contrairement au modele "3
 * propositions classees" du CDC, remplace ici). Consomme par
 * DemandeAccepteeListener.
 */
@Service
public class AffectationConfirmationService {

    private static final Logger log = LoggerFactory.getLogger(AffectationConfirmationService.class);

    private final AffectationRepository affectationRepository;
    private final OptEventPublisher eventPublisher;
    private final ServiceCapClient serviceCapClient;
    private final ServiceGeoClient serviceGeoClient;

    public AffectationConfirmationService(AffectationRepository affectationRepository,
                                           OptEventPublisher eventPublisher,
                                           ServiceCapClient serviceCapClient,
                                           ServiceGeoClient serviceGeoClient) {
        this.affectationRepository = affectationRepository;
        this.eventPublisher = eventPublisher;
        this.serviceCapClient = serviceCapClient;
        this.serviceGeoClient = serviceGeoClient;
    }

    /**
     * @param transporteurIdAcceptant transmis par l'evenement DemandeAcceptee -
     *        peut differer de celui deja connu de l'Affectation si
     *        service-cap ne l'avait pas encore publie au moment de la
     *        diffusion (meme tolerance que le reste du code, cf
     *        CapaciteDeclareeEvent.transporteurId nullable).
     */
    @Transactional
    public void confirmer(UUID affectationId, UUID transporteurIdAcceptant) {
        int lignesAffectees = affectationRepository.confirmerSiProposee(affectationId);

        if (lignesAffectees == 0) {
            // Trop tard : une autre proposition de la meme demande a deja
            // gagne la course, ou cette Affectation n'existe plus a l'etat
            // PROPOSEE. Pas une erreur - le comportement attendu de la
            // diffusion-course pour tout candidat qui n'a pas ete le premier.
            log.info("Confirmation ignoree - affectation={} deja non-PROPOSEE (course perdue ou deja traitee)",
                    affectationId);
            return;
        }

        Affectation affectation = affectationRepository.findById(affectationId)
                .orElseThrow(() -> new IllegalStateException(
                        "Affectation " + affectationId + " confirmee mais introuvable juste apres - incoherence"));

        log.info("Course gagnee - affectation={}, demande={}, capacite={}",
                affectationId, affectation.getDemandeId(), affectation.getCapaciteId());

        // --- Reservation reelle de la capacite (deplacee ici depuis
        //     AffectationL1Service - ne doit jamais avoir lieu avant
        //     acceptation reelle, cf diffusion-course) ---
        if (affectation.getPoidsTaxableKg() != null) {
            serviceCapClient.reserver(affectation.getCapaciteId(), affectation.getPoidsTaxableKg(),
                    affectation.getId().toString());
        }

        // --- Publication AffectationConfirmee (-> service-exe) ---
        // Champs desormais persistes sur Affectation depuis le meme correctif
        // qui a supprime AffectationConfirmeeEvent de AffectationL1Service -
        // plus aucun placeholder null ici (cf migration ajoutant vehicule_id
        // et les champs marchandise/destinataire).
        AffectationConfirmeeEvent confirmation = new AffectationConfirmeeEvent(
                UUID.randomUUID(),
                affectation.getId(),
                affectation.getDemandeId(),
                affectation.getCapaciteId(),
                affectation.getVehiculeId(),
                transporteurIdAcceptant,
                null,
                affectation.getAxeId(),
                affectation.getOrigineLatitude(), affectation.getOrigineLongitude(), null,
                affectation.getDestinationLatitude(), affectation.getDestinationLongitude(), null,
                affectation.getDistanceMetres(),
                affectation.getDureeSecondes() != null ? affectation.getDureeSecondes().longValue() : null,
                affectation.getIntervalleConfianceSecondes() != null
                        ? affectation.getIntervalleConfianceSecondes().longValue() : null,
                affectation.getGeometrieEncodee(),
                affectation.getPrixTransport(),
                affectation.getCommissionPlateforme(),
                affectation.getMontantVerseTransporteur(),
                "XAF", "DEPOT", "RETRAIT",
                Instant.now(),
                affectation.getTypeEmballageNom(), affectation.getQuantite(),
                affectation.getPoidsTaxableKg(),
                affectation.getDestinataireNom(), affectation.getDestinataireTelephone(),
                affectation.getModeCollecte(), affectation.getTypeDisponibilite(),
                affectation.getPoidsTotalKg(), affectation.getGrandeValeur()
        );
        eventPublisher.publierAffectationConfirmee(confirmation);

        // --- Expiration des autres propositions de la meme demande ---
        expirerAutresPropositions(affectation);

        // --- RepartitionConventionnelleAppliquee (-> service-pay, Phase 4) ---
        publierRepartitionSiApplicable(affectation);
    }

    private void expirerAutresPropositions(Affectation gagnante) {
        List<Affectation> autres = affectationRepository
                .findByDemandeIdAndStatut(gagnante.getDemandeId(), StatutAffectation.PROPOSEE);
        for (Affectation perdante : autres) {
            if (perdante.getId().equals(gagnante.getId())) {
                continue;
            }
            // Meme mise a jour atomique que la confirmation - une perdante
            // pourrait elle-meme etre en train d'etre confirmee au meme
            // instant par un autre thread ; le WHERE ... AND statut=PROPOSEE
            // dans confirmerSiProposee empeche deja toute double-victoire,
            // mais l'expiration explicite ici reste "best effort" (pas
            // d'exception si 0 ligne affectee - juste deja traitee ailleurs).
            perdanteExpirerSiProposee(perdante.getId());
        }
        log.info("{} proposition(s) concurrente(s) expiree(s) pour demande={}", autres.size(), gagnante.getDemandeId());
        // TODO (a cabler avec Mobile) : publier un evenement "PropositionExpiree"
        // par perdante pour que la notification disparaisse reellement cote
        // app chauffeur (plan de reorientation : "la notification disparait
        // chez les autres") - pas encore de contrat AsyncAPI brouillon pour
        // celui-ci, a faire avant que Mobile ne cable l'ecran de disparition.
    }

    private void perdanteExpirerSiProposee(UUID affectationId) {
        // Reutilise la meme requete conditionnelle que la confirmation, mais
        // vers EXPIREE - meme garantie d'atomicite, meme tolerance au "0 ligne
        // affectee" (deja traitee par ailleurs, jamais une erreur).
        affectationRepository.findById(affectationId).ifPresent(Affectation::marquerExpireeSiProposee);
    }

    private void publierRepartitionSiApplicable(Affectation affectation) {
        if (affectation.getAxeId() == null) {
            return;
        }
        var axeDetail = serviceGeoClient.axeParId(affectation.getAxeId());
        if (axeDetail == null || axeDetail.parametres() == null) {
            return;
        }
        if (!(axeDetail.parametres().get("conventionRepartition") instanceof java.util.Map<?, ?> conventionMap)) {
            return;
        }
        Object conventionCodeObj = conventionMap.get("conventionCode");
        Object partsObj = conventionMap.get("partsPourcent");
        if (conventionCodeObj == null || !(partsObj instanceof java.util.Map<?, ?> partsMap)) {
            return;
        }
        java.util.Map<String, Double> parts = new java.util.HashMap<>();
        for (var entree : partsMap.entrySet()) {
            if (entree.getValue() instanceof Number n) {
                parts.put(entree.getKey().toString(), n.doubleValue());
            }
        }
        eventPublisher.publierRepartitionConventionnelleAppliquee(new RepartitionConventionnelleAppliqueeEvent(
                UUID.randomUUID(), affectation.getId(), affectation.getAxeId(),
                conventionCodeObj.toString(), parts, Instant.now(), false));
    }

    /**
     * Refus explicite d'un chauffeur (distinct de l'expiration automatique).
     * Marque uniquement CETTE Affectation EXPIREE - ne touche pas aux autres
     * propositions de la meme demande (elles restent PROPOSEE, la course
     * continue normalement pour les autres candidats).
     */
    @Transactional
    public void refuser(UUID affectationId) {
        affectationRepository.findById(affectationId).ifPresentOrElse(
                Affectation::marquerExpireeSiProposee,
                () -> log.warn("Refus recu pour une affectation introuvable - affectation={}", affectationId));
    }
}
