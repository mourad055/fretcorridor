package com.flysoft.fretcorridor.cap.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TarifChauffeurRepository extends JpaRepository<TarifChauffeur, UUID> {

    // "Mes tarifs" (app Chauffeur/Transporteur) - propres tarifs du
    // transporteur connecte, plus recents d'abord.
    List<TarifChauffeur> findByTransporteurIdAndTenantIdOrderByDateCreationDesc(UUID transporteurId, String tenantId);

    // Unicite applicative avant creation (en plus de la contrainte UNIQUE
    // en base, qui reste le vrai garde-fou contre une course).
    Optional<TarifChauffeur> findByTransporteurIdAndAxeIdAndCategorieMarchandise(
            UUID transporteurId, UUID axeId, CategorieMarchandise categorieMarchandise);

    // Lecture interne par OPT (L1, un candidat a la fois) - seul un tarif
    // actif peut etre applique au calcul de prix.
    Optional<TarifChauffeur> findByTransporteurIdAndAxeIdAndCategorieMarchandiseAndActifTrue(
            UUID transporteurId, UUID axeId, CategorieMarchandise categorieMarchandise);

    // Agregat pour l'intervalle affiche au client - tous les chauffeurs
    // actuellement actifs sur cet axe et cette categorie.
    List<TarifChauffeur> findByAxeIdAndCategorieMarchandiseAndActifTrue(UUID axeId, CategorieMarchandise categorieMarchandise);
}
