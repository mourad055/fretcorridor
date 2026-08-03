package com.fretcorridor.geo.domain;

import com.uber.h3core.H3Core;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Encapsule la librairie Uber H3 pour tout le service-geo.
 *
 * Objectif : aucune autre classe du service ne doit importer H3Core directement -
 * ca centralise ici la conversion lat/lon <-> index H3 et le calcul de voisinage (k-ring),
 * et ca isole le reste du code d'une eventuelle montee de version de la librairie.
 *
 * La resolution n'est jamais codee en dur (anti-patron explicite du CDC S12.4) :
 * elle est lue depuis geo.configuration_h3 a chaque appel. Le cout d'une lecture DB
 * supplementaire est negligeable ici (config quasi-statique, pas dans le budget L0 ~50ms
 * d'OPT qui, lui, appellera resolutionActuelle() une seule fois par cycle et mettra
 * en cache le resultat de son cote si besoin).
 */
@Service
public class ZonageH3Service {

    // Cle de configuration attendue dans geo.configuration_h3 (cf migration V3).
    private static final String CLE_RESOLUTION_DEFAUT = "resolution_defaut";

    // Valeur de secours UNIQUEMENT si la ligne de config venait a manquer en base -
    // ne remplace pas la configuration versionnee, c'est un garde-fou de derniere ligne
    // pour eviter un crash du service si quelqu'un supprime la ligne par erreur.
    private static final int RESOLUTION_SECOURS = 7;

    private final H3Core h3Core;
    private final ConfigurationH3Repository configurationH3Repository;

    public ZonageH3Service(ConfigurationH3Repository configurationH3Repository) throws IOException {
        // H3Core.newInstance() charge les tables de correspondance H3 en memoire -
        // couteux a l'instanciation (quelques ms), c'est pourquoi on ne cree qu'une
        // seule instance (@Service = singleton Spring) plutot qu'a chaque appel.
        this.h3Core = H3Core.newInstance();
        this.configurationH3Repository = configurationH3Repository;
    }

    /**
     * Resolution H3 actuellement configuree (lue en base, jamais codee en dur).
     * Resolution 7 par defaut ~= hexagones de 1.2 km2 (cf commentaire migration V3).
     */
    public int resolutionActuelle() {
        return configurationH3Repository.findByCle(CLE_RESOLUTION_DEFAUT)
                .map(config -> Integer.parseInt(config.getValeur()))
                .orElse(RESOLUTION_SECOURS);
    }

    /**
     * Calcule l'index H3 de la cellule contenant le point (latitude, longitude),
     * a la resolution actuellement configuree.
     * Utilise a la creation d'un Hub (une seule fois, jamais recalcule ensuite).
     */
    public String indexPourPoint(double latitude, double longitude) {
        return h3Core.latLngToCellAddress(latitude, longitude, resolutionActuelle());
    }

    /**
     * k-ring : la cellule elle-meme plus ses k anneaux de voisines.
     * C'est exactement ce qu'OPT appellera en synchrone interne pour son filtre L0 -
     * reduire l'espace de recherche a "cette zone et ses abords" plutot que tout l'axe.
     *
     * @param indexH3 cellule centrale (ex. celle d'une demande de transport)
     * @param k rayon en nombre d'anneaux (k=1 = la cellule + ses 6 voisines directes)
     */
    public List<String> kRing(String indexH3, int k) {
        return h3Core.gridDisk(indexH3, k);
    }
}
