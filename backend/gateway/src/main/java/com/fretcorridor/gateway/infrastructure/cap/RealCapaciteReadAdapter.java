package com.fretcorridor.gateway.infrastructure.cap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fretcorridor.gateway.domain.cap.Capacite;
import com.fretcorridor.gateway.domain.cap.CapaciteEtat;
import com.fretcorridor.gateway.domain.cap.CapacitePort;
import com.fretcorridor.gateway.domain.cap.CapServiceIndisponibleException;
import com.fretcorridor.gateway.domain.cap.ModeCollecte;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.function.Function;

/**
 * FE-TRP-01 (lecture web) : appelle service-cap GET /api/cap/capacites/mes avec
 * le delegationToken service-ida — même principe que RealCapaciteDeclarationAdapter.
 * Enrichit origine/destination via service-geo et le libellé véhicule via service-flt.
 */
@Component
public class RealCapaciteReadAdapter implements CapacitePort {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient capClient;
    private final WebClient geoClient;
    private final WebClient fltClient;

    public RealCapaciteReadAdapter(WebClient.Builder webClientBuilder,
                                    @Value("${fretcorridor.service-cap.base-url}") String capBaseUrl,
                                    @Value("${fretcorridor.service-geo.base-url}") String geoBaseUrl,
                                    @Value("${fretcorridor.service-flt.base-url}") String fltBaseUrl) {
        HttpClient httpClient = HttpClient.create().responseTimeout(TIMEOUT);
        this.capClient = webClientBuilder
                .baseUrl(capBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        this.geoClient = webClientBuilder
                .baseUrl(geoBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        this.fltClient = webClientBuilder
                .baseUrl(fltBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public Flux<Capacite> listerMesCapacites(String transporteurId, String delegationToken) {
        if (delegationToken == null) {
            return Flux.error(new CapServiceIndisponibleException());
        }
        return Mono.zip(chargerAxes(delegationToken), chargerVehicules(delegationToken))
                .flatMapMany(referentiels -> capClient.get()
                        .uri("/api/cap/capacites/mes")
                        .headers(h -> h.setBearerAuth(delegationToken))
                        .retrieve()
                        .bodyToFlux(CapaciteCapDto.class)
                        .map(dto -> versCapacite(dto, transporteurId, referentiels.getT1(), referentiels.getT2())))
                .onErrorMap(e -> new CapServiceIndisponibleException());
    }

    private Mono<Map<String, AxeGeoDto>> chargerAxes(String delegationToken) {
        return geoClient.get()
                .uri("/api/geo/axes")
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve()
                .bodyToFlux(AxeGeoDto.class)
                .collectMap(AxeGeoDto::id, Function.identity())
                .onErrorReturn(Map.of());
    }

    private Mono<Map<String, VehiculeFltDto>> chargerVehicules(String delegationToken) {
        return fltClient.get()
                .uri("/api/flt/vehicules/mes")
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve()
                .bodyToFlux(VehiculeFltDto.class)
                .collectMap(VehiculeFltDto::id, Function.identity())
                .onErrorReturn(Map.of());
    }

    private Capacite versCapacite(CapaciteCapDto dto, String transporteurIdAttendu,
                                 Map<String, AxeGeoDto> axes, Map<String, VehiculeFltDto> vehicules) {
        AxeGeoDto axe = axes.get(dto.axeId());
        String origine = axe != null ? axe.hubOrigineNom() : "—";
        String destination = axe != null ? axe.hubDestinationNom() : "—";
        VehiculeFltDto vehicule = vehicules.get(dto.vehiculeId());
        String libelleVehicule = vehicule != null
                ? (vehicule.typeVehicule() + " — " + vehicule.immatriculation())
                : dto.typeVehiculeFallback();
        return new Capacite(
                dto.id(),
                transporteurIdAttendu,
                libelleVehicule,
                origine,
                destination,
                dto.dateDepart(),
                dto.poidsTaxableKg().doubleValue(),
                mapperModeCollecte(dto.modeDeclaration()),
                mapperEtat(dto));
    }

    private static ModeCollecte mapperModeCollecte(String modeDeclaration) {
        if ("POINT_DEPOT".equals(modeDeclaration)) {
            return ModeCollecte.POINT_DEPOT;
        }
        return ModeCollecte.PORTE_A_PORTE;
    }

    private static CapaciteEtat mapperEtat(CapaciteCapDto dto) {
        if (dto.expiree()) {
            return CapaciteEtat.EXPIREE;
        }
        if (dto.capaciteResiduelleKg().compareTo(dto.poidsTaxableKg()) < 0) {
            return CapaciteEtat.APPARIEE;
        }
        return CapaciteEtat.PUBLIEE;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AxeGeoDto(String id, String hubOrigineNom, String hubDestinationNom) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record VehiculeFltDto(String id, String typeVehicule, String immatriculation) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CapaciteCapDto(
            String id,
            String vehiculeId,
            String axeId,
            String modeDeclaration,
            BigDecimal poidsTaxableKg,
            BigDecimal capaciteResiduelleKg,
            boolean expiree,
            Instant dateDepart
    ) {
        String typeVehiculeFallback() {
            return "Véhicule";
        }
    }
}
