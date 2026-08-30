package com.fretcorridor.opt.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DemandeEnAttenteRepository extends JpaRepository<DemandeEnAttente, UUID> {
    List<DemandeEnAttente> findByAxeIdAndTraiteeFalse(UUID axeId);

    // DemandeAnnuleeListener : retire une demande annulee de la file
    // d'attente avant qu'un cycle de matching ne la traite (traiteeFalse
    // uniquement - si deja traitee, une Affectation existe deja, trop tard
    // pour l'annuler cote Moteur).
    List<DemandeEnAttente> findByDemandeIdAndTraiteeFalse(UUID demandeId);

    // Diffusion-course : retrouver une demande quelle que soit sa valeur
    // traitee - necessaire au rematching apres refus de chauffeur (une
    // demande deja diffusee est traitee=true, mais on doit pouvoir la
    // remettre en file pour un prochain cycle).
    java.util.Optional<DemandeEnAttente> findByDemandeId(UUID demandeId);
}
