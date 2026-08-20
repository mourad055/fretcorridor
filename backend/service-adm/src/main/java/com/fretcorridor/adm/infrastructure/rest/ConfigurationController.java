package com.fretcorridor.adm.infrastructure.rest;

import com.fretcorridor.adm.domain.ConfigurationService;
import com.fretcorridor.adm.domain.ConfigurationVersionnee;
import com.fretcorridor.adm.infrastructure.rest.dto.ConfigurationResponse;
import com.fretcorridor.adm.infrastructure.rest.dto.DefinirConfigurationRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** FE-ADM-03 : console de configuration versionnée et auditée des paramètres métier. */
@RestController
@RequestMapping("/api/v1/configurations")
public class ConfigurationController {

    private final ConfigurationService configurationService;

    public ConfigurationController(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    /** EF-ADM-06 : catalogue de tous les paramètres métier déjà configurés (pas besoin de connaître la clé à l'avance). */
    @GetMapping
    public List<ConfigurationResponse> catalogue() {
        return configurationService.catalogue().stream().map(ConfigurationResponse::from).toList();
    }

    @GetMapping("/{cle}")
    public ResponseEntity<ConfigurationResponse> valeurCourante(
            @PathVariable String cle,
            @RequestParam(defaultValue = ConfigurationVersionnee.PERIMETRE_GLOBAL) String perimetre
    ) {
        return configurationService.valeurCourante(cle, perimetre)
                .map(config -> ResponseEntity.ok(ConfigurationResponse.from(config)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{cle}/historique")
    public List<ConfigurationResponse> historique(
            @PathVariable String cle,
            @RequestParam(defaultValue = ConfigurationVersionnee.PERIMETRE_GLOBAL) String perimetre
    ) {
        return configurationService.historique(cle, perimetre).stream().map(ConfigurationResponse::from).toList();
    }

    @PutMapping("/{cle}")
    public ConfigurationResponse definir(@PathVariable String cle,
                                          @Valid @RequestBody DefinirConfigurationRequest request) {
        String perimetre = request.perimetre() == null || request.perimetre().isBlank()
                ? ConfigurationVersionnee.PERIMETRE_GLOBAL
                : request.perimetre();
        return ConfigurationResponse.from(
                configurationService.definir(cle, perimetre, request.valeur(), request.auteur()));
    }
}
