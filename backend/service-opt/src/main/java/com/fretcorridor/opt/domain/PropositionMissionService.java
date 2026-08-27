package com.fretcorridor.opt.domain;

import com.fretcorridor.opt.client.ServiceNotClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * UC-MAT-02 du CDC ("Notification, acceptation ou refus d'une mission par le
 * chauffeur", page 43) : cote reponse du transporteur a une
 * {@link PropositionMission} de rang 1. Voir AffectationL1Service pour la
 * creation de la proposition et {@link #accepter} pour la suite du flux
 * (Affectation, reservation de capacite, etc. -- deplace la depuis
 * AffectationL1Service).
 */
@Service
public class PropositionMissionService {

    private static final Logger log = LoggerFactory.getLogger(PropositionMissionService.class);

    private final PropositionMissionRepository propositionMissionRepository;
    private final AffectationL1Service affectationL1Service;
    private final DemandeEnAttenteRepository demandeEnAttenteRepository;
    private final CapaciteEnAttenteRepository capaciteEnAttenteRepository;
    private final ServiceNotClient serviceNotClient;

    public PropositionMissionService(PropositionMissionRepository propositionMissionRepository,
                                      AffectationL1Service affectationL1Service,
                                      DemandeEnAttenteRepository demandeEnAttenteRepository,
                                      CapaciteEnAttenteRepository capaciteEnAttenteRepository,
                                      ServiceNotClient serviceNotClient) {
        this.propositionMissionRepository = propositionMissionRepository;
        this.affectationL1Service = affectationL1Service;
        this.demandeEnAttenteRepository = demandeEnAttenteRepository;
        this.capaciteEnAttenteRepository = capaciteEnAttenteRepository;
        this.serviceNotClient = serviceNotClient;
    }

    /** "Mes propositions" (app Chauffeur) -- marque au passage les propositions expirees (E1). */
    @Transactional
    public List<PropositionMission> mesPropositions(UUID transporteurId) {
        List<PropositionMission> enAttente = propositionMissionRepository
                .findByTransporteurIdAndStatutOrderByDateCreationDesc(transporteurId, PropositionMission.Statut.EN_ATTENTE);
        for (PropositionMission p : enAttente) {
            if (p.estExpiree()) {
                expirer(p);
            }
        }
        return enAttente.stream().filter(p -> p.getStatut() == PropositionMission.Statut.EN_ATTENTE).toList();
    }

    /**
     * RG-050 (3 interactions au plus) : accepte -- reserve la capacite et
     * cree la mission (deplace depuis AffectationL1Service, voir
     * confirmerDepuisProposition). E3 (acceptation concurrente) : garde par
     * accepterSiPossible(), qui ne transitionne que depuis EN_ATTENTE.
     */
    @Transactional
    public PropositionMission accepter(UUID propositionId, UUID transporteurId) {
        PropositionMission proposition = trouverAppartenant(propositionId, transporteurId);
        if (proposition.estExpiree()) {
            expirer(proposition);
            throw new ResponseStatusException(HttpStatus.GONE, "PROPOSITION_EXPIREE");
        }
        if (!proposition.accepterSiPossible()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "PROPOSITION_DEJA_TRAITEE");
        }
        propositionMissionRepository.save(proposition);
        affectationL1Service.confirmerDepuisProposition(proposition);
        return proposition;
    }

    /**
     * RG-051 (un refus isole n'affecte pas l'indice de conformite -- aucune
     * penalite appliquee ici, juste la transition d'etat) : remet la
     * demande et la capacite en file pour le prochain cycle de matching
     * (E1, "reproposee au candidat suivant" -- au cycle suivant, RG-045,
     * plutot qu'une cascade immediate intra-cycle, cf javadoc de la
     * migration V22 pour le detail de ce choix de perimetre).
     */
    @Transactional
    public PropositionMission refuser(UUID propositionId, UUID transporteurId, String motif) {
        PropositionMission proposition = trouverAppartenant(propositionId, transporteurId);
        if (proposition.estExpiree()) {
            expirer(proposition);
            throw new ResponseStatusException(HttpStatus.GONE, "PROPOSITION_EXPIREE");
        }
        if (!proposition.refuserSiPossible(motif)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "PROPOSITION_DEJA_TRAITEE");
        }
        propositionMissionRepository.save(proposition);
        remettreEnFile(proposition);
        return proposition;
    }

    private void expirer(PropositionMission proposition) {
        proposition.marquerExpireeSiNecessaire();
        propositionMissionRepository.save(proposition);
        remettreEnFile(proposition);
    }

    private void remettreEnFile(PropositionMission proposition) {
        demandeEnAttenteRepository.findFirstByDemandeIdOrderByDateReceptionDesc(proposition.getDemandeId())
                .ifPresentOrElse(d -> {
                    d.remettreEnAttente();
                    demandeEnAttenteRepository.save(d);
                }, () -> log.warn("Aucune DemandeEnAttente retrouvee pour {} au refus/expiration de la proposition {} - "
                        + "remise en file impossible, la demande pourrait ne plus etre reconsideree.",
                        proposition.getDemandeId(), proposition.getId()));

        capaciteEnAttenteRepository.findFirstByCapaciteIdOrderByDateReceptionDesc(proposition.getCapaciteId())
                .ifPresentOrElse(c -> {
                    c.remettreEnAttente();
                    capaciteEnAttenteRepository.save(c);
                }, () -> log.warn("Aucune CapaciteEnAttente retrouvee pour {} au refus/expiration de la proposition {} - "
                        + "remise en file impossible.", proposition.getCapaciteId(), proposition.getId()));
    }

    private PropositionMission trouverAppartenant(UUID propositionId, UUID transporteurId) {
        return propositionMissionRepository.findById(propositionId)
                .filter(p -> transporteurId.equals(p.getTransporteurId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Proposition introuvable : " + propositionId));
    }
}
