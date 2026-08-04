package com.fretcorridor.trk.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PositionRepository extends JpaRepository<Position, UUID> {
    // save() herite suffit pour cet increment : l'idempotence repose sur la
    // contrainte UNIQUE(event_id) en base (cf migration V2), pas sur une
    // verification prealable ici - un round-trip DB de moins sur le chemin
    // heureux, l'exception ne survient que sur un vrai doublon (cas rare).
}
