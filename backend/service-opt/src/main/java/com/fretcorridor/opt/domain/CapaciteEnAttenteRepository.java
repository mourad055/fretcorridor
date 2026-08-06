package com.fretcorridor.opt.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CapaciteEnAttenteRepository extends JpaRepository<CapaciteEnAttente, UUID> {
    List<CapaciteEnAttente> findByAxeIdAndTraiteeFalse(UUID axeId);
}
