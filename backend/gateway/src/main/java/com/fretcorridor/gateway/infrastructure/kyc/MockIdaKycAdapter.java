package com.fretcorridor.gateway.infrastructure.kyc;

import com.fretcorridor.gateway.domain.kyc.DecisionInvalideException;
import com.fretcorridor.gateway.domain.kyc.KycDossier;
import com.fretcorridor.gateway.domain.kyc.KycDossierIntrouvableException;
import com.fretcorridor.gateway.domain.kyc.KycPort;
import com.fretcorridor.gateway.domain.kyc.KycStatut;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO(mobile): remplacer par l'appel réel à service-ida (file de vérification KYC,
 * UC-IDA-01 étape 9) une fois ce service livré. Adaptateur mock documenté, conforme
 * à la règle "scope strict" (PRD §10.4).
 */
@Component
public class MockIdaKycAdapter implements KycPort {

    private final Map<String, KycDossier> dossiers = new ConcurrentHashMap<>();
    private final Map<String, KycDossier> resultatsParCleIdempotence = new ConcurrentHashMap<>();

    public MockIdaKycAdapter() {
        seed("kyc-1", "Jean Mbarga", "+237677000001", "CHAUFFEUR", 2);
        seed("kyc-2", "Transport Étoile SARL", "+237677000002", "TRANSPORTEUR_PERSONNE_MORALE", 5);
        seed("kyc-3", "Awa Ndiaye", "+237677000003", "CHAUFFEUR", 1);
    }

    private void seed(String id, String nom, String telephone, String type, long joursEcoules) {
        dossiers.put(id, new KycDossier(id, nom, telephone, type, Instant.now().minus(joursEcoules, ChronoUnit.DAYS), KycStatut.EN_ATTENTE));
    }

    @Override
    public Flux<KycDossier> listerEnAttente() {
        return Flux.fromIterable(dossiers.values())
                .filter(d -> d.statut() == KycStatut.EN_ATTENTE);
    }

    @Override
    public Mono<KycDossier> decider(String dossierId, KycStatut decision, String idempotencyKey) {
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

        KycDossier miseAJour = dossier.avecStatut(decision);
        dossiers.put(dossierId, miseAJour);
        resultatsParCleIdempotence.put(idempotencyKey, miseAJour);
        return Mono.just(miseAJour);
    }
}
