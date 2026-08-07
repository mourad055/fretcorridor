package com.fretcorridor.mat.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ModelePonderationRepository extends JpaRepository<ModelePonderation, UUID> {

    // Modele specifique a l'axe, s'il existe (RG-106, EF-GEO-02).
    Optional<ModelePonderation> findFirstByAxeIdAndActifTrue(UUID axeId);

    // Modele par defaut (axeId null), utilise en repli. Au plus une ligne
    // garantie par l'index unique partiel (cf migration V3).
    Optional<ModelePonderation> findFirstByAxeIdIsNullAndActifTrue();
}
