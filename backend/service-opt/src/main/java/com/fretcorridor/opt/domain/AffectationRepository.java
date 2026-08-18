package com.fretcorridor.opt.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AffectationRepository extends JpaRepository<Affectation, UUID> {
    // findById(UUID) suffit pour l'instant : c'est exactement l'appel que TRK
    // fera avec le mission_id recu (a terme) via AffectationConfirmee cote
    // Mobile, ou directement connu du contexte appelant en Phase 1.

    // Sprint 11 (sequencement L2) : affectations confirmees pas encore
    // regroupees dans une Tournee. Requete ciblee plutot que findAll() +
    // filtre en memoire cote Java - evite de charger toute la table a
    // chaque cycle de SequencementDeclencheur.
    @org.springframework.data.jpa.repository.Query(
            "SELECT a FROM Affectation a WHERE a.id NOT IN "
                    + "(SELECT DISTINCT e.affectationId FROM com.fretcorridor.opt.sequencement.EtapeTournee e)")
    java.util.List<Affectation> findNonEncoreSequencees();
}
