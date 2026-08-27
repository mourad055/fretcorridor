package com.fretcorridor.gateway.infrastructure.kyc;

import com.fretcorridor.gateway.domain.kyc.DecisionInvalideException;
import com.fretcorridor.gateway.domain.kyc.KycDetail;
import com.fretcorridor.gateway.domain.kyc.KycDossier;
import com.fretcorridor.gateway.domain.kyc.KycDossierIntrouvableException;
import com.fretcorridor.gateway.domain.kyc.KycPieceContenu;
import com.fretcorridor.gateway.domain.kyc.KycPort;
import com.fretcorridor.gateway.domain.kyc.KycStatut;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixture de test uniquement — vit dans src/test/java (même mécanisme que
 * MockGeoAdapter / MockIdaAuthenticationAdapter). @Primary lève l'ambiguïté
 * avec RealIdaKycAdapter pendant les @SpringBootTest.
 */
@Component
@Primary
public class MockIdaKycAdapter implements KycPort {

    private final Map<String, KycDossier> dossiers = new ConcurrentHashMap<>();
    private final Map<String, KycDossier> resultatsParCleIdempotence = new ConcurrentHashMap<>();
    private final Map<String, KycDetail> details = new ConcurrentHashMap<>();

    public MockIdaKycAdapter() {
        seed("kyc-1", "Jean Mbarga", "+237677000001", "CHAUFFEUR", "NIVEAU_1", 2);
        seed("kyc-2", "Transport Étoile SARL", "+237677000002", "TRANSPORTEUR", "NIVEAU_1", 5);
        seed("kyc-3", "Awa Ndiaye", "+237677000003", "CHAUFFEUR", "NIVEAU_1", 1);
        seedNiveau2("kyc-n2", "Validé SARL", "+237677000099", "TRANSPORTEUR");
    }

    private void seed(String id, String nom, String telephone, String type, String niveau, long joursEcoules) {
        dossiers.put(id, new KycDossier(
                id, nom, telephone, type,
                Instant.now().minus(joursEcoules, ChronoUnit.DAYS),
                KycStatut.EN_ATTENTE, niveau, Set.of(type)));
        details.put(id, new KycDetail(
                id, telephone, nom, null, null, niveau, Set.of(type),
                List.of(new KycDetail.Piece("piece-" + id, "CNI", "https://example.test/cni-" + id, null))));
    }

    private void seedNiveau2(String id, String nom, String telephone, String type) {
        dossiers.put(id, new KycDossier(
                id, nom, telephone, type, Instant.now().minus(10, ChronoUnit.DAYS),
                KycStatut.VALIDE, "NIVEAU_2", Set.of(type)));
        details.put(id, new KycDetail(
                id, telephone, nom, null, nom, "NIVEAU_2", Set.of(type),
                List.of(new KycDetail.Piece("piece-" + id, "RCCM", "https://example.test/rccm-" + id, null))));
    }

    @Override
    public Flux<KycDossier> listerEnAttente(String tenantId, String delegationToken) {
        return Flux.fromIterable(dossiers.values())
                .filter(d -> d.statut() == KycStatut.EN_ATTENTE)
                .filter(d -> !"NIVEAU_2".equals(d.niveauKyc()));
    }

    @Override
    public Flux<KycDossier> listerParNiveau(String tenantId, String niveau, String delegationToken) {
        return Flux.fromIterable(dossiers.values())
                .filter(d -> niveau.equals(d.niveauKyc()));
    }

    @Override
    public Mono<KycDetail> detail(String acteurId, String tenantId, String delegationToken) {
        KycDetail detail = details.get(acteurId);
        if (detail == null) {
            return Mono.error(new KycDossierIntrouvableException(acteurId));
        }
        return Mono.just(detail);
    }

    @Override
    public Mono<KycDossier> decider(
            String dossierId,
            KycStatut decision,
            String idempotencyKey,
            String tenantId,
            String delegationToken,
            String motif) {
        if (decision == KycStatut.EN_ATTENTE) {
            return Mono.error(new DecisionInvalideException("La décision doit être VALIDE ou REJETE"));
        }

        KycDossier dejaTraite = resultatsParCleIdempotence.get(idempotencyKey);
        if (dejaTraite != null) {
            return Mono.just(dejaTraite);
        }

        KycDossier dossier = dossiers.get(dossierId);
        if (dossier == null) {
            return Mono.error(new KycDossierIntrouvableException(dossierId));
        }

        String niveau = decision == KycStatut.VALIDE ? "NIVEAU_2" : "NIVEAU_1";
        KycDossier miseAJour = dossier.avecNiveau(niveau, decision);
        dossiers.put(dossierId, miseAJour);
        resultatsParCleIdempotence.put(idempotencyKey, miseAJour);
        return Mono.just(miseAJour);
    }

    @Override
    public Mono<KycPieceContenu> telechargerPiece(
            String acteurId,
            String pieceId,
            String tenantId,
            String delegationToken) {
        KycDetail detail = details.get(acteurId);
        if (detail == null || detail.pieces().stream().noneMatch(p -> p.id().equals(pieceId))) {
            return Mono.error(new KycDossierIntrouvableException(pieceId));
        }
        byte[] png1x1 = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
            (byte) 0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41, 0x54, 0x78, (byte) 0x9C, 0x63, 0x00, 0x01, 0x00,
            0x00, 0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
            (byte) 0xAE, 0x42, 0x60, (byte) 0x82
        };
        return Mono.just(new KycPieceContenu("image/png", png1x1));
    }
}
