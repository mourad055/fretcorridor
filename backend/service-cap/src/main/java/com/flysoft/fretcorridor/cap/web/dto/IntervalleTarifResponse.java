package com.flysoft.fretcorridor.cap.web.dto;

import com.flysoft.fretcorridor.cap.domain.TarifChauffeurService;

import java.math.BigDecimal;

/**
 * Reponse de GET /api/cap/tarifs/intervalle -- intervalle de tarif (F/kg,
 * pas encore multiplie par le poids) parmi les chauffeurs actuellement
 * actifs pour un axe et une categorie. L'appelant (gateway/service-mkt)
 * multiplie par le poids taxable pour obtenir le prix final.
 */
public record IntervalleTarifResponse(BigDecimal tarifMinFcfaParKg, BigDecimal tarifMaxFcfaParKg, int nombreChauffeurs) {
    public static IntervalleTarifResponse from(TarifChauffeurService.IntervalleTarif i) {
        return new IntervalleTarifResponse(i.tarifMinFcfaParKg(), i.tarifMaxFcfaParKg(), i.nombreChauffeurs());
    }
}
