package com.fretcorridor.gateway.infrastructure.ida;

import com.fretcorridor.gateway.domain.ida.IdaProfilPort;
import com.fretcorridor.gateway.domain.ida.Profil;
import com.fretcorridor.gateway.domain.ida.ProfilCompletionRefuseeException;
import com.fretcorridor.gateway.domain.ida.ProfilServiceIndisponibleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Appelle service-ida (Mobile, port 8081) pour la complétion du profil KYC
 * niveau 1 (RG-011, Sprint 2). Utilise le delegationToken de l'acteur — le JWT
 * service-ida retransmis par la gateway (cf. AuthenticatedActor, ADR double
 * autorité JWT) — jamais le JWT du gateway, que service-ida ne valide pas.
 * Même principe que ServiceIdaAuthenticationAdapter côté authentification.
 */
@Component
public class RealIdaProfilAdapter implements IdaProfilPort {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final WebClient webClient;

    public RealIdaProfilAdapter(WebClient.Builder webClientBuilder,
                                 @Value("${fretcorridor.service-ida.base-url}") String baseUrl) {
        HttpClient httpClient = HttpClient.create().responseTimeout(TIMEOUT);
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public Mono<Profil> profil(String delegationToken) {
        if (delegationToken == null) {
            return Mono.error(new ProfilServiceIndisponibleException());
        }
        return webClient.get()
                .uri("/api/kyc/profil")
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve()
                .bodyToMono(ProfilDto.class)
                .map(ProfilDto::versProfil)
                .transform(this::gererErreurs);
    }

    @Override
    public Mono<Profil> completerParticulier(String delegationToken, String nom, String prenom) {
        if (delegationToken == null) {
            return Mono.error(new ProfilServiceIndisponibleException());
        }
        return webClient.put()
                .uri("/api/kyc/profil/particulier")
                .headers(h -> h.setBearerAuth(delegationToken))
                .bodyValue(Map.of("nom", nom, "prenom", prenom))
                .retrieve()
                .bodyToMono(CompletionDto.class)
                .map(r -> r.profil().versProfil())
                .transform(this::gererErreurs);
    }

    @Override
    public Mono<Profil> completerEntreprise(String delegationToken, String raisonSociale, String numeroRegistreCommerce) {
        if (delegationToken == null) {
            return Mono.error(new ProfilServiceIndisponibleException());
        }
        Map<String, String> corps = new HashMap<>();
        corps.put("raisonSociale", raisonSociale);
        if (numeroRegistreCommerce != null) {
            corps.put("numeroRegistreCommerce", numeroRegistreCommerce);
        }
        return webClient.put()
                .uri("/api/kyc/profil/entreprise")
                .headers(h -> h.setBearerAuth(delegationToken))
                .bodyValue(corps)
                .retrieve()
                .bodyToMono(CompletionDto.class)
                .map(r -> r.profil().versProfil())
                .transform(this::gererErreurs);
    }

    private Mono<Profil> gererErreurs(Mono<Profil> mono) {
        return mono
                .onErrorMap(this::estRefus, e -> new ProfilCompletionRefuseeException(messageDe(e)))
                .onErrorMap(e -> !(e instanceof ProfilCompletionRefuseeException), e -> new ProfilServiceIndisponibleException());
    }

    private boolean estRefus(Throwable e) {
        return e instanceof WebClientResponseException wcre && wcre.getStatusCode().value() == 400;
    }

    private String messageDe(Throwable e) {
        return e instanceof WebClientResponseException wcre ? wcre.getResponseBodyAsString() : "Requête refusée";
    }

    private record ProfilDto(String acteurId, String type, String nom, String prenom,
                              String raisonSociale, String niveauKyc) {
        Profil versProfil() {
            return new Profil(acteurId, type, nom, prenom, raisonSociale, niveauKyc);
        }
    }

    private record CompletionDto(String accessToken, String refreshToken, ProfilDto profil) {
    }
}
