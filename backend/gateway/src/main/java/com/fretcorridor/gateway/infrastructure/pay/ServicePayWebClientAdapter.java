package com.fretcorridor.gateway.infrastructure.pay;

import com.fretcorridor.gateway.domain.pay.DeclarationEspecesVue;
import com.fretcorridor.gateway.domain.pay.EcritureVue;
import com.fretcorridor.gateway.domain.pay.PayReadPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

/**
 * Appelle le service réel service-pay (contrairement aux autres adaptateurs
 * de ce périmètre, ce n'est pas un mock : service-pay est implémenté,
 * cf. PRD Plan d'Exécution §4.3 — consultation synchrone via la gateway).
 */
@Component
public class ServicePayWebClientAdapter implements PayReadPort {

    private final WebClient webClient;

    public ServicePayWebClientAdapter(WebClient.Builder webClientBuilder, @Value("${fretcorridor.service-pay.base-url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public Flux<EcritureVue> ecrituresDuTransporteur(String transporteurId) {
        return webClient.get()
                .uri("/api/v1/pay/transporteurs/{transporteurId}/ecritures", transporteurId)
                .retrieve()
                .bodyToFlux(EcritureVue.class);
    }

    @Override
    public Flux<EcritureVue> rapportDuTenant(String tenantId) {
        return webClient.get()
                .uri("/api/v1/pay/tenants/{tenantId}/rapport", tenantId)
                .retrieve()
                .bodyToFlux(EcritureVue.class);
    }

    @Override
    public Flux<DeclarationEspecesVue> paiementsEspecesDuTenant(String tenantId) {
        return webClient.get()
                .uri("/api/v1/pay/tenants/{tenantId}/paiements-especes", tenantId)
                .retrieve()
                .bodyToFlux(DeclarationEspecesVue.class);
    }
}
