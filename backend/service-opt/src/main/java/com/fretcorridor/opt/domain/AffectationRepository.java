package com.fretcorridor.opt.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AffectationRepository extends JpaRepository<Affectation, UUID> {
    // findById(UUID) suffit pour l'instant : c'est exactement l'appel que TRK
    // fera avec le mission_id recu (a terme) via AffectationConfirmee cote
    // Mobile, ou directement connu du contexte appelant en Phase 1.
}
