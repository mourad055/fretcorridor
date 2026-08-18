package com.fretcorridor.gateway.infrastructure.rest.adm;

import com.fretcorridor.gateway.domain.adm.AdmPort;
import com.fretcorridor.gateway.infrastructure.rest.adm.dto.ConfigurationResponse;
import com.fretcorridor.gateway.infrastructure.rest.adm.dto.DefinirConfigurationRequest;
import com.fretcorridor.gateway.infrastructure.security.AuthenticatedActor;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** FE-ADM-03 : console de configuration versionnée et auditée des paramètres métier. */
@RestController
@RequestMapping("/api/v1/admin/configurations")
public class ConfigurationController {

    private static final String PERIMETRE_GLOBAL = "GLOBAL";

    private final AdmPort admPort;

    public ConfigurationController(AdmPort admPort) {
        this.admPort = admPort;
    }

    /** EF-ADM-06 : catalogue de tous les paramètres métier déjà configurés (pas besoin de connaître la clé à l'avance). */
    @GetMapping
    public Flux<ConfigurationResponse> catalogue() {
        return admPort.catalogueConfigurations().map(ConfigurationResponse::from);
    }

    @GetMapping("/{cle}")
    public Mono<ConfigurationResponse> valeurCourante(@PathVariable String cle,
                                                       @RequestParam(required = false) String perimetre) {
        return admPort.configurationCourante(cle, perimetre == null ? PERIMETRE_GLOBAL : perimetre)
                .map(ConfigurationResponse::from);
    }

    @GetMapping("/{cle}/historique")
    public Flux<ConfigurationResponse> historique(@PathVariable String cle,
                                                    @RequestParam(required = false) String perimetre) {
        return admPort.historiqueConfiguration(cle, perimetre == null ? PERIMETRE_GLOBAL : perimetre)
                .map(ConfigurationResponse::from);
    }

    @PutMapping("/{cle}")
    public Mono<ConfigurationResponse> definir(@PathVariable String cle,
                                                @Valid @RequestBody DefinirConfigurationRequest request,
                                                @AuthenticationPrincipal AuthenticatedActor actor) {
        String perimetre = request.perimetre() == null || request.perimetre().isBlank()
                ? PERIMETRE_GLOBAL
                : request.perimetre();
        return admPort.definirConfiguration(cle, perimetre, request.valeur(), actor.actorId())
                .map(ConfigurationResponse::from);
    }
}
