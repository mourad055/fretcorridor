package com.fretcorridor.gateway.domain.pay;

import java.math.BigDecimal;
import java.time.Instant;

/** Vue en lecture seule d'une écriture du grand livre miroir de service-pay. */
public record EcritureVue(
        String id,
        String missionId,
        String typeCompte,
        String nature,
        String sens,
        BigDecimal montant,
        Instant creeLe,
        String statut
) {
}
