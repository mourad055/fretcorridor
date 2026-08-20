package com.flysoft.fretcorridor.cap.web;

import com.flysoft.fretcorridor.cap.domain.Capacite;
import com.flysoft.fretcorridor.cap.domain.CapaciteService;
import com.flysoft.fretcorridor.cap.security.JwtService;
import com.flysoft.fretcorridor.cap.web.dto.CapaciteCreationRequest;
import com.flysoft.fretcorridor.cap.web.dto.CapaciteResponse;
import com.flysoft.fretcorridor.cap.web.dto.DecrementRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/cap/capacites")
public class CapaciteController {

    private final CapaciteService capaciteService;
    private final JwtService jwtService;

    public CapaciteController(CapaciteService capaciteService, JwtService jwtService) {
        this.capaciteService = capaciteService;
        this.jwtService = jwtService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CapaciteResponse declarer(@Valid @RequestBody CapaciteCreationRequest requete,
                                      @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String tenantId = jwtService.extraireTenantId(token);
        Capacite capacite = capaciteService.declarer(requete, tenantId, token);
        return CapaciteResponse.from(capacite);
    }

    // IDOR corrige (audit CDC du 19 aout) : le tenant vient desormais du
    // JWT, jamais du corps de requete - DecrementRequest ne porte plus
    // aucune notion de tenant.
    @PostMapping("/{id}/decrement")
    public CapaciteResponse decrementer(@PathVariable UUID id, @Valid @RequestBody DecrementRequest requete,
                                         @RequestHeader("Authorization") String authHeader) {
        try {
            String tenantId = jwtService.extraireTenantId(authHeader.substring(7));
            Capacite capacite = capaciteService.decrementer(id, tenantId, requete.montantKg(), requete.cleIdempotence());
            return CapaciteResponse.from(capacite);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    // S12 (retour à vide) : resolution capaciteId -> transporteurId côté
    // service-not, avant notification au chauffeur. Symétrique du POST
    // existant (même resource), même pattern que ServiceFltClient
    // (service-cap -> service-flt) mais dans l'autre sens.
    @GetMapping("/{id}")
    public CapaciteResponse obtenir(@PathVariable UUID id) {
        try {
            return CapaciteResponse.from(capaciteService.obtenir(id));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
