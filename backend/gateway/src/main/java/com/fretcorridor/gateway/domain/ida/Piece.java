package com.fretcorridor.gateway.domain.ida;

/** Pièce justificative KYC déposée (EF-IDA-03) — url présignée, à durée limitée. */
public record Piece(String typeDocument, String url, String dateDepot) {
}
