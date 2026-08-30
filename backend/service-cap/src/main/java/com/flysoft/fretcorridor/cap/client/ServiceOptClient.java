package com.flysoft.fretcorridor.cap.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.UUID;

/**
 * UC-MAT-02/diffusion-course : "mes propositions en attente" du transporteur
 * -- GET /api/opt/affectations/proposees construit specifiquement par le
 * porteur Moteur pour cet usage (voir commentaire application.yml). Cle
 * interne partagee, meme mecanisme que les autres appels internes de ce
 * service (ServiceFltClient).
 */
@Component
public class ServiceOptClient {

    private static final Logger log = LoggerFactory.getLogger(ServiceOptClient.class);

    private final RestClient restClient;
    private final String cleInterne;

    public ServiceOptClient(@Qualifier("serviceOptRestClient") RestClient serviceOptRestClient,
                             @Value("${fretcorridor.internal.service-key}") String cleInterne) {
        this.restClient = serviceOptRestClient;
        this.cleInterne = cleInterne;
    }

    public List<AffectationProposeeDto> listerPropositions(UUID transporteurId) {
        try {
            AffectationProposeeDto[] reponse = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/opt/affectations/proposees")
                            .queryParam("transporteurId", transporteurId)
                            .build())
                    .header("X-Internal-Service-Key", cleInterne)
                    .retrieve()
                    .body(AffectationProposeeDto[].class);
            return reponse == null ? List.of() : List.of(reponse);
        } catch (RestClientException exception) {
            log.warn("Echec appel service-opt (mes propositions, transporteur={}) : {}",
                    transporteurId, exception.getMessage());
            throw new ServiceOptIndisponibleException();
        }
    }
}
