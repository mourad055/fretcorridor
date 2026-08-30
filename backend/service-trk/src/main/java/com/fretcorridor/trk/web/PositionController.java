package com.fretcorridor.trk.web;

import com.fretcorridor.trk.domain.Position;
import com.fretcorridor.trk.domain.PositionRepository;
import com.fretcorridor.trk.web.dto.PositionActuelleResponse;
import com.fretcorridor.trk.web.dto.PositionsBatchRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Premier endpoint HTTP expose par service-trk (jusqu'ici uniquement des
 * listeners Kafka + un client sortant vers OPT). Consomme en synchrone
 * interne par OPT (meme porteur, cf README moteur "MAT<->OPT<->GEO<->TRK =
 * synchrone interne") pour le matching en position temps reel du chauffeur
 * (plan de reorientation post-demo, partie Chauffeur point 1).
 *
 * Pas de PATCH/POST ici - lecture seule, donc aucune restriction par role
 * necessaire (meme raisonnement que service-geo GET, service-mat, service-opt :
 * cf SecurityConfig de ces services).
 */
@RestController
@RequestMapping("/api/trk/positions")
public class PositionController {

    private final PositionRepository positionRepository;

    public PositionController(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    /**
     * 404 si aucune position n'est jamais arrivee pour ce vehicule - distinct
     * d'un vehicule dont la derniere position est simplement vieille (200
     * avec horodatageCapture ancien) : c'est a l'appelant de juger la
     * fraicheur, jamais a ce controller de trancher a sa place.
     */
    @GetMapping("/derniere")
    public PositionActuelleResponse dernierePosition(@RequestParam UUID vehiculeId) {
        return positionRepository.findFirstByVehiculeIdOrderByHorodatageCaptureDesc(vehiculeId)
                .map(PositionActuelleResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "aucune position connue pour le vehicule : " + vehiculeId));
    }

    /**
     * Equivalent groupe de /derniere - une requete SQL pour N vehicules
     * (plan de reorientation, position GPS temps reel dans le matching).
     * Un vehicule absent de la Map retournee = aucune position connue pour
     * lui (equivalent du 404 unitaire, mais jamais d'exception ici : un seul
     * vehicule sans position ne doit pas faire echouer tout le lot, ENF-DIS-04
     * applique au niveau batch).
     */
    @PostMapping("/batch")
    public Map<UUID, PositionActuelleResponse> dernieresPositions(@Valid @RequestBody PositionsBatchRequest requete) {
        List<Position> positions = positionRepository.findDernieresPositionsPourVehicules(requete.vehiculeIds());
        return positions.stream()
                .collect(Collectors.toMap(Position::getVehiculeId, PositionActuelleResponse::from));
    }
}
