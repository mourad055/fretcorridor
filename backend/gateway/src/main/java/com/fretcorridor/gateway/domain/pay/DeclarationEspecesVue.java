package com.fretcorridor.gateway.domain.pay;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Vue en lecture seule d'un paiement en espèces déclaré (EF-PAY-07, S) —
 * jamais une écriture de grand livre, cf. {@link EcritureVue}.
 * {@code protectionAssuree} est toujours {@code false} pour ce mode.
 */
public record DeclarationEspecesVue(
        String id,
        String missionId,
        BigDecimal montant,
        Instant declareeLe,
        boolean protectionAssuree
) {
}
