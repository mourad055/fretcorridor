package com.fretcorridor.opt.sequencement.alns;

import com.fretcorridor.dto.PointGeoDto;
import com.fretcorridor.util.HaversineUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * EF-MAT-10 / RG-056 / RG-108 (CDC S8.6.3, S9.4) : "Le systeme doit borner
 * le detour impose a une demande individuelle." Contrainte DURE, jamais une
 * penalite (contrairement aux fenetres temporelles, cf CoutSolution) -
 * citation CDC retenue : "un systeme qui sacrifie systematiquement les
 * memes clients a l'optimum global perd ces clients."
 *
 * Haversine (vol d'oiseau) pendant la recherche ALNS - approximation
 * deliberee, jamais Valhalla ici : la recherche evalue des milliers de
 * solutions candidates par seconde (budget L2 ~5s/30s, CDC S8.10), un appel
 * HTTP par evaluation serait incompatible avec ce budget. Le detour EXACT
 * (Valhalla) n'est recalcule qu'une fois sur la Tournee finale retenue,
 * pour la tracabilite persistee (EtapeTournee.detourDistanceMetres).
 */
@Component
public class DetourValidator {

    private static final Logger log = LoggerFactory.getLogger(DetourValidator.class);

    /**
     * @param origineDirecte  point de collecte de la demande evaluee
     * @param destinationDirecte point de livraison de la demande evaluee
     * @param distanceReelleDansLaTourneeKm somme des segments Haversine
     *        reellement parcourus par cette demande dans la sequence candidate
     *        (enlevement -> ... -> livraison, en passant par les arrets
     *        intermediaires d'autres demandes consolidees)
     * @param parametresAxe Axe.parametres (EF-GEO-02) - lit
     *        "detourMaxDistanceKm" (RG-056). Cle absente = pas de borne
     *        appliquee, meme principe permissif que rayonAppariementKm
     *        (MatchingCycleService) : jamais une valeur par defaut inventee.
     */
    public boolean detourAcceptable(PointGeoDto origineDirecte, PointGeoDto destinationDirecte,
                                     double distanceReelleDansLaTourneeKm,
                                     Map<String, Object> parametresAxe) {
        Double detourMaxKm = extraireDetourMaxKm(parametresAxe);
        if (detourMaxKm == null) {
            return true; // pas de borne configuree ce cycle - permissif, pas un defaut invente
        }

        double distanceDirecteKm = HaversineUtils.distance(origineDirecte, destinationDirecte);
        double detourKm = distanceReelleDansLaTourneeKm - distanceDirecteKm;

        boolean acceptable = detourKm <= detourMaxKm;
        if (!acceptable) {
            log.debug("Detour rejete (EF-MAT-10) : {} km reel vs {} km direct, detour={} km > max={} km",
                    distanceReelleDansLaTourneeKm, distanceDirecteKm, detourKm, detourMaxKm);
        }
        return acceptable;
    }

    private Double extraireDetourMaxKm(Map<String, Object> parametresAxe) {
        if (parametresAxe == null) {
            return null;
        }
        Object valeur = parametresAxe.get("detourMaxDistanceKm");
        return (valeur instanceof Number n) ? n.doubleValue() : null;
    }
}
