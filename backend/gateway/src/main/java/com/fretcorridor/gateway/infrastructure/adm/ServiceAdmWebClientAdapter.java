package com.fretcorridor.gateway.infrastructure.adm;

import com.fretcorridor.gateway.domain.adm.AdmPort;
import com.fretcorridor.gateway.domain.adm.AdmServiceIndisponibleException;
import com.fretcorridor.gateway.domain.adm.ConfigurationVue;
import com.fretcorridor.gateway.domain.adm.DossierVue;
import com.fretcorridor.gateway.domain.adm.EntreeJournalAuditVue;
import com.fretcorridor.gateway.domain.adm.TenantVue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Component
public class ServiceAdmWebClientAdapter implements AdmPort {

    private final WebClient webClient;

    public ServiceAdmWebClientAdapter(WebClient.Builder webClientBuilder,
                                       @Value("${fretcorridor.service-adm.base-url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public Flux<DossierVue> fileDeTravail(String tenantId, String delegationToken) {
        if (delegationToken == null) {
            return Flux.error(new AdmServiceIndisponibleException());
        }
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/dossiers").queryParam("tenantId", tenantId).build())
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve()
                .bodyToFlux(DossierVue.class);
    }

    @Override
    public Mono<DossierVue> dossier(String dossierId, String delegationToken) {
        if (delegationToken == null) {
            return Mono.error(new AdmServiceIndisponibleException());
        }
        return webClient.get().uri("/api/v1/dossiers/{id}", dossierId)
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve().bodyToMono(DossierVue.class);
    }

    @Override
    public Mono<DossierVue> priseEnCharge(String dossierId, String acteurId, String delegationToken) {
        if (delegationToken == null) {
            return Mono.error(new AdmServiceIndisponibleException());
        }
        return webClient.post()
                .uri("/api/v1/dossiers/{id}/prise-en-charge", dossierId)
                .headers(h -> h.setBearerAuth(delegationToken))
                .bodyValue(Map.of("acteurId", acteurId))
                .retrieve()
                .bodyToMono(DossierVue.class);
    }

    @Override
    public Mono<DossierVue> decider(String dossierId, String decision, String motif, String acteurId, String delegationToken) {
        if (delegationToken == null) {
            return Mono.error(new AdmServiceIndisponibleException());
        }
        return webClient.post()
                .uri("/api/v1/dossiers/{id}/decision", dossierId)
                .headers(h -> h.setBearerAuth(delegationToken))
                .bodyValue(Map.of("decision", decision, "motif", motif, "acteurId", acteurId))
                .retrieve()
                .bodyToMono(DossierVue.class);
    }

    @Override
    public Flux<DossierVue> declencherEscalade(String delegationToken) {
        if (delegationToken == null) {
            return Flux.error(new AdmServiceIndisponibleException());
        }
        return webClient.post().uri("/api/v1/dossiers/escalade")
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve().bodyToFlux(DossierVue.class);
    }

    @Override
    public Flux<ConfigurationVue> catalogueConfigurations(String delegationToken) {
        if (delegationToken == null) {
            return Flux.error(new AdmServiceIndisponibleException());
        }
        return webClient.get().uri("/api/v1/configurations")
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve().bodyToFlux(ConfigurationVue.class);
    }

    @Override
    public Mono<ConfigurationVue> configurationCourante(String cle, String perimetre, String delegationToken) {
        if (delegationToken == null) {
            return Mono.error(new AdmServiceIndisponibleException());
        }
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/configurations/{cle}").queryParam("perimetre", perimetre).build(cle))
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve()
                .bodyToMono(ConfigurationVue.class);
    }

    @Override
    public Flux<ConfigurationVue> historiqueConfiguration(String cle, String perimetre, String delegationToken) {
        if (delegationToken == null) {
            return Flux.error(new AdmServiceIndisponibleException());
        }
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/configurations/{cle}/historique").queryParam("perimetre", perimetre).build(cle))
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve()
                .bodyToFlux(ConfigurationVue.class);
    }

    @Override
    public Mono<ConfigurationVue> definirConfiguration(String cle, String perimetre, String valeur, String auteur, String delegationToken) {
        if (delegationToken == null) {
            return Mono.error(new AdmServiceIndisponibleException());
        }
        Map<String, Object> body = new HashMap<>();
        body.put("perimetre", perimetre);
        body.put("valeur", valeur);
        body.put("auteur", auteur);
        return webClient.put()
                .uri("/api/v1/configurations/{cle}", cle)
                .headers(h -> h.setBearerAuth(delegationToken))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(ConfigurationVue.class);
    }

    @Override
    public Flux<TenantVue> tenants(String delegationToken) {
        if (delegationToken == null) {
            return Flux.error(new AdmServiceIndisponibleException());
        }
        return webClient.get().uri("/api/v1/tenants")
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve().bodyToFlux(TenantVue.class);
    }

    @Override
    public Mono<TenantVue> creerTenant(String id, String nom, String pays, String auteur, String delegationToken) {
        if (delegationToken == null) {
            return Mono.error(new AdmServiceIndisponibleException());
        }
        return webClient.post()
                .uri("/api/v1/tenants")
                .headers(h -> h.setBearerAuth(delegationToken))
                .bodyValue(Map.of("id", id, "nom", nom, "pays", pays, "auteur", auteur))
                .retrieve()
                .bodyToMono(TenantVue.class);
    }

    @Override
    public Mono<TenantVue> modifierTenant(String id, String nom, String pays, boolean actif, String delegationToken) {
        if (delegationToken == null) {
            return Mono.error(new AdmServiceIndisponibleException());
        }
        return webClient.put()
                .uri("/api/v1/tenants/{id}", id)
                .headers(h -> h.setBearerAuth(delegationToken))
                .bodyValue(Map.of("nom", nom, "pays", pays, "actif", actif))
                .retrieve()
                .bodyToMono(TenantVue.class);
    }

    @Override
    public Flux<EntreeJournalAuditVue> journalAudit(String tenantId, String delegationToken) {
        if (delegationToken == null) {
            return Flux.error(new AdmServiceIndisponibleException());
        }
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/api/v1/journal-audit");
                    if (tenantId != null) {
                        uriBuilder.queryParam("tenantId", tenantId);
                    }
                    return uriBuilder.build();
                })
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve()
                .bodyToFlux(EntreeJournalAuditVue.class);
    }

    @Override
    public Mono<String> exporterJournalAudit(String tenantId, String delegationToken) {
        if (delegationToken == null) {
            return Mono.error(new AdmServiceIndisponibleException());
        }
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/api/v1/journal-audit/export");
                    if (tenantId != null) {
                        uriBuilder.queryParam("tenantId", tenantId);
                    }
                    return uriBuilder.build();
                })
                .headers(h -> h.setBearerAuth(delegationToken))
                .retrieve()
                .bodyToMono(String.class);
    }

    @Override
    public Mono<Void> enregistrerAudit(String tenantId, String acteurId, String action, String ressource, String delegationToken) {
        if (delegationToken == null) {
            return Mono.error(new AdmServiceIndisponibleException());
        }
        Map<String, Object> body = new HashMap<>();
        body.put("tenantId", tenantId);
        body.put("acteurId", acteurId);
        body.put("action", action);
        body.put("ressource", ressource);
        return webClient.post()
                .uri("/api/v1/journal-audit")
                .headers(h -> h.setBearerAuth(delegationToken))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class);
    }
}
