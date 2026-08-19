package com.fretcorridor.opt.oracle;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LotDemandeRepository extends JpaRepository<LotDemande, UUID> {
    List<LotDemande> findByDemandeId(UUID demandeId);
}
