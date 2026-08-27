package com.fretcorridor.opt.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DemandeEnAttenteRepository extends JpaRepository<DemandeEnAttente, UUID> {
    List<DemandeEnAttente> findByAxeIdAndTraiteeFalse(UUID axeId);

    // DemandeAnnuleeListener : retire une demande annulee de la file
    // d'attente avant qu'un cycle de matching ne la traite (traiteeFalse
    // uniquement - si deja traitee, une Affectation existe deja, trop tard
    // pour l'annuler cote Moteur).
    List<DemandeEnAttente> findByDemandeIdAndTraiteeFalse(UUID demandeId);

    // UC-MAT-02 : retrouve la ligne (traitee ou non) a remettre en file au
    // refus/expiration d'une PropositionMission -- meme pattern que
    // CapaciteEnAttenteRepository.findFirstByCapaciteIdOrderByDateReceptionDesc.
    java.util.Optional<DemandeEnAttente> findFirstByDemandeIdOrderByDateReceptionDesc(UUID demandeId);
}
