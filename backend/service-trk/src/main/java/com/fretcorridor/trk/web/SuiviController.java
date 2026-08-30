package com.fretcorridor.trk.web;

import com.fretcorridor.trk.client.AffectationDto;
import com.fretcorridor.trk.client.ServiceOptClient;
import com.fretcorridor.trk.domain.ColisRecuperation;
import com.fretcorridor.trk.domain.ColisRecuperationRepository;
import com.fretcorridor.trk.domain.Position;
import com.fretcorridor.trk.domain.PositionRepository;
import com.fretcorridor.trk.web.dto.SuiviMissionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

/**
 * Endpoint de suivi (point 6 du plan de reorientation : "colis recupere =
 * position chauffeur"). Verifie si le colis d'une mission a ete recupere
 * (enlevement execute, EtapeExecuteeListener) et retourne la position a
 * afficher en consequence :
 *   - colis recupere  -> position GPS temps reel du chauffeur (TRK Position) ;
 *   - colis pas encore -> position estimee du colis (son point d'enlevement,
 *     porte par l'affectation cote OPT).
 *
 * Consomme en synchrone interne par service-exe (Mobile) pour l'ecran de
 * suivi client. 404 si la mission n'existe pas chez OPT (aucune position
 * estimee possible).
 */
@RestController
@RequestMapping("/api/trk/suivi")
public class SuiviController {

    private final ColisRecuperationRepository colisRecuperationRepository;
    private final PositionRepository positionRepository;
    private final ServiceOptClient serviceOptClient;

    public SuiviController(ColisRecuperationRepository colisRecuperationRepository,
                           PositionRepository positionRepository,
                           ServiceOptClient serviceOptClient) {
        this.colisRecuperationRepository = colisRecuperationRepository;
        this.positionRepository = positionRepository;
        this.serviceOptClient = serviceOptClient;
    }

    @GetMapping("/{missionId}")
    public SuiviMissionResponse consulter(@PathVariable UUID missionId) {
        Optional<ColisRecuperation> colis = colisRecuperationRepository.findFirstByMissionId(missionId);

        if (colis.isPresent()) {
            // Colis a bord : position GPS temps reel du chauffeur.
            Optional<Position> derniere = positionRepository
                    .findFirstByMissionIdOrderByHorodatageCaptureDesc(missionId);
            if (derniere.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "colis recupere mais aucune position GPS recue pour la mission : " + missionId);
            }
            Position position = derniere.get();
            return new SuiviMissionResponse(
                    true,
                    SuiviMissionResponse.SourcePosition.GPS_CHAUFFEUR,
                    position.getLatitude(), position.getLongitude(),
                    position.getHorodatageCapture(),
                    colis.get().getHorodatageEnlevement());
        }

        // Colis pas encore recupere : position estimee = point d'enlevement de
        // la mission (porte par OPT). Injoignable / introuvable -> 404.
        Optional<AffectationDto> affectation = serviceOptClient.obtenirAffectation(missionId);
        if (affectation.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "mission introuvable chez OPT (impossible d'estimer la position du colis) : "
                            + missionId);
        }
        AffectationDto mission = affectation.get();
        return new SuiviMissionResponse(
                false,
                SuiviMissionResponse.SourcePosition.POSITION_ESTIMEE,
                mission.origineLatitude(), mission.origineLongitude(),
                null,
                null);
    }
}
