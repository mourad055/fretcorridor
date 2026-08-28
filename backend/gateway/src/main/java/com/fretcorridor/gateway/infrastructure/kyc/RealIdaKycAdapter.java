package com.fretcorridor.gateway.infrastructure.kyc;

import com.fretcorridor.gateway.domain.kyc.DecisionInvalideException;
import com.fretcorridor.gateway.domain.kyc.KycDetail;
import com.fretcorridor.gateway.domain.kyc.KycDossier;
import com.fretcorridor.gateway.domain.kyc.KycDossierIntrouvableException;
import com.fretcorridor.gateway.domain.kyc.KycPieceContenu;
import com.fretcorridor.gateway.domain.kyc.KycPort;
import com.fretcorridor.gateway.domain.kyc.KycServiceIndisponibleException;
import com.fretcorridor.gateway.domain.kyc.KycStatut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Appelle service-ida (/api/ida/admin/kyc) pour la revue KYC Admin —
 * même principe que RealIdaCompteAdminAdapter (delegationToken, jamais le JWT gateway).
 */
@Component
public class RealIdaKycAdapter implements KycPort {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final WebClient webClient;
    /** Cache local d'idempotence (ENF-SEC / mutations) — IDA ne stocke pas la clé. */
    private final Map<String, KycDossier> resultatsParCleIdempotence = new ConcurrentHashMap<>();

    public RealIdaKycAdapter(WebClient.Builder webClientBuilder,
                             @Value("${fretcorridor.service-ida.base-url}") String baseUrl) {
        HttpClient httpClient = HttpClient.create().responseTimeout(TIMEOUT);
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public Flux<KycDossier> listerEnAttente(String tenantId, String delegationToken) {
        if (delegationToken == null) {
            return Flux.error(new KycServiceIndisponibleException());
        }
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/ida/admin/kyc/pending").queryParam("tenantId", tenantId).build())
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve()
                .bodyToFlux(ActeurSummaryDto.class)
                .map(dto -> dto.versDossier(KycStatut.EN_ATTENTE))
                .onErrorMap(e -> !(e instanceof KycDossierIntrouvableException), e -> new KycServiceIndisponibleException());
    }

    @Override
    public Flux<KycDossier> listerParNiveau(String tenantId, String niveau, String delegationToken) {
        if (delegationToken == null) {
            return Flux.error(new KycServiceIndisponibleException());
        }
        KycStatut statutAffiche = "NIVEAU_2".equals(niveau) ? KycStatut.VALIDE : KycStatut.EN_ATTENTE;
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/ida/admin/kyc")
                        .queryParam("tenantId", tenantId)
                        .queryParam("niveau", niveau)
                        .build())
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve()
                .bodyToFlux(ActeurSummaryDto.class)
                .map(dto -> dto.versDossier(statutAffiche))
                .onErrorMap(e -> !(e instanceof KycDossierIntrouvableException), e -> new KycServiceIndisponibleException());
    }

    @Override
    public Mono<KycDetail> detail(String acteurId, String tenantId, String delegationToken) {
        if (delegationToken == null) {
            return Mono.error(new KycServiceIndisponibleException());
        }
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/ida/admin/kyc/{id}").queryParam("tenantId", tenantId).build(acteurId))
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve()
                .bodyToMono(ActeurDetailDto.class)
                .map(ActeurDetailDto::versDetail)
                .onErrorMap(this::est404, e -> new KycDossierIntrouvableException(acteurId))
                .onErrorMap(e -> !(e instanceof KycDossierIntrouvableException), e -> new KycServiceIndisponibleException());
    }

    @Override
    public Mono<KycPieceContenu> telechargerPiece(
            String acteurId,
            String pieceId,
            String tenantId,
            String delegationToken) {
        if (delegationToken == null) {
            return Mono.error(new KycServiceIndisponibleException());
        }
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/ida/admin/kyc/{acteurId}/pieces/{pieceId}/content")
                        .queryParam("tenantId", tenantId)
                        .build(acteurId, pieceId))
                .headers(h -> h.setBearerAuth(delegationToken))
                .exchangeToMono(response -> {
                    if (response.statusCode().value() == 404) {
                        return Mono.error(new KycDossierIntrouvableException(pieceId));
                    }
                    if (response.statusCode().isError()) {
                        return Mono.error(new KycServiceIndisponibleException());
                    }
                    MediaType contentType = response.headers().contentType().orElse(MediaType.APPLICATION_OCTET_STREAM);
                    return response.bodyToMono(byte[].class)
                            .map(body -> new KycPieceContenu(contentType.toString(), body));
                })
                .onErrorMap(e -> !(e instanceof KycDossierIntrouvableException), e -> new KycServiceIndisponibleException());
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
        if (delegationToken == null) {
            return Mono.error(new KycServiceIndisponibleException());
        }

        KycDossier dejaTraite = resultatsParCleIdempotence.get(idempotencyKey);
        if (dejaTraite != null) {
            return Mono.just(dejaTraite);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("decision", decision.name());
        if (motif != null && !motif.isBlank()) {
            body.put("motif", motif);
        }

        return webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/ida/admin/kyc/{id}/decision")
                        .queryParam("tenantId", tenantId)
                        .build(dossierId))
                .headers(h -> h.setBearerAuth(delegationToken))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(ActeurSummaryDto.class)
                .map(dto -> dto.versDossier(decision))
                .doOnNext(dossier -> resultatsParCleIdempotence.put(idempotencyKey, dossier))
                .onErrorMap(this::est404, e -> new KycDossierIntrouvableException(dossierId))
                .onErrorMap(this::est400Decision, e -> new DecisionInvalideException("La décision doit être VALIDE ou REJETE"))
                .onErrorMap(e -> !(e instanceof KycDossierIntrouvableException)
                                && !(e instanceof DecisionInvalideException),
                        e -> new KycServiceIndisponibleException());
    }

    private boolean est404(Throwable e) {
        return e instanceof WebClientResponseException wcre && wcre.getStatusCode().value() == 404;
    }

    private boolean est400Decision(Throwable e) {
        return e instanceof WebClientResponseException wcre && wcre.getStatusCode().value() == 400;
    }

    private record ActeurSummaryDto(
            String acteurId,
            String telephone,
            String nom,
            String prenom,
            String raisonSociale,
            String niveauKyc,
            Set<String> roles
    ) {
        KycDossier versDossier(KycStatut statut) {
            return new KycDossier(
                    acteurId,
                    nomAffiche(),
                    telephone,
                    typeActeur(),
                    Instant.EPOCH,
                    statut,
                    niveauKyc,
                    roles == null ? Set.of() : roles);
        }

        private String nomAffiche() {
            if (raisonSociale != null && !raisonSociale.isBlank()) {
                return raisonSociale;
            }
            String n = (nom == null ? "" : nom).trim();
            String p = (prenom == null ? "" : prenom).trim();
            String compose = (n + " " + p).trim();
            return compose.isEmpty() ? telephone : compose;
        }

        private String typeActeur() {
            if (roles == null || roles.isEmpty()) {
                return "INCONNU";
            }
            List<String> priorite = List.of("CHAUFFEUR_PROPRIETAIRE", "CHAUFFEUR", "TRANSPORTEUR");
            for (String role : priorite) {
                if (roles.contains(role)) {
                    return role;
                }
            }
            return roles.iterator().next();
        }
    }

    private record ActeurDetailDto(
            String acteurId,
            String telephone,
            String nom,
            String prenom,
            String raisonSociale,
            String niveauKyc,
            Set<String> roles,
            List<PieceDto> pieces
    ) {
        KycDetail versDetail() {
            List<KycDetail.Piece> mapped = pieces == null ? List.of() : pieces.stream()
                    .map(p -> new KycDetail.Piece(p.id(), p.typeDocument(), p.url(), p.dateDepot()))
                    .collect(Collectors.toList());
            return new KycDetail(
                    acteurId,
                    telephone,
                    nom,
                    prenom,
                    raisonSociale,
                    niveauKyc,
                    roles == null ? Set.of() : roles,
                    mapped);
        }
    }

    private record PieceDto(String id, String typeDocument, String url, LocalDateTime dateDepot) {}
}
