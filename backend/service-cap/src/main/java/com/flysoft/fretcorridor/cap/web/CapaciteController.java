package com.flysoft.fretcorridor.cap.web;

import com.flysoft.fretcorridor.cap.domain.Capacite;
import com.flysoft.fretcorridor.cap.domain.CapaciteService;
import com.flysoft.fretcorridor.cap.web.dto.CapaciteCreationRequest;
import com.flysoft.fretcorridor.cap.web.dto.CapaciteResponse;
import com.flysoft.fretcorridor.cap.web.dto.DecrementRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cap/capacites")
public class CapaciteController {

    private final CapaciteService capaciteService;

    public CapaciteController(CapaciteService capaciteService) {
        this.capaciteService = capaciteService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CapaciteResponse declarer(@Valid @RequestBody CapaciteCreationRequest requete) {
        Capacite capacite = capaciteService.declarer(requete);
        return CapaciteResponse.from(capacite);
    }

    @PostMapping("/{id}/decrement")
    public CapaciteResponse decrementer(@PathVariable UUID id, @Valid @RequestBody DecrementRequest requete) {
        Capacite capacite = capaciteService.decrementer(id, requete.montantKg(), requete.cleIdempotence());
        return CapaciteResponse.from(capacite);
    }
}
