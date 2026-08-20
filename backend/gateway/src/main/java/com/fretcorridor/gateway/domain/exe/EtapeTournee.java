package com.fretcorridor.gateway.domain.exe;

/**
 * S11 (EF-MAT-05/06) : une étape planifiée d'une Tournée consolidée (LTL),
 * telle qu'exposée par service-exe (TourneeConstitueeListener). missionStatut
 * (statut réel de la Mission à laquelle l'étape se rattache) permet de
 * dériver l'étape courante côté app sans dupliquer la logique de
 * progression déjà portée par Mission.statut.
 */
public record EtapeTournee(String missionId, int rang, String typeEtape, String demandeId,
                            double pointLatitude, double pointLongitude,
                            String fenetreDebut, String fenetreFin, String missionStatut) {
}
