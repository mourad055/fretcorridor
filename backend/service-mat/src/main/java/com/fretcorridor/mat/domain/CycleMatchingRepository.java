package com.fretcorridor.mat.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CycleMatchingRepository extends JpaRepository<CycleMatching, UUID> {
    // save/saveAll/findById herites suffisent pour cet increment (calcul + persistance).
    // Une recherche par demande_id (ex. pour un ecran de litige cote Web/ADM) viendra
    // avec l'increment "consultation", hors perimetre ici.
}
