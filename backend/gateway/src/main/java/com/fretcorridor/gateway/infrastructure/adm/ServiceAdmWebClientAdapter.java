package com.fretcorridor.gateway.infrastructure.adm;

import com.fretcorridor.gateway.domain.adm.AdmPort;
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
    public Flux<DossierVue> fileDeTravail(String tenantId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/dossiers").queryParam("tenantId", tenantId).build())
                .retrieve()
                .bodyToFlux(DossierVue.class);
    }

    @Override
    public Mono<DossierVue> dossier(String dossierId) {
        return webClient.get().uri("/api/v1/dossiers/{id}", dossierId).retrieve().bodyToMono(DossierVue.class);
    }

    @Override
    public Mono<DossierVue> priseEnCharge(String dossierId, String acteurId) {
        return webClient.post()
                .uri("/api/v1/dossiers/{id}/prise-en-charge", dossierId)
                .bodyValue(Map.of("acteurId", acteurId))
                .retrieve()
                .bodyToMono(DossierVue.class);
    }

    @Override
    public Mono<DossierVue> decider(String dossierId, String decision, String motif, String acteurId) {
        return webClient.post()
                .uri("/api/v1/dossiers/{id}/decision", dossierId)
                .bodyValue(Map.of("decision", decision, "motif", motif, "acteurId", acteurId))
                .retrieve()
                .bodyToMono(DossierVue.class);
    }

    @Override
    public Flux<DossierVue> declencherEscalade() {
        return webClient.post().uri("/api/v1/dossiers/escalade").retrieve().bodyToFlux(DossierVue.class);
    }

    @Override
    public Mono<ConfigurationVue> configurationCourante(String cle, String perimetre) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/configurations/{cle}").queryParam("perimetre", perimetre).build(cle))
                .retrieve()
                .bodyToMono(ConfigurationVue.class);
    }

    @Override
    public Flux<ConfigurationVue> historiqueConfiguration(String cle, String perimetre) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/configurations/{cle}/historique").queryParam("perimetre", perimetre).build(cle))
                .retrieve()
                .bodyToFlux(ConfigurationVue.class);
    }

    @Override
    public Mono<ConfigurationVue> definirConfiguration(String cle, String perimetre, String valeur, String auteur) {
        Map<String, Object> body = new HashMap<>();
        body.put("perimetre", perimetre);
        body.put("valeur", valeur);
        body.put("auteur", auteur);
        return webClient.put()
                .uri("/api/v1/configurations/{cle}", cle)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(ConfigurationVue.class);
    }

    @Override
    public Flux<TenantVue> tenants() {
        return webClient.get().uri("/api/v1/tenants").retrieve().bodyToFlux(TenantVue.class);
    }

    @Override
    public Mono<TenantVue> creerTenant(String id, String nom, String pays, String auteur) {
        return webClient.post()
                .uri("/api/v1/tenants")
                .bodyValue(Map.of("id", id, "nom", nom, "pays", pays, "auteur", auteur))
                .retrieve()
                .bodyToMono(TenantVue.class);
    }

    @Override
    public Flux<EntreeJournalAuditVue> journalAudit(String tenantId) {
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/api/v1/journal-audit");
                    if (tenantId != null) {
                        uriBuilder.queryParam("tenantId", tenantId);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToFlux(EntreeJournalAuditVue.class);
    }

    @Override
    public Mono<String> exporterJournalAudit(String tenantId) {
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/api/v1/journal-audit/export");
                    if (tenantId != null) {
                        uriBuilder.queryParam("tenantId", tenantId);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(String.class);
    }

    @Override
    public Mono<Void> enregistrerAudit(String tenantId, String acteurId, String action, String ressource) {
        Map<String, Object> body = new HashMap<>();
        body.put("tenantId", tenantId);
        body.put("acteurId", acteurId);
        body.put("action", action);
        body.put("ressource", ressource);
        return webClient.post()
                .uri("/api/v1/journal-audit")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class);
    }
}
