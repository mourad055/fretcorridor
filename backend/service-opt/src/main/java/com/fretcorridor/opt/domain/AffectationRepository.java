package com.fretcorridor.opt.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface AffectationRepository extends JpaRepository<Affectation, UUID> {

    /**
     * Coeur de la robustesse "premier arrive gagne" (diffusion-course) :
     * UPDATE conditionnel en une seule requete atomique, pas un
     * findById + verification + save en 3 etapes qui laisserait une fenetre
     * de course entre deux threads/instances traitant deux acceptations
     * simultanees pour la meme demande. Retourne le nombre de lignes
     * affectees : 1 = cette invocation a gagne la course, 0 = une autre
     * affectation de la meme demande a deja ete confirmee avant (ou celle-ci
     * n'existe plus a l'etat PROPOSEE) - l'appelant doit traiter 0 comme un
     * "trop tard", jamais une erreur.
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(
            "UPDATE Affectation a SET a.statut = com.fretcorridor.opt.domain.StatutAffectation.CONFIRMEE "
                    + "WHERE a.id = :id AND a.statut = com.fretcorridor.opt.domain.StatutAffectation.PROPOSEE")
    int confirmerSiProposee(java.util.UUID id);

    /**
     * Les autres propositions PROPOSEE de la meme demande, une fois l'une
     * d'elles confirmee (ou refusee) - a marquer EXPIREE pour que
     * "notification disparait chez les autres" (plan de reorientation) soit
     * reellement observable cote Mobile via un evenement, pas juste un etat
     * silencieux en base.
     */
    java.util.List<Affectation> findByDemandeIdAndStatut(UUID demandeId, StatutAffectation statut);

    // Point 5 (matrice compatibilite marchandises) : demandes deja CONFIMEES
    // sur une capacite, pour verifier en PRE-L1 qu'une nouvelle demande
    // candidate n'introduit pas de marchandise incompatible avec ce que la
    // capacite transporte deja.
    java.util.List<Affectation> findByCapaciteIdAndStatut(UUID capaciteId, StatutAffectation statut);

    // Diffusion-course (trouvaille Mobile) : propositions en attente d'un
    // transporteur - "mes propositions a traiter" cote app chauffeur.
    // S'appuie sur la denormalisation transporteur_id (V27) et l'index
    // (transporteur_id, statut). PROPOSEE uniquement : une proposition
    // CONFIRMEE (mission en cours) ou EXPIREE ne doit plus apparaitre dans
    // la liste "a traiter".
    java.util.List<Affectation> findByTransporteurIdAndStatut(UUID transporteurId, StatutAffectation statut);

    // Diffusion-course (timeout compte a rebours Mobile) : affectations PROPOSEE
    // dont le delai expireA est depasse. Consomme par ExpirationPropositionService
    // pour les passer EXPIREE (re-diffusion possible aux cycles suivants).
    java.util.List<Affectation> findByStatutAndExpireALessThan(StatutAffectation statut, Instant expireA);

    // findById(UUID) suffit pour l'instant : c'est exactement l'appel que TRK
    // fera avec le mission_id recu (a terme) via AffectationConfirmee cote
    // Mobile, ou directement connu du contexte appelant en Phase 1.

    // Sprint 11 (sequencement L2) : affectations confirmees pas encore
    // regroupees dans une Tournee. Requete ciblee plutot que findAll() +
    // filtre en memoire cote Java - evite de charger toute la table a
    // chaque cycle de SequencementDeclencheur.
    @org.springframework.data.jpa.repository.Query(
            "SELECT a FROM Affectation a WHERE a.id NOT IN "
                    + "(SELECT DISTINCT e.affectationId FROM com.fretcorridor.opt.sequencement.EtapeTournee e)")
    java.util.List<Affectation> findNonEncoreSequencees();
}
