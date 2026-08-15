package com.fretcorridor.geo.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JournalAuditRisqueRepository extends JpaRepository<JournalAuditRisque, UUID> {

    /** Historique des decisions sur un axe, plus recent d'abord. */
    List<JournalAuditRisque> findByAxeIdOrderByDateDecisionDesc(UUID axeId);
}
