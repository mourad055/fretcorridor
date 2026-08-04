package com.fretcorridor.mat.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PonderationCritereRepository extends JpaRepository<PonderationCritere, UUID> {

    List<PonderationCritere> findByModeleId(UUID modeleId);
}
