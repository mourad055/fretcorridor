package com.fretcorridor.gateway.infrastructure.exe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fretcorridor.gateway.domain.exe.EtapeEtat;
import com.fretcorridor.gateway.domain.exe.EtapeMission;
import com.fretcorridor.gateway.domain.exe.EtapeType;
import com.fretcorridor.gateway.domain.exe.ExePort;
import com.fretcorridor.gateway.domain.exe.ExeServiceIndisponibleException;
import com.fretcorridor.gateway.domain.exe.Mission;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Vue web Bureau/Transporteur (Sprint 7) :
 * - Bureau : missions matérialisées par service-bur (EF-BUR-01), chronologie
 *   simplifiée à partir du statut connu côté service-exe quand disponible ;
 * - Transporteur : appels réels service-exe /api/missions/mes + chronologie.
 * Libellés transporteur : service-ida (ENF-MUL-01, tenant du JWT).
 */
@Component
public class RealExeAdapter implements ExePort {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient exeClient;
    private final WebClient burClient;
    private final WebClient idaClient;

    public RealExeAdapter(WebClient.Builder webClientBuilder,
                           @Value("${fretcorridor.service-exe.base-url}") String exeBaseUrl,
                           @Value("${fretcorridor.service-bur.base-url}") String burBaseUrl,
                           @Value("${fretcorridor.service-ida.base-url}") String idaBaseUrl) {
        HttpClient httpClient = HttpClient.create().responseTimeout(TIMEOUT);
        this.exeClient = webClientBuilder
                .baseUrl(exeBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        this.burClient = webClientBuilder
                .baseUrl(burBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        this.idaClient = webClientBuilder
                .baseUrl(idaBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public Flux<Mission> listerMissionsParTenant(String tenantId, String delegationToken) {
        if (delegationToken == null) {
            return Flux.error(new ExeServiceIndisponibleException());
        }
        return burClient.get()
                .uri("/api/v1/bur/missions-appariees")
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve()
                .bodyToFlux(MissionBurDto.class)
                .collectList()
                .flatMapMany(dtos -> {
                    List<String> ids = dtos.stream()
                            .map(MissionBurDto::transporteurId)
                            .distinct()
                            .toList();
                    return libellesTransporteurs(ids, delegationToken)
                            .flatMapMany(libelles -> Flux.fromIterable(dtos)
                                    .flatMapSequential(dto -> chronologieExe(dto.missionId(), delegationToken)
                                            .map(chrono -> versMissionBureau(tenantId, dto, chrono, libelles))
                                            .defaultIfEmpty(versMissionBureau(tenantId, dto, null, libelles))));
                })
                .onErrorMap(e -> new ExeServiceIndisponibleException());
    }

    @Override
    public Flux<Mission> listerMissionsParTransporteur(String tenantId, String transporteurId,
                                                        String delegationToken) {
        if (delegationToken == null) {
            return Flux.error(new ExeServiceIndisponibleException());
        }
        return libellesTransporteurs(List.of(transporteurId), delegationToken)
                .flatMapMany(libelles -> exeClient.get()
                        .uri("/api/missions/mes")
                        .headers(h -> h.setBearerAuth(delegationToken))
                        .retrieve()
                        .bodyToFlux(MissionResumeDto.class)
                        .flatMapSequential(resume -> chronologieExe(resume.missionId(), delegationToken)
                                .map(chrono -> versMissionTransporteur(tenantId, transporteurId, resume, chrono, libelles))
                                .defaultIfEmpty(versMissionTransporteur(tenantId, transporteurId, resume, null, libelles))))
                .onErrorMap(e -> new ExeServiceIndisponibleException());
    }

    private Mono<Map<String, String>> libellesTransporteurs(List<String> transporteurIds, String delegationToken) {
        if (transporteurIds.isEmpty()) {
            return Mono.just(Map.of());
        }
        String query = transporteurIds.stream()
                .map(id -> "ids=" + id)
                .collect(Collectors.joining("&"));
        return idaClient.get()
                .uri("/api/ida/transporteurs/libelles?" + query)
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve()
                .bodyToFlux(LibelleIdaDto.class)
                .collectMap(LibelleIdaDto::acteurId, LibelleIdaDto::libelle)
                .onErrorReturn(new HashMap<>());
    }

    private Mono<ChronologieDto> chronologieExe(String missionId, String delegationToken) {
        return exeClient.get()
                .uri("/api/missions/{id}", missionId)
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve()
                .bodyToMono(ChronologieDto.class)
                .onErrorResume(e -> Mono.empty());
    }

    private Mission versMissionBureau(String tenantId, MissionBurDto dto, ChronologieDto chrono,
                                       Map<String, String> libelles) {
        String origine = chrono != null && chrono.origineNom() != null ? chrono.origineNom() : dto.origineNom();
        String destination = chrono != null && chrono.destinationNom() != null ? chrono.destinationNom() : dto.destinationNom();
        String statut = chrono != null ? chrono.statut()
                : (dto.confirmeeLe() != null ? "PRISE_EN_CHARGE" : "EN_ATTENTE");
        String nom = libelles.getOrDefault(dto.transporteurId(), raccourcirId(dto.transporteurId()));
        return new Mission(
                dto.missionId(),
                tenantId,
                dto.transporteurId(),
                nom,
                origine,
                destination,
                etapesSimplifiees(origine, destination, statut));
    }

    private Mission versMissionTransporteur(String tenantId, String transporteurId,
                                             MissionResumeDto resume, ChronologieDto chrono,
                                             Map<String, String> libelles) {
        String origine = chrono != null && chrono.origineNom() != null ? chrono.origineNom() : resume.origineNom();
        String destination = chrono != null && chrono.destinationNom() != null ? chrono.destinationNom() : resume.destinationNom();
        String statut = chrono != null ? chrono.statut() : resume.statut();
        String nom = libelles.getOrDefault(transporteurId, raccourcirId(transporteurId));
        return new Mission(
                resume.missionId(),
                tenantId,
                transporteurId,
                nom,
                origine,
                destination,
                etapesSimplifiees(origine, destination, statut));
    }

    private static String raccourcirId(String id) {
        if (id == null || id.length() < 8) {
            return "Transporteur";
        }
        return "Transporteur " + id.substring(0, 8);
    }

    private static List<EtapeMission> etapesSimplifiees(String origine, String destination, String statut) {
        EtapeEtat etatEnlevement = switch (statut) {
            case "EN_ATTENTE" -> EtapeEtat.A_VENIR;
            case "PRISE_EN_CHARGE", "EN_TRANSIT", "LIVREE" -> EtapeEtat.TERMINEE;
            default -> EtapeEtat.A_VENIR;
        };
        EtapeEtat etatLivraison = switch (statut) {
            case "LIVREE" -> EtapeEtat.TERMINEE;
            case "PRISE_EN_CHARGE", "EN_TRANSIT" -> EtapeEtat.EN_COURS;
            default -> EtapeEtat.A_VENIR;
        };
        return List.of(
                new EtapeMission(1, EtapeType.ENLEVEMENT, origine, etatEnlevement),
                new EtapeMission(2, EtapeType.LIVRAISON, destination, etatLivraison));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LibelleIdaDto(String acteurId, String libelle) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MissionBurDto(
            String missionId,
            String transporteurId,
            String origineNom,
            String destinationNom,
            java.time.Instant confirmeeLe
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MissionResumeDto(
            String missionId,
            String statut,
            String origineNom,
            String destinationNom
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChronologieDto(String missionId, String statut, String origineNom, String destinationNom) {
    }
}
