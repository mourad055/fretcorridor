package com.fretcorridor.geo.web;

import com.fretcorridor.geo.domain.Hub;
import com.fretcorridor.geo.domain.HubRepository;
import com.fretcorridor.geo.domain.ZonageH3Service;
import com.fretcorridor.geo.web.dto.HubCreationRequest;
import com.fretcorridor.geo.web.dto.HubResponse;
import jakarta.validation.Valid;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * API interne de consultation/gestion des hubs (Sprint 3 - fondation geospatiale).
 *
 * Consommee en synchrone REST par OPT/TRK (meme porteur, moi - cf regle de communication
 * du perimetre Moteur : appels directs justifies par le budget de latence L0 ~50ms).
 * Consommee via evenement Kafka par tout module d'un autre porteur (Web/ADM notamment) -
 * a brancher dans un increment suivant, jamais d'appel synchrone cross-porteur.
 */
@RestController
@RequestMapping("/api/geo/hubs")
public class HubController {

    // Reutilisable et thread-safe : une seule instance suffit pour tout le service,
    // pas besoin de la recreer a chaque requete
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final HubRepository hubRepository;
    // Calcule l'index H3 a la creation - jamais fait dans l'entite elle-meme,
    // qui n'a pas acces a la resolution configurable stockee en base.
    private final ZonageH3Service zonageH3Service;

    // Injection par constructeur (pas de @Autowired sur un champ) : rend les dependances
    // explicites et facilite les tests unitaires avec un mock
    public HubController(HubRepository hubRepository, ZonageH3Service zonageH3Service) {
        this.hubRepository = hubRepository;
        this.zonageH3Service = zonageH3Service;
    }

    // Liste complete des hubs. Pas de pagination pour l'instant (volume faible en Phase 1,
    // un seul axe) - a revoir des que le nombre de hubs grandit (Phase 2+).
    @GetMapping
    public List<HubResponse> lister() {
        return hubRepository.findAll().stream()
                .map(HubResponse::from)
                .toList();
    }

    // Consultation unitaire. 404 explicite si l'id n'existe pas, plutot que null
    // ou une exception non geree - le consommateur (OPT/TRK) recoit une reponse HTTP claire.
    @GetMapping("/{id}")
    public HubResponse consulter(@PathVariable UUID id) {
        Hub hub = hubRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hub introuvable : " + id));
        return HubResponse.from(hub);
    }

    // Creation d'un hub. @Valid declenche les contraintes de HubCreationRequest avant
    // meme d'entrer dans la methode - toute donnee invalide est rejetee en 400 automatiquement.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HubResponse creer(@Valid @RequestBody HubCreationRequest request) {
        // Attention a l'ordre JTS : Coordinate(x, y) = Coordinate(longitude, latitude),
        // inverse de l'ordre naturel "latitude, longitude" - source classique d'erreur silencieuse
        Point position = GEOMETRY_FACTORY.createPoint(new Coordinate(request.longitude(), request.latitude()));

        // Index H3 calcule une seule fois, ici, a la creation - jamais recalcule ensuite
        // (un hub ne change pas de position une fois cree, cf commentaire sur le champ h3Index).
        String h3Index = zonageH3Service.indexPourPoint(request.latitude(), request.longitude());

        Hub hub = new Hub(request.nom(), request.ville(), request.typeHub(), position, h3Index);
        return HubResponse.from(hubRepository.save(hub));
    }
}
