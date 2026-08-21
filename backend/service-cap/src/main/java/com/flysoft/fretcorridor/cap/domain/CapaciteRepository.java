package com.flysoft.fretcorridor.cap.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CapaciteRepository extends JpaRepository<Capacite, UUID> {

    // EF-CAP-08 : capacites dont la fenetre de depart est passee, pas
    // encore marquees expirees - cible du job d'expiration automatique.
    List<Capacite> findByDateDepartBeforeAndExpireeFalse(Instant maintenant);

    // Liste "Mes capacites" (app Chauffeur/Transporteur) - propres
    // declarations du transporteur connecte, plus recentes d'abord.
    List<Capacite> findByTransporteurIdAndTenantIdOrderByDateCreationDesc(java.util.UUID transporteurId, String tenantId);
}
