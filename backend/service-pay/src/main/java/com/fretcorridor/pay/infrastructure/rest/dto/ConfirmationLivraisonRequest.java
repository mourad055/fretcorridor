package com.fretcorridor.pay.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * RG-078 : libère le séquestre indépendamment de tout encaissement — chemin
 * requis par le reversement adossé à une garantie tierce (EF-PAY-06 terme
 * contractuel), où aucun encaissement réel n'existe. {@code cloture()} reste
 * le raccourci encaissement+libération pour le cas standard ; cet endpoint
 * couvre le cas où seule la livraison doit être actée.
 */
public record ConfirmationLivraisonRequest(
        @NotBlank String tenantId,
        @NotBlank String transporteurId,
        @NotBlank String preuveLivraisonReference
) {
}
