package com.flysoft.fretcorridor.exe.repository;

import com.flysoft.fretcorridor.exe.entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MissionRepository extends JpaRepository<Mission, UUID> {
    Optional<Mission> findByDemandeIdAndTenantId(UUID demandeId, String tenantId);

    // BUG CORRIGE (retour utilisateur direct, 22 aout) : le chargeur (tenant
    // "MARKETPLACE_CM") consultant le suivi de SA propre demande recevait
    // toujours 204 - findByDemandeIdAndTenantId ne matchait jamais, car
    // Mission.tenantId porte le tenant PHASE 1 cote execution/transporteur
    // ("tenant-bgft-douala", cf AffectationConfirmeeListener), pas le tenant
    // marketplace du chargeur qui a publie la demande. demandeId (UUID non
    // devinable, connu du seul chargeur proprietaire) sert deja de frontiere
    // de securite suffisante ici - meme principe que notificationAppartenantA
    // (service-not), scope par identifiant plutot que par tenant.
    Optional<Mission> findByDemandeId(UUID demandeId);

    // S7 : reste vide en pratique tant que transporteurId n'est pas peuplé
    // en amont (AffectationConfirmee le porte toujours à null aujourd'hui,
    // cf. AffectationL1Service côté service-opt — écart documenté).
    List<Mission> findByTransporteurIdAndTenantIdOrderByDateCreationDesc(UUID transporteurId, String tenantId);
}
