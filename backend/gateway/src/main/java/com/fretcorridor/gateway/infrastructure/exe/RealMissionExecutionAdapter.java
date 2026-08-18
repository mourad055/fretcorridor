package com.fretcorridor.gateway.infrastructure.exe;

import com.fretcorridor.gateway.domain.exe.EtapeExecution;
import com.fretcorridor.gateway.domain.exe.EtapeRefuseeException;
import com.fretcorridor.gateway.domain.exe.ExeServiceIndisponibleException;
import com.fretcorridor.gateway.domain.exe.MissionExecution;
import com.fretcorridor.gateway.domain.exe.MissionExecutionDetail;
import com.fretcorridor.gateway.domain.exe.MissionExecutionPort;
import com.fretcorridor.gateway.domain.exe.MissionIntrouvableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Appelle service-exe (Mobile, port 8093) pour l'exécution de mission côté
 * chauffeur (S7, EF-EXE-02/04). Même principe de delegationToken que les
 * autres adaptateurs Mobile — service-exe valide les JWT service-ida.
 */
@Component
public class RealMissionExecutionAdapter implements MissionExecutionPort {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;

    public RealMissionExecutionAdapter(WebClient.Builder webClientBuilder,
                                        @Value("${fretcorridor.service-exe.base-url}") String baseUrl) {
        HttpClient httpClient = HttpClient.create().responseTimeout(TIMEOUT);
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public Flux<MissionExecution> mesMissions(String delegationToken) {
        if (delegationToken == null) {
            return Flux.error(new ExeServiceIndisponibleException());
        }
        return webClient.get()
                .uri("/api/missions/mes")
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve()
                .bodyToFlux(MissionResumeDto.class)
                .map(MissionResumeDto::versMissionExecution)
                .onErrorMap(e -> new ExeServiceIndisponibleException());
    }

    @Override
    public Mono<MissionExecutionDetail> chronologie(String delegationToken, String missionId) {
        if (delegationToken == null) {
            return Mono.error(new ExeServiceIndisponibleException());
        }
        return webClient.get()
                .uri("/api/missions/{id}", missionId)
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve()
                .bodyToMono(ChronologieDto.class)
                .map(ChronologieDto::versDetail)
                .transform(this::gererErreurs);
    }

    @Override
    public Mono<MissionExecutionDetail> ajouterEtape(String delegationToken, String missionId, String type,
                                                       String libelle, String horodatageCapture) {
        if (delegationToken == null) {
            return Mono.error(new ExeServiceIndisponibleException());
        }
        Map<String, String> corps = new java.util.HashMap<>();
        corps.put("type", type);
        corps.put("libelle", libelle);
        if (horodatageCapture != null) {
            corps.put("horodatageCapture", horodatageCapture);
        }
        return webClient.post()
                .uri("/api/missions/{id}/etapes", missionId)
                .headers(h -> h.setBearerAuth(delegationToken))
                .bodyValue(corps)
                .retrieve()
                .bodyToMono(ChronologieDto.class)
                .map(ChronologieDto::versDetail)
                .transform(this::gererErreurs);
    }

    private Mono<MissionExecutionDetail> gererErreurs(Mono<MissionExecutionDetail> mono) {
        return mono
                .onErrorMap(this::est404, e -> new MissionIntrouvableException())
                .onErrorMap(this::est400, e -> new EtapeRefuseeException(messageDe(e)))
                .onErrorMap(e -> !(e instanceof MissionIntrouvableException) && !(e instanceof EtapeRefuseeException),
                        e -> new ExeServiceIndisponibleException());
    }

    private boolean est400(Throwable e) {
        return e instanceof WebClientResponseException wcre && wcre.getStatusCode().value() == 400;
    }

    private boolean est404(Throwable e) {
        return e instanceof WebClientResponseException wcre && wcre.getStatusCode().value() == 404;
    }

    private String messageDe(Throwable e) {
        return e instanceof WebClientResponseException wcre ? wcre.getResponseBodyAsString() : "Requête refusée";
    }

    private record MissionResumeDto(String missionId, String statut, String origineNom, String destinationNom,
                                     String dateCreation) {
        MissionExecution versMissionExecution() {
            return new MissionExecution(missionId, statut, origineNom, destinationNom, dateCreation);
        }
    }

    private record EtapeDto(String type, String libelle, String horodatageCapture, String horodatageTransmission) {
        EtapeExecution versEtape() {
            return new EtapeExecution(type, libelle, horodatageCapture, horodatageTransmission);
        }
    }

    private record ChronologieDto(String missionId, String statut, List<EtapeDto> etapes) {
        MissionExecutionDetail versDetail() {
            List<EtapeExecution> mapped = etapes == null ? List.of() : etapes.stream().map(EtapeDto::versEtape).toList();
            return new MissionExecutionDetail(missionId, statut, mapped);
        }
    }
}
