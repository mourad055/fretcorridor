package com.fretcorridor.mat.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ModelePonderationRepository extends JpaRepository<ModelePonderation, UUID> {

    // Au plus une ligne actif=true garantie par l'index unique partiel (cf migration V1) -
    // findFirst plutot que find pour rester explicite sur l'intention, meme si en
    // pratique il ne peut y en avoir qu'une.
    Optional<ModelePonderation> findFirstByActifTrue();
}
