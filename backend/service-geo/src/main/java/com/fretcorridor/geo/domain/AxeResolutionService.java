package com.fretcorridor.geo.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Resolution d'un axe a partir de coordonnees GPS (origine + destination).
 *
 * Complement de la resolution par nom de ville faite cote service-mkt
 * (ServiceGeoClient.resoudreAxe) : ce service ne connait que des coordonnees,
 * il est donc utilisable pour n'importe quelle source (GPS mobile, geocodage
 * Nominatim en repli, etc.).
 *
 * Logique, symetrique du zonage H3 deja utilise par le filtre L0 d'OPT :
 *  1. index H3 de chaque point (origine et destination) ;
 *  2. k-ring des cellules voisines (rayon borne, cf RAYON_H3_K) ;
 *  3. hub le plus proche de chaque point (distance Haversine) ;
 *  4. axe oriente hubOrigine -> hubDestination, matching actif.
 *
 * Aucun resultat (point hors zone couverte, hubs identiques, axe inexistant
 * ou inactif, axe d'un autre tenant) = Optional.empty() : l'appelant applique
 * sa degradation gracieuse (ENF-DIS-04), jamais une exception.
 */
@Service
public class AxeResolutionService {

    private static final Logger log = LoggerFactory.getLogger(AxeResolutionService.class);

    /**
     * Rayon de recherche en anneaux H3. 3 est le maximum autorise par le
     * controller de zonage (garde-fou anti-den de service) et couvre
     * largement l'ecart entre un point geocode (centre-ville) et le hub
     * de la meme ville a la resolution configurable (defaut 7).
     */
    private static final int RAYON_H3_K = 3;

    private static final double RAYON_TERRESTRE_KM = 6371.0;

    private final HubRepository hubRepository;
    private final AxeRepository axeRepository;
    private final ZonageH3Service zonageH3Service;

    public AxeResolutionService(HubRepository hubRepository, AxeRepository axeRepository,
                                ZonageH3Service zonageH3Service) {
        this.hubRepository = hubRepository;
        this.axeRepository = axeRepository;
        this.zonageH3Service = zonageH3Service;
    }

    /**
     * @param tenantId optionnel : si fourni, l'axe retourne doit appartenir a
     *                 ce tenant (ENF-MUL-01, meme principe que listerAxes).
     *                 Null = appel interne sans notion de tenant.
     */
    public Optional<Axe> resoudre(double origineLatitude, double origineLongitude,
                                  double destinationLatitude, double destinationLongitude,
                                  String tenantId) {

        Optional<Hub> hubOrigine = hubLePlusProche(origineLatitude, origineLongitude);
        Optional<Hub> hubDestination = hubLePlusProche(destinationLatitude, destinationLongitude);

        if (hubOrigine.isEmpty() || hubDestination.isEmpty()) {
            log.debug("Aucun hub dans le rayon H3 (k={}) pour origine=({}, {}) / destination=({}, {})",
                    RAYON_H3_K, origineLatitude, origineLongitude,
                    destinationLatitude, destinationLongitude);
            return Optional.empty();
        }

        if (hubOrigine.get().getId().equals(hubDestination.get().getId())) {
            log.debug("Origine et destination resolvent sur le meme hub {} - aucun axe oriente.",
                    hubOrigine.get().getId());
            return Optional.empty();
        }

        Optional<Axe> axe = axeRepository
                .findFirstByHubOrigineIdAndHubDestinationIdAndMatchingActifTrue(
                        hubOrigine.get().getId(), hubDestination.get().getId());

        if (axe.isEmpty()) {
            log.debug("Aucun axe actif pour le matching entre les hubs {} et {}.",
                    hubOrigine.get().getId(), hubDestination.get().getId());
            return Optional.empty();
        }

        if (tenantId != null && !tenantId.equals(axe.get().getTenantId())) {
            log.warn("Axe {} resolu mais appartient au tenant {} (demande : {}) - refus (ENF-MUL-01).",
                    axe.get().getId(), axe.get().getTenantId(), tenantId);
            return Optional.empty();
        }

        return axe;
    }

    /**
     * Hub le plus proche d'un point parmi ceux presents dans la cellule H3 du
     * point ou dans ses k anneaux de voisines. Optional.empty() si aucun hub
     * n'est dans ce rayon : on ne rabat jamais sur un hub arbitrairement
     * lointain (une ville non desservie ne doit pas etre rattachee a un axe).
     */
    private Optional<Hub> hubLePlusProche(double latitude, double longitude) {
        String index = zonageH3Service.indexPourPoint(latitude, longitude);
        List<String> cellulesVoisines = zonageH3Service.kRing(index, RAYON_H3_K);
        List<Hub> hubs = hubRepository.findByH3IndexIn(cellulesVoisines);

        return hubs.stream()
                .min(Comparator.comparingDouble(hub -> distanceKm(latitude, longitude, hub)));
    }

    /** Haversine, point JTS : x = longitude, y = latitude (SRID 4326). */
    private double distanceKm(double latitude, double longitude, Hub hub) {
        double lat1 = Math.toRadians(latitude);
        double lat2 = Math.toRadians(hub.getPosition().getY());
        double deltaLat = lat2 - lat1;
        double deltaLon = Math.toRadians(hub.getPosition().getX()) - Math.toRadians(longitude);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return RAYON_TERRESTRE_KM * c;
    }
}
