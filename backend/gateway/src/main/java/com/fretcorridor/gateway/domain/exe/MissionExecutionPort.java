package com.fretcorridor.gateway.domain.exe;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Exécution de mission côté chauffeur/transporteur (S7, EF-EXE-02/04) —
 * appel réel à service-exe, séparé d'ExePort (vue Bureau, encore mockée :
 * service-exe n'expose aucun équivalent "toutes les missions du tenant").
 *
 * Reste vide en pratique tant que transporteurId n'est pas peuplé en amont
 * (AffectationConfirmee le porte toujours à null aujourd'hui côté
 * service-opt) — écart documenté, pas un bug de cet adaptateur.
 */
public interface MissionExecutionPort {
    Flux<MissionExecution> mesMissions(String delegationToken);

    Mono<MissionExecutionDetail> chronologie(String delegationToken, String missionId);

    Mono<MissionExecutionDetail> ajouterEtape(String delegationToken, String missionId, String type,
                                               String libelle, String horodatageCapture);

    // S11 : ordre planifié de la tournée (multi-étapes, LTL consolidé).
    Mono<TourneeDetail> tournee(String delegationToken, String tourneeId);
}
