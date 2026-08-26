package com.fretcorridor.gateway.infrastructure.rest.kyc.dto;

import com.fretcorridor.gateway.domain.kyc.KycDetail;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record KycDetailResponse(
        String id,
        String telephone,
        String nom,
        String prenom,
        String raisonSociale,
        String niveauKyc,
        Set<String> roles,
        List<PieceResponse> pieces
) {
    public record PieceResponse(String typeDocument, String url, LocalDateTime dateDepot) {}

    public static KycDetailResponse from(KycDetail detail) {
        List<PieceResponse> pieces = detail.pieces().stream()
                .map(p -> new PieceResponse(p.typeDocument(), p.url(), p.dateDepot()))
                .toList();
        return new KycDetailResponse(
                detail.id(),
                detail.telephone(),
                detail.nom(),
                detail.prenom(),
                detail.raisonSociale(),
                detail.niveauKyc(),
                detail.roles(),
                pieces);
    }
}
