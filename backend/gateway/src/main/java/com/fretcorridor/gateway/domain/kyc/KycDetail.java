package com.fretcorridor.gateway.domain.kyc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** Détail d'un acteur pour revue KYC Admin (pièces MinIO présignées). */
public record KycDetail(
        String id,
        String telephone,
        String nom,
        String prenom,
        String raisonSociale,
        String niveauKyc,
        Set<String> roles,
        List<Piece> pieces
) {
    public record Piece(String typeDocument, String url, LocalDateTime dateDepot) {}
}
