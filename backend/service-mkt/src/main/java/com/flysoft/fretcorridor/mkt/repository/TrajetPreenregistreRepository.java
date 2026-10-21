package com.flysoft.fretcorridor.mkt.repository;

import com.flysoft.fretcorridor.mkt.entity.TrajetPreenregistre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrajetPreenregistreRepository extends JpaRepository<TrajetPreenregistre, UUID> {
    List<TrajetPreenregistre> findByClientActeurIdAndTenantIdOrderByDateCreationDesc(UUID clientActeurId, String tenantId);

    // clientActeurId ET tenantId (pas seulement tenantId, contrairement a
    // DemandeRepository.findByIdAndTenantId) : un trajet preenregistre est
    // une donnee personnelle au client, jamais partagee entre clients d'un
    // meme tenant -- IDOR a eviter des la conception ici.
    Optional<TrajetPreenregistre> findByIdAndClientActeurIdAndTenantId(UUID id, UUID clientActeurId, String tenantId);
}
