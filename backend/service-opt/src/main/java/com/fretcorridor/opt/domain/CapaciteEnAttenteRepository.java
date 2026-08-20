package com.fretcorridor.opt.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CapaciteEnAttenteRepository extends JpaRepository<CapaciteEnAttente, UUID> {

    List<CapaciteEnAttente> findByAxeIdAndTraiteeFalse(UUID axeId);

    // EF-MAT-07 (Sprint 11, capacite dynamique) : SequencementDeclencheur a
    // besoin de capaciteResiduelleKg (plafond de la CAPACITE, distinct de
    // Affectation.poidsTaxableKg qui porte le poids de la DEMANDE). Pas de
    // contrainte d'unicite sur capaciteId seul (seulement sur eventId) - la
    // plus recente est retenue plutot que de risquer une ambiguite si
    // plusieurs CapaciteEnAttente existent pour le meme capaciteId dans le
    // temps (ex. redeclaration cote service-cap).
    Optional<CapaciteEnAttente> findFirstByCapaciteIdOrderByDateReceptionDesc(UUID capaciteId);
}
