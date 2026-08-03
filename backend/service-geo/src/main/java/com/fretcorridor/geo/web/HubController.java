package com.fretcorridor.geo.web;

import com.fretcorridor.geo.domain.Hub;
import com.fretcorridor.geo.domain.HubRepository;
import com.fretcorridor.geo.web.dto.HubCreationRequest;
import com.fretcorridor.geo.web.dto.HubResponse;
import jakarta.validation.Valid;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * API interne de consultation/gestion des hubs.
 * Consommee en synchrone par OPT/TRK (meme porteur) ; consommee via evenement Kafka
 * par les modules d'un autre porteur (ADM notamment) - a brancher dans un increment suivant.
 */
@RestController
@RequestMapping("/api/geo/hubs")
public class HubController {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final HubRepository hubRepository;

    public HubController(HubRepository hubRepository) {
        this.hubRepository = hubRepository;
    }

    @GetMapping
    public List<HubResponse> lister() {
        return hubRepository.findAll().stream()
                .map(HubResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public HubResponse consulter(@PathVariable UUID id) {
        Hub hub = hubRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hub introuvable : " + id));
        return HubResponse.from(hub);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HubResponse creer(@Valid @RequestBody HubCreationRequest request) {
        Point position = GEOMETRY_FACTORY.createPoint(new Coordinate(request.longitude(), request.latitude()));
        Hub hub = new Hub(request.nom(), request.ville(), request.typeHub(), position);
        return HubResponse.from(hubRepository.save(hub));
    }
}
