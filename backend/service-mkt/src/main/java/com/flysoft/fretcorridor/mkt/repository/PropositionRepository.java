package com.flysoft.fretcorridor.mkt.repository;

import com.flysoft.fretcorridor.mkt.entity.Proposition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PropositionRepository extends JpaRepository<Proposition, UUID> {
    List<Proposition> findByDemandeIdOrderByRangAsc(UUID demandeId);

    java.util.Optional<Proposition> findByIdAndDemandeId(UUID id, UUID demandeId);
}
