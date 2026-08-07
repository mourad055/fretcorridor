package com.fretcorridor.opt.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DemandeEnAttenteRepository extends JpaRepository<DemandeEnAttente, UUID> {
    List<DemandeEnAttente> findByAxeIdAndTraiteeFalse(UUID axeId);
}
