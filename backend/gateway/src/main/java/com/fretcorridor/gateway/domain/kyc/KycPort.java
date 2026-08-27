package com.fretcorridor.gateway.domain.kyc;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Port hexagonal : file de revue KYC Admin → service-ida
 * (/api/ida/admin/kyc). Les endpoints mobiles /api/kyc/* ne passent pas ici.
 */
public interface KycPort {

    Flux<KycDossier> listerEnAttente(String tenantId, String delegationToken);

    Flux<KycDossier> listerParNiveau(String tenantId, String niveau, String delegationToken);

    Mono<KycDetail> detail(String acteurId, String tenantId, String delegationToken);

    Mono<KycDossier> decider(
            String dossierId,
            KycStatut decision,
            String idempotencyKey,
            String tenantId,
            String delegationToken,
            String motif);

    Mono<KycPieceContenu> telechargerPiece(
            String acteurId,
            String pieceId,
            String tenantId,
            String delegationToken);
}
