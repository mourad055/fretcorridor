package com.fretcorridor.gateway.domain.ida;

/** Sous-ensemble du profil KYC niveau 1 tel qu'exposé par service-ida (RG-011). */
public record Profil(String acteurId, String type, String nom, String prenom, String raisonSociale, String niveauKyc) {
}
