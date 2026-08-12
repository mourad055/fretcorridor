package com.fretcorridor.gateway.infrastructure.rest.ida.dto;

import com.fretcorridor.gateway.domain.ida.Piece;
import com.fretcorridor.gateway.domain.ida.Profil;
import java.util.List;

public record ProfilResponse(String acteurId, String type, String nom, String prenom,
                              String raisonSociale, String niveauKyc, List<PieceResponse> pieces) {
    public static ProfilResponse from(Profil profil) {
        return new ProfilResponse(profil.acteurId(), profil.type(), profil.nom(), profil.prenom(),
                profil.raisonSociale(), profil.niveauKyc(), profil.pieces().stream().map(PieceResponse::from).toList());
    }

    public record PieceResponse(String typeDocument, String url, String dateDepot) {
        public static PieceResponse from(Piece piece) {
            return new PieceResponse(piece.typeDocument(), piece.url(), piece.dateDepot());
        }
    }
}
