package com.fretcorridor.gateway.domain.cap;

import java.math.BigDecimal;

/**
 * Intervalle de tarif (F/kg, pas encore multiplié par le poids) parmi les
 * chauffeurs actifs d'un axe+catégorie — plan §1bis. L'appelant (app client)
 * multiplie par le poids taxable pour afficher un prix indicatif.
 */
public record IntervalleTarif(BigDecimal tarifMinFcfaParKg, BigDecimal tarifMaxFcfaParKg, int nombreChauffeurs) {
}
