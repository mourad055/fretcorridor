package com.fretcorridor.gateway.infrastructure.rest.exe.dto;

import com.fretcorridor.gateway.domain.exe.TourneeDetail;
import java.util.List;

public record TourneeDetailResponse(String tourneeId, List<EtapeTourneeResponse> etapes) {
    public static TourneeDetailResponse from(TourneeDetail d) {
        return new TourneeDetailResponse(d.tourneeId(), d.etapes().stream().map(EtapeTourneeResponse::from).toList());
    }

    public record EtapeTourneeResponse(String missionId, int rang, String typeEtape, String demandeId,
                                        double pointLatitude, double pointLongitude,
                                        String fenetreDebut, String fenetreFin, String missionStatut) {
        public static EtapeTourneeResponse from(com.fretcorridor.gateway.domain.exe.EtapeTournee e) {
            return new EtapeTourneeResponse(e.missionId(), e.rang(), e.typeEtape(), e.demandeId(),
                    e.pointLatitude(), e.pointLongitude(), e.fenetreDebut(), e.fenetreFin(), e.missionStatut());
        }
    }
}
