package com.fretcorridor.opt.sequencement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TourneeRepository extends JpaRepository<Tournee, UUID> {
    List<Tournee> findByCapaciteIdAndStatut(UUID capaciteId, Tournee.Statut statut);
}
