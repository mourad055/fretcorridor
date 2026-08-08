package com.fretcorridor.mat.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Requete de calcul de cout composite pour un lot de candidats face a une
 * meme demande - UN SEUL appel HTTP pour tout le lot post-filtrage L0, jamais
 * un appel par paire : indispensable pour rester dans le budget de latence L0
 * ~50ms d'OPT (ENF-PRF-01/02), un aller-retour reseau par candidat serait
 * incompatible avec ce budget des que le lot depasse quelques elements.
 *
 * @Valid sur la liste : necessaire pour que la validation cascade dans chaque
 * PaireCandidatRequest, sinon seule la liste elle-meme (non-vide, taille) est
 * verifiee, pas son contenu.
 */
public record CoutLotRequest(

        @NotNull
        UUID demandeId,

        // Nullable : une demande sans axe connu retombe sur le modele de
        // ponderation par defaut cote CoutCompositeService (RG-106).
        UUID axeId,

        @NotNull
        @NotEmpty
        @Size(max = 200, message = "200 candidats maximum par lot")
        @Valid
        List<PaireCandidatRequest> candidats
) {
}
