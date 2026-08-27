package com.fretcorridor.gateway.domain.kyc;

/** Contenu binaire d'une pièce KYC servi via proxy (évite les URLs MinIO présignées). */
public record KycPieceContenu(String contentType, byte[] donnees) {}
