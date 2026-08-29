package com.fretcorridor.opt.web;

import com.fretcorridor.opt.simulation.SimulationInsertionService;
import com.fretcorridor.opt.web.dto.SimulationInsertionRequest;
import com.fretcorridor.opt.web.dto.SimulationInsertionResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint ON-DEMAND (point 4 du plan de reorientation) - contrairement au
 * sequencement cyclique, celui-ci est declenche seulement quand un chauffeur
 * consulte l'apercu d'une demande candidate : POST /api/opt/simulation-insertion.
 *
 * Consomme par l'app chauffeur (via la gateway, comme tous les endpoints
 * internes), eventuellement par service-exe. Ne jamais appeler en boucle
 * periodique : c'est un service de consultation, pas un service de fond.
 *
 * AUCUN effet de bord : lecture seule de la tournee en cours + calcul ALNS
 * in-memory, rien n'est persistee (cf SimulationInsertionService).
 */
@RestController
@RequestMapping("/api/opt/simulation-insertion")
public class SimulationInsertionController {

    private final SimulationInsertionService simulationInsertionService;

    public SimulationInsertionController(SimulationInsertionService simulationInsertionService) {
        this.simulationInsertionService = simulationInsertionService;
    }

    @PostMapping
    public SimulationInsertionResponse simuler(@RequestBody SimulationInsertionRequest requete) {
        return simulationInsertionService.simulerInsertion(requete);
    }
}