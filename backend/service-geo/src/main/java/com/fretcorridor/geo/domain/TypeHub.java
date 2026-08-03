package com.fretcorridor.geo.domain;

/**
 * Type de noeud du reseau (cf CDC v4 S13 - Entite Hub).
 *
 * VILLE               : hub represente par une ville entiere (granularite large, phase 1)
 * PLATEFORME          : plateforme logistique dediee (entrepot, cour de transit)
 * POINT_CONSOLIDATION : point ou plusieurs lots/colis sont regroupes avant re-expedition (LTL, phase 2+)
 */
public enum TypeHub {
    VILLE,
    PLATEFORME,
    POINT_CONSOLIDATION
}
