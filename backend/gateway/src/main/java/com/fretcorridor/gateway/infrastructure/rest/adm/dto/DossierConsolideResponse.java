package com.fretcorridor.gateway.infrastructure.rest.adm.dto;

import com.fretcorridor.gateway.infrastructure.rest.exe.dto.MissionResponse;
import com.fretcorridor.gateway.infrastructure.rest.pay.dto.EcritureVueResponse;

import java.util.List;

/**
 * FE-ADM-02 : dossier consolidé — agrège en lecture le dossier (service-adm),
 * la chronologie de la mission concernée (service-exe, via ExePort mock) et
 * ses écritures de paiement (service-pay, réel). Cette composition est faite
 * ici, côté gateway, car seul le client web via la gateway peut consommer
 * plusieurs services de façon synchrone (PRD §4.2).
 */
public record DossierConsolideResponse(
        DossierResponse dossier,
        MissionResponse mission,
        List<EcritureVueResponse> ecritures
) {
}
