package com.fretcorridor.gateway.infrastructure.auth;

import com.fretcorridor.gateway.domain.Actor;
import com.fretcorridor.gateway.domain.AuthenticationPort;
import com.fretcorridor.gateway.domain.AuthenticationServiceUnavailableException;
import com.fretcorridor.gateway.domain.InvalidCredentialsException;
import com.fretcorridor.gateway.domain.Role;
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
import java.util.Optional;

/**
 * Appelle service-ida (Mobile, port 8081) pour valider les identifiants.
 * service-ida signe son propre JWT avec son propre secret — jamais utilisé
 * ici : la gateway ne s'en sert que pour authentifier, puis émet son propre
 * JWT via JwtService, exactement comme avec le mock qu'il remplace (cf.
 * AuthController, inchangé).
 *
 * service-ida expose téléphone + code PIN, pas téléphone + code à usage
 * unique (OTP) comme le voudrait EF-IDA-01 du CDC — écart connu, documenté
 * séparément (docs/ROADMAP_INTEGRATION_gateway.md) : on s'adapte ici au
 * contrat réel existant plutôt que d'inventer un flux qui n'existe pas.
 */
@Component
public class ServiceIdaAuthenticationAdapter implements AuthenticationPort {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);
    private static final List<String> ROLES_GATEWAY_CONNUS = List.of("BUREAU", "TRANSPORTEUR", "ADMINISTRATION");

    private final WebClient webClient;

    public ServiceIdaAuthenticationAdapter(WebClient.Builder webClientBuilder,
                                            @Value("${fretcorridor.service-ida.base-url}") String baseUrl) {
        HttpClient httpClient = HttpClient.create().responseTimeout(TIMEOUT);
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public Mono<Actor> authenticate(String phone, String code) {
        return webClient.post()
                .uri("/api/auth/login")
                .bodyValue(Map.of("telephone", phone, "codePin", code))
                .retrieve()
                .bodyToMono(LoginResponse.class)
                .flatMap(response -> versActeur(phone, response)
                        .map(Mono::just)
                        .orElseGet(() -> Mono.error(new InvalidCredentialsException())))
                .onErrorMap(this::estNonAutorise, e -> new InvalidCredentialsException())
                // Tout le reste (timeout, connexion refusée, réponse malformée, 5xx...)
                // signale un service-ida indisponible, jamais un problème d'identifiants.
                .onErrorMap(e -> !(e instanceof InvalidCredentialsException), e -> new AuthenticationServiceUnavailableException());
    }

    private boolean estNonAutorise(Throwable e) {
        return e instanceof WebClientResponseException wcre && wcre.getStatusCode().value() == 401;
    }

    /**
     * service-ida peut renvoyer plusieurs rôles métier (ex. CHAUFFEUR) que la
     * gateway ne gère pas ; on retient le premier rôle qu'elle reconnaît. Un
     * acteur sans rôle reconnu est traité comme un identifiant invalide — ne
     * pas distinguer les deux cas, pour ne rien révéler à un attaquant sur
     * l'existence du compte.
     */
    private Optional<Actor> versActeur(String phone, LoginResponse response) {
        return response.roles().stream()
                .filter(ROLES_GATEWAY_CONNUS::contains)
                .findFirst()
                .map(this::mapperRole)
                .map(role -> new Actor(response.acteurId(), phone, role, response.tenantId()));
    }

    private Role mapperRole(String role) {
        return "ADMINISTRATION".equals(role) ? Role.ADMIN : Role.valueOf(role);
    }

    private record LoginResponse(String accessToken, String refreshToken, String acteurId, List<String> roles, String tenantId) {
    }
}
