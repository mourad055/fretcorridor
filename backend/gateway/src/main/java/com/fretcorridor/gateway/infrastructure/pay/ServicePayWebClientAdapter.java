package com.fretcorridor.gateway.infrastructure.pay;

import com.fretcorridor.gateway.domain.exe.MissionIntrouvableException;
import com.fretcorridor.gateway.domain.pay.DeclarationEspecesVue;
import com.fretcorridor.gateway.domain.pay.EcritureVue;
import com.fretcorridor.gateway.domain.pay.ModePaiementChoisi;
import com.fretcorridor.gateway.domain.pay.PayReadPort;
import com.fretcorridor.gateway.domain.pay.PayServiceIndisponibleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    public Flux<EcritureVue> ecrituresDuTransporteur(String transporteurId, String delegationToken) {
        if (delegationToken == null) {
            return Flux.error(new PayServiceIndisponibleException());
        }
        return webClient.get()
                .uri("/api/v1/pay/transporteurs/{transporteurId}/ecritures", transporteurId)
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve()
                .bodyToFlux(EcritureVue.class);
    }

    @Override
    public Flux<EcritureVue> rapportDuTenant(String tenantId, String delegationToken) {
        if (delegationToken == null) {
            return Flux.error(new PayServiceIndisponibleException());
        }
        return webClient.get()
                .uri("/api/v1/pay/tenants/{tenantId}/rapport", tenantId)
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve()
                .bodyToFlux(EcritureVue.class);
    }

    @Override
    public Flux<DeclarationEspecesVue> paiementsEspecesDuTenant(String tenantId, String delegationToken) {
        if (delegationToken == null) {
            return Flux.error(new PayServiceIndisponibleException());
        }
        return webClient.get()
                .uri("/api/v1/pay/tenants/{tenantId}/paiements-especes", tenantId)
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve()
                .bodyToFlux(DeclarationEspecesVue.class);
    }

    @Override
    public Mono<ModePaiementChoisi> modePaiementChoisi(String missionId, String delegationToken) {
        if (delegationToken == null) {
            return Mono.error(new PayServiceIndisponibleException());
        }
        return webClient.get()
                .uri("/api/v1/pay/missions/{missionId}/moyen-paiement", missionId)
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve()
                .bodyToMono(ModePaiementChoisi.class)
                .onErrorMap(this::est404, e -> new MissionIntrouvableException());
    }

    private boolean est404(Throwable e) {
        return e instanceof WebClientResponseException wcre && wcre.getStatusCode().value() == 404;
    }
}
