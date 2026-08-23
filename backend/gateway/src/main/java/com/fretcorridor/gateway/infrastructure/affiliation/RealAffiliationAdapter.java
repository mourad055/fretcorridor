package com.fretcorridor.gateway.infrastructure.affiliation;

import com.fretcorridor.gateway.domain.Actor;
import com.fretcorridor.gateway.domain.Role;
import com.fretcorridor.gateway.domain.affiliation.AffiliationPort;
import com.fretcorridor.gateway.domain.affiliation.AffiliationServiceIndisponibleException;
import com.fretcorridor.gateway.domain.affiliation.TenantNonAuthoriseException;
import com.fretcorridor.gateway.domain.affiliation.TenantOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Appelle service-ida (Mobile, port 8081) pour S18 — même principe que
 * RealIdaProfilAdapter (delegationToken de l'acteur, jamais le JWT gateway).
 */
@Component
public class RealAffiliationAdapter implements AffiliationPort {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final WebClient webClient;

    public RealAffiliationAdapter(WebClient.Builder webClientBuilder,
                                   @Value("${fretcorridor.service-ida.base-url}") String baseUrl) {
        HttpClient httpClient = HttpClient.create().responseTimeout(TIMEOUT);
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public Mono<List<TenantOption>> mesTenants(String delegationToken) {
        if (delegationToken == null) {
            return Mono.error(new AffiliationServiceIndisponibleException());
        }
        return webClient.get()
                .uri("/api/ida/affiliations/mes-tenants")
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve()
                .bodyToFlux(TenantDisponibleDto.class)
                .map(dto -> new TenantOption(dto.tenantId(), dto.origine()))
                .collectList()
                .onErrorMap(e -> new AffiliationServiceIndisponibleException());
    }

    @Override
    public Mono<Actor> selectionner(String delegationToken, String phone, Role role, String tenantIdChoisi) {
        if (delegationToken == null) {
            return Mono.error(new AffiliationServiceIndisponibleException());
        }
        return webClient.post()
                .uri("/api/ida/affiliations/selection")
                .headers(h -> h.setBearerAuth(delegationToken))
                .bodyValue(Map.of("tenantId", tenantIdChoisi))
                .retrieve()
                .bodyToMono(SelectionResponse.class)
                .map(r -> new Actor(r.acteurId(), phone, role, r.tenantId(), r.accessToken()))
                .onErrorMap(this::estRefus, e -> new TenantNonAuthoriseException())
                .onErrorMap(e -> !(e instanceof TenantNonAuthoriseException), e -> new AffiliationServiceIndisponibleException());
    }

    @Override
    public Mono<Void> inviter(String delegationToken, String telephoneTransporteur) {
        if (delegationToken == null) {
            return Mono.error(new AffiliationServiceIndisponibleException());
        }
        return webClient.post()
                .uri("/api/ida/affiliations")
                .headers(h -> h.setBearerAuth(delegationToken))
                .bodyValue(Map.of("telephone", telephoneTransporteur))
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorMap(e -> new AffiliationServiceIndisponibleException());
    }

    private boolean estRefus(Throwable e) {
        return e instanceof WebClientResponseException wcre && wcre.getStatusCode().value() == 400;
    }

    private record TenantDisponibleDto(String tenantId, boolean origine) {
    }

    private record SelectionResponse(String accessToken, String refreshToken, String acteurId,
                                      List<String> roles, String tenantId) {
    }
}
