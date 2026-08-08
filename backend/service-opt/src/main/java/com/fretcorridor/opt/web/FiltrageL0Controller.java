package com.fretcorridor.opt.web;

import com.fretcorridor.opt.client.HubProcheDto;
import com.fretcorridor.opt.client.ServiceGeoClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint de verification manuelle du filtrage L0 (Sprint 5, premiere brique).
 *
 * Ne fait pour l'instant qu'exposer le resultat brut de l'appel a service-geo,
 * sans encore appliquer l'affectation Kuhn-Munkres (L1) - c'est volontaire,
 * conforme au guide "avancer module par module" : on valide chaque brique
 * independamment avant de les enchainer.
 *
 * A terme, cet endpoint disparaitra au profit d'un declenchement par evenement
 * Kafka (DemandePubliee, CapaciteDeclaree) plutot que par appel HTTP direct -
 * conserve ici uniquement pour les tests de la Phase 1 / Sprint 5.
 */
@RestController
@RequestMapping("/api/opt/filtrage-l0")
public class FiltrageL0Controller {

    private final ServiceGeoClient serviceGeoClient;

    public FiltrageL0Controller(ServiceGeoClient serviceGeoClient) {
        this.serviceGeoClient = serviceGeoClient;
    }

    /**
     * Reduction de l'espace de recherche autour d'un point (ex. origine d'une
     * demande de transport) - coeur du budget de latence L0 ~50ms.
     */
    @GetMapping
    public List<HubProcheDto> filtrerParZone(@RequestParam double latitude,
                                              @RequestParam double longitude,
                                              @RequestParam(defaultValue = "1") int k) {
        return serviceGeoClient.hubsProches(latitude, longitude, k);
    }
}
