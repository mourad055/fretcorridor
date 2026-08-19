package com.fretcorridor.opt.messaging;

import java.util.UUID;

/** Miroir exact de LotPayload (shared-contracts/asyncapi/events/demande-publiee-lots.yaml, valide avec Mobile). */
public record LotPayload(
        UUID lotId,
        String typeCatalogue,
        Integer quantite,
        Double poidsKg,
        Double longueurM,
        Double largeurM,
        Double hauteurM,
        boolean gerbable,
        boolean fragile,
        String classeDanger
) {
}
