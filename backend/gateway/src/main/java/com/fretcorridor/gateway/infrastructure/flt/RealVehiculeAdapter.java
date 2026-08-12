package com.fretcorridor.gateway.infrastructure.flt;

import com.fretcorridor.gateway.domain.flt.DeclarationVehicule;
import com.fretcorridor.gateway.domain.flt.FltServiceIndisponibleException;
import com.fretcorridor.gateway.domain.flt.Vehicule;
import com.fretcorridor.gateway.domain.flt.VehiculePort;
import com.fretcorridor.gateway.domain.flt.VehiculeRefuseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Appelle service-flt (Mobile, port 8083) pour la console de flotte
 * simplifiée (S10). Même principe de delegationToken que RealPositionAdapter.
 */
@Component
public class RealVehiculeAdapter implements VehiculePort {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;

    public RealVehiculeAdapter(WebClient.Builder webClientBuilder,
                                @Value("${fretcorridor.service-flt.base-url}") String baseUrl) {
        HttpClient httpClient = HttpClient.create().responseTimeout(TIMEOUT);
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public Mono<Vehicule> declarer(String delegationToken, DeclarationVehicule declaration) {
        if (delegationToken == null) {
            return Mono.error(new FltServiceIndisponibleException());
        }
        Map<String, Object> corps = new HashMap<>();
        corps.put("typeVehicule", declaration.typeVehicule());
        corps.put("immatriculation", declaration.immatriculation());
        corps.put("profilHauteurMetres", declaration.profilHauteurMetres());
        corps.put("profilLargeurMetres", declaration.profilLargeurMetres());
        corps.put("profilLongueurMetres", declaration.profilLongueurMetres());
        corps.put("profilPoidsMaxTonnes", declaration.profilPoidsMaxTonnes());
        corps.put("profilChargeMaxParEssieuTonnes", declaration.profilChargeMaxParEssieuTonnes());
        corps.put("profilNombreEssieux", declaration.profilNombreEssieux());
        corps.put("profilMatieresDangereuses", declaration.profilMatieresDangereuses());

        return webClient.post()
                .uri("/api/flt/vehicules")
                .headers(h -> h.setBearerAuth(delegationToken))
                .bodyValue(corps)
                .retrieve()
                .bodyToMono(VehiculeDto.class)
                .map(VehiculeDto::versVehicule)
                .onErrorMap(this::estRefus, e -> new VehiculeRefuseException(messageDe(e)))
                .onErrorMap(e -> !(e instanceof VehiculeRefuseException), e -> new FltServiceIndisponibleException());
    }

    @Override
    public Flux<Vehicule> mesVehicules(String delegationToken) {
        if (delegationToken == null) {
            return Flux.error(new FltServiceIndisponibleException());
        }
        return webClient.get()
                .uri("/api/flt/vehicules/mes")
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve()
                .bodyToFlux(VehiculeDto.class)
                .map(VehiculeDto::versVehicule)
                .onErrorMap(e -> new FltServiceIndisponibleException());
    }

    private boolean estRefus(Throwable e) {
        return e instanceof WebClientResponseException wcre && wcre.getStatusCode().value() == 400;
    }

    private String messageDe(Throwable e) {
        return e instanceof WebClientResponseException wcre ? wcre.getResponseBodyAsString() : "Requête refusée";
    }

    private record VehiculeDto(String id, String typeVehicule, String immatriculation, Double profilHauteurMetres,
                                Double profilLargeurMetres, Double profilLongueurMetres, Double profilPoidsMaxTonnes,
                                Double profilChargeMaxParEssieuTonnes, Integer profilNombreEssieux,
                                Boolean profilMatieresDangereuses, String dateCreation) {
        Vehicule versVehicule() {
            return new Vehicule(id, typeVehicule, immatriculation, profilHauteurMetres, profilLargeurMetres,
                    profilLongueurMetres, profilPoidsMaxTonnes, profilChargeMaxParEssieuTonnes, profilNombreEssieux,
                    Boolean.TRUE.equals(profilMatieresDangereuses), dateCreation);
        }
    }
}
