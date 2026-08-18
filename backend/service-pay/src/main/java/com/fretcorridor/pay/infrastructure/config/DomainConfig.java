package com.fretcorridor.pay.infrastructure.config;

import com.fretcorridor.pay.domain.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class DomainConfig {

    @Bean
    public GrandLivreService grandLivreService(GrandLivrePort grandLivrePort, GarantiePort garantiePort,
                                                LitigeMissionPort litigeMissionPort, SequestrePort sequestrePort) {
        return new GrandLivreService(grandLivrePort, garantiePort, litigeMissionPort, sequestrePort);
    }

    @Bean
    public SequestreService sequestreService(SequestrePort sequestrePort) {
        return new SequestreService(sequestrePort);
    }

    @Bean
    public GarantieService garantieService(GarantiePort garantiePort) {
        return new GarantieService(garantiePort);
    }

    @Bean
    public ReversementAutomatiqueService reversementAutomatiqueService(
            SequestrePort sequestrePort,
            GrandLivreService grandLivreService,
            @Value("${fretcorridor.pay.ordonnanceur-reversement.delai-contestation-heures}") long delaiContestationHeures
    ) {
        return new ReversementAutomatiqueService(sequestrePort, grandLivreService, Duration.ofHours(delaiContestationHeures));
    }

    @Bean
    public PaiementEspecesService paiementEspecesService(DeclarationEspecesPort declarationEspecesPort) {
        return new PaiementEspecesService(declarationEspecesPort);
    }

    @Bean
    public ReconciliationService reconciliationService(GrandLivrePort grandLivrePort, PrestatairePaiementPort prestatairePaiementPort,
                                                         ReconciliationEventPort reconciliationEventPort) {
        return new ReconciliationService(grandLivrePort, prestatairePaiementPort, reconciliationEventPort);
    }

    @Bean
    public NotificationPrestataireService notificationPrestataireService(
            GrandLivreService grandLivreService,
            NotificationIdempotencePort notificationIdempotencePort,
            SignatureVerifierPort signatureVerifierPort
    ) {
        return new NotificationPrestataireService(grandLivreService, notificationIdempotencePort, signatureVerifierPort);
    }
}
