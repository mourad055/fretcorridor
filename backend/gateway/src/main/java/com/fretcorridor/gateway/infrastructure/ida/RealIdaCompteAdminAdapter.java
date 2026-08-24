package com.fretcorridor.gateway.infrastructure.ida;

import com.fretcorridor.gateway.domain.ida.CompteAdmin;
import com.fretcorridor.gateway.domain.ida.CompteAdminServiceIndisponibleException;
import com.fretcorridor.gateway.domain.ida.CompteIntrouvableException;
import com.fretcorridor.gateway.domain.ida.IdaCompteAdminPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * Appelle service-ida (Mobile, port 8081) pour la gestion des comptes par un
 * Admin (audit UX 2026-08-23, §1.1) — même principe que RealIdaProfilAdapter
 * (delegationToken de l'acteur, jamais le JWT gateway).
 */
@Component
public class RealIdaCompteAdminAdapter implements IdaCompteAdminPort {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final WebClient webClient;

    public RealIdaCompteAdminAdapter(WebClient.Builder webClientBuilder,
                                      @Value("${fretcorridor.service-ida.base-url}") String baseUrl) {
        HttpClient httpClient = HttpClient.create().responseTimeout(TIMEOUT);
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public Flux<CompteAdmin> listerParTenant(String tenantId, String delegationToken) {
        if (delegationToken == null) {
            return Flux.error(new CompteAdminServiceIndisponibleException());
        }
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/ida/comptes").queryParam("tenantId", tenantId).build())
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve()
                .bodyToFlux(CompteDto.class)
                .map(CompteDto::versCompteAdmin)
                .onErrorMap(e -> !(e instanceof CompteIntrouvableException), e -> new CompteAdminServiceIndisponibleException());
    }

    @Override
    public Mono<CompteAdmin> changerStatut(String compteId, String tenantId, boolean actif, String delegationToken) {
        if (delegationToken == null) {
            return Mono.error(new CompteAdminServiceIndisponibleException());
        }
        return webClient.put()
                .uri(uriBuilder -> uriBuilder.path("/api/ida/comptes/{id}/statut").queryParam("tenantId", tenantId).build(compteId))
                .headers(h -> h.setBearerAuth(delegationToken))
                .bodyValue(Map.of("actif", actif))
                .retrieve()
                .bodyToMono(CompteDto.class)
                .map(CompteDto::versCompteAdmin)
                .transform(this::gererErreurs);
    }

    @Override
    public Mono<CompteAdmin> changerRoles(String compteId, String tenantId, Set<String> roles, String delegationToken) {
        if (delegationToken == null) {
            return Mono.error(new CompteAdminServiceIndisponibleException());
        }
        return webClient.put()
                .uri(uriBuilder -> uriBuilder.path("/api/ida/comptes/{id}/roles").queryParam("tenantId", tenantId).build(compteId))
                .headers(h -> h.setBearerAuth(delegationToken))
                .bodyValue(Map.of("roles", roles))
                .retrieve()
                .bodyToMono(CompteDto.class)
                .map(CompteDto::versCompteAdmin)
                .transform(this::gererErreurs);
    }

    private Mono<CompteAdmin> gererErreurs(Mono<CompteAdmin> mono) {
        return mono
                .onErrorMap(this::est404, e -> new CompteIntrouvableException())
                .onErrorMap(e -> !(e instanceof CompteIntrouvableException), e -> new CompteAdminServiceIndisponibleException());
    }

    private boolean est404(Throwable e) {
        return e instanceof WebClientResponseException wcre && wcre.getStatusCode().value() == 404;
    }

    private record CompteDto(String id, String telephone, String nom, String prenom, String raisonSociale,
                              String tenantId, Set<String> roles, boolean actif, String niveauKyc) {
        CompteAdmin versCompteAdmin() {
            return new CompteAdmin(id, telephone, nom, prenom, raisonSociale, tenantId, roles, actif, niveauKyc);
        }
    }
}
