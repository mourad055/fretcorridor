package com.fretcorridor.gateway.infrastructure.opt;

import com.fretcorridor.gateway.domain.opt.PropositionMissionCandidate;
import com.fretcorridor.gateway.domain.opt.PropositionMissionIndisponibleException;
import com.fretcorridor.gateway.domain.opt.PropositionMissionPort;
import com.fretcorridor.gateway.domain.opt.PropositionMissionServiceIndisponibleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * UC-MAT-02 (CDC page 43, "Notification, acceptation ou refus d'une mission
 * par le chauffeur").
 *
 * ATTENTION -- ENTORSE ARCHITECTURALE TEMPORAIRE (26/08) : cet adaptateur
 * appelle service-opt (Moteur) en REST synchrone direct depuis la gateway
 * (Web), ce que shared-contracts/openapi/opt-api.yaml interdit explicitement
 * ("Aucun consommateur cross-porteur (Mobile/Web) n'appelle cette API
 * directement — le flux OPT -> Mobile passe exclusivement par événements
 * Kafka asynchrones") et que l'ADR 0013 documente comme decision d'equipe
 * (Plan d'Execution §4.3, asynchrone obligatoire entre porteurs differents).
 *
 * Assume ici pour livrer une demo fonctionnelle de bout en bout dans le
 * delai imparti (branche locale feature/retours-ux-27-08, jamais pousse sur
 * dev en l'etat). A REGULARISER avant tout merge, en coordination avec le
 * porteur Moteur -- patron cible : service-opt publie un evenement Kafka
 * (ex. PropositionMissionEmise) a la creation, un service porteur Mobile
 * (service-exe est le candidat naturel, deja proprietaire de "Mission" cote
 * chauffeur) le consomme et materialise un modele de lecture + relaie les
 * actions accepter/refuser -- meme patron que ADR 0013 (service-bur pour les
 * missions appariees) et ADR 0014 (positions).
 *
 * X-Internal-Service-Key (pas de JWT) : meme garde que AffectationController
 * cote service-opt, qui n'a aucune infrastructure JwtService -- la
 * resolution de "qui est le transporteur" reste faite ici, cote gateway
 * (JWT deja valide), puis transmise en clair (transporteurId) plutot que
 * de faire porter cette responsabilite a service-opt.
 */
@Component
public class RealPropositionMissionAdapter implements PropositionMissionPort {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;
    private final String cleInterne;

    public RealPropositionMissionAdapter(WebClient.Builder webClientBuilder,
                                          @Value("${fretcorridor.service-opt.base-url}") String baseUrl,
                                          @Value("${fretcorridor.internal.service-key}") String cleInterne) {
        HttpClient httpClient = HttpClient.create().responseTimeout(TIMEOUT);
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        this.cleInterne = cleInterne;
    }

    @Override
    public Flux<PropositionMissionCandidate> mesPropositions(UUID transporteurId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/opt/propositions-mission/mes")
                        .queryParam("transporteurId", transporteurId)
                        .build())
                .header("X-Internal-Service-Key", cleInterne)
                .retrieve()
                .bodyToFlux(PropositionMissionDto.class)
                .map(PropositionMissionDto::versCandidate)
                .onErrorMap(e -> !(e instanceof WebClientResponseException), e -> new PropositionMissionServiceIndisponibleException());
    }

    @Override
    public Mono<PropositionMissionCandidate> accepter(UUID propositionId, UUID transporteurId) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/opt/propositions-mission/{id}/accepter")
                        .queryParam("transporteurId", transporteurId)
                        .build(propositionId))
                .header("X-Internal-Service-Key", cleInterne)
                .retrieve()
                .bodyToMono(PropositionMissionDto.class)
                .map(PropositionMissionDto::versCandidate)
                .onErrorMap(this::estConflitOuExpire, this::versExceptionMetier)
                .onErrorMap(e -> !(e instanceof PropositionMissionIndisponibleException), e -> new PropositionMissionServiceIndisponibleException());
    }

    @Override
    public Mono<PropositionMissionCandidate> refuser(UUID propositionId, UUID transporteurId, String motif) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/opt/propositions-mission/{id}/refuser")
                        .queryParam("transporteurId", transporteurId)
                        .build(propositionId))
                .header("X-Internal-Service-Key", cleInterne)
                .bodyValue(Map.of("motif", motif != null ? motif : ""))
                .retrieve()
                .bodyToMono(PropositionMissionDto.class)
                .map(PropositionMissionDto::versCandidate)
                .onErrorMap(this::estConflitOuExpire, this::versExceptionMetier)
                .onErrorMap(e -> !(e instanceof PropositionMissionIndisponibleException), e -> new PropositionMissionServiceIndisponibleException());
    }

    private boolean estConflitOuExpire(Throwable e) {
        if (!(e instanceof WebClientResponseException wcre)) {
            return false;
        }
        HttpStatusCode statut = wcre.getStatusCode();
        return statut.value() == 409 || statut.value() == 410 || statut.value() == 404;
    }

    private PropositionMissionIndisponibleException versExceptionMetier(Throwable e) {
        return new PropositionMissionIndisponibleException(
                "Cette proposition n'est plus disponible (déjà répondue, expirée, ou introuvable).");
    }

    private record PropositionMissionDto(UUID id, UUID demandeId, BigDecimal prixTransport, String origineNom,
                                          String destinationNom, String typeEmballageNom, Integer quantite,
                                          Double poidsTotalKg, String destinataireNom, String destinataireTelephone,
                                          String modeCollecte, String typeDisponibilite, Double distanceMetres,
                                          Double dureeSecondes, Boolean grandeValeur, String statut,
                                          Instant expireA, Instant dateCreation) {
        PropositionMissionCandidate versCandidate() {
            return new PropositionMissionCandidate(id, demandeId, prixTransport, origineNom, destinationNom,
                    typeEmballageNom, quantite, poidsTotalKg, destinataireNom, destinataireTelephone,
                    modeCollecte, typeDisponibilite, distanceMetres, dureeSecondes, grandeValeur,
                    statut, expireA, dateCreation);
        }
    }
}
