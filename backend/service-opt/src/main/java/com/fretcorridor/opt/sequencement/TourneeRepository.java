package com.fretcorridor.opt.sequencement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TourneeRepository extends JpaRepository<Tournee, UUID> {
    List<Tournee> findByCapaciteIdAndStatut(UUID capaciteId, Tournee.Statut statut);

    // Sprint 12 (SequencementDeclencheur/ReplanificationService) : trouve une
    // tournee EN COURS (non TERMINEE) pour une capacite donnee - condition
    // pour choisir entre "creer une nouvelle Tournee" (aucune tournee active)
    // et "inserer dans le residuel" (une tournee CONFIRMEE ou EN_EXECUTION
    // existe deja). Sans cette methode, le declencheur creerait une seconde
    // Tournee en parallele sur la meme capacite - bug latent corrige ici.
    List<Tournee> findByCapaciteIdAndStatutIn(UUID capaciteId, List<Tournee.Statut> statuts);
}
