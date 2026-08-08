package com.fretcorridor.pay.infrastructure.config;

import com.fretcorridor.pay.domain.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public GrandLivreService grandLivreService(GrandLivrePort grandLivrePort) {
        return new GrandLivreService(grandLivrePort);
    }

    @Bean
    public SequestreService sequestreService(SequestrePort sequestrePort) {
        return new SequestreService(sequestrePort);
    }

    @Bean
    public ReconciliationService reconciliationService(GrandLivrePort grandLivrePort, PrestatairePaiementPort prestatairePaiementPort) {
        return new ReconciliationService(grandLivrePort, prestatairePaiementPort);
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
