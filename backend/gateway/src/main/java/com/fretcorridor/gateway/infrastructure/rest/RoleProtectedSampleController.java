package com.fretcorridor.gateway.infrastructure.rest;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints de démonstration du RBAC (Sprint 1), un par rôle. Ils seront remplacés
 * par le routage réel vers service-bur/service-adm/console transporteur au fil des
 * sprints suivants — leur seul objet ici est de prouver que la garde de rôle
 * fonctionne côté gateway ET côté web sur le même découpage de chemins.
 */
@RestController
public class RoleProtectedSampleController {

    @GetMapping("/api/v1/bureau/ping")
    public Map<String, String> bureauPing(@AuthenticationPrincipal String actorId) {
        return Map.of("message", "Bureau — accès autorisé", "actorId", actorId);
    }

    @GetMapping("/api/v1/transporteur/ping")
    public Map<String, String> transporteurPing(@AuthenticationPrincipal String actorId) {
        return Map.of("message", "Transporteur — accès autorisé", "actorId", actorId);
    }

    @GetMapping("/api/v1/admin/ping")
    public Map<String, String> adminPing(@AuthenticationPrincipal String actorId) {
        return Map.of("message", "Administration — accès autorisé", "actorId", actorId);
    }
}
