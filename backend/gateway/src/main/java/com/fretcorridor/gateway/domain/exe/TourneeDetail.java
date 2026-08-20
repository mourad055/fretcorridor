package com.fretcorridor.gateway.domain.exe;

import java.util.List;

/** S11 : ordre planifié complet d'une Tournée consolidée (LTL). */
public record TourneeDetail(String tourneeId, List<EtapeTournee> etapes) {
}
