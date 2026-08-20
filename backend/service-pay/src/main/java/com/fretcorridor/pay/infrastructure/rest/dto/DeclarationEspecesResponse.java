package com.fretcorridor.pay.infrastructure.rest.dto;

import com.fretcorridor.pay.domain.DeclarationEspeces;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * EF-PAY-07 : {@code protectionAssuree} est toujours {@code false} pour ce
 * mode — champ explicite plutôt qu'implicite pour que tout consommateur de
 * l'API (web, rapport) signale sans ambiguïté l'absence de protection au
 * lieu de la déduire silencieusement du type de la réponse.
 */
public record DeclarationEspecesResponse(
        String id,
        String missionId,
        BigDecimal montant,
        Instant declareeLe,
        boolean protectionAssuree
) {
    public static DeclarationEspecesResponse from(DeclarationEspeces d) {
        return new DeclarationEspecesResponse(d.id(), d.missionId(), d.montant(), d.declareeLe(), false);
    }
}
